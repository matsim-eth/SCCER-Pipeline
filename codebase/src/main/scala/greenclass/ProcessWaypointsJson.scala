package greenclass

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path, Paths}
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.time.format.DateTimeFormatter
import java.util
import java.util.Properties

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.graphhopper.util.GPXEntry
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import ethz.ivt.externalities.{HelperFunctions, MeasureExternalities}
import ethz.ivt.externalities.actors.TraceActor.JsonFile
import ethz.ivt.externalities.actors._
import ethz.ivt.externalities.aggregation.CongestionAggregator
import ethz.ivt.externalities.counters.{ExtendedPersonDepartureEvent, ExternalityCostCalculator}
import ethz.ivt.externalities.data.congestion.PtChargingZones
import ethz.ivt.externalities.data.{AggregateDataPerTime, AggregateDataPerTimeImpl, AggregateDataPerTimeMock, TripLeg, TripRecord}
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader
import ethz.ivt.graphhopperMM.{GHtoEvents, LinkGPXStruct, MATSimMMBuilder}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.{Id, Scenario, TransportMode}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.algorithms.EventWriterXML
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84

import scala.collection.JavaConverters._
import scala.io.{BufferedSource, Source}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.matsim.api.core.v01.events._
import org.matsim.api.core.v01.network.Link
import org.matsim.api.core.v01.population.Person
import org.matsim.vehicles.{VehicleReaderV1, VehicleType, VehicleUtils, VehicleWriterV1}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.locationtech.jts.geom.{Geometry, GeometryFactory, LineString}
import org.locationtech.jts.operation.linemerge.LineMerger
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping
import org.matsim.core.network.NetworkUtils
import org.matsim.core.utils.geometry.GeometryUtils

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object ProcessWaypointsJson {

  Logger.getLogger("com.graphhopper.matching.MapMatchingUnlimited").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

  def main(args: Array[String]) {

    //val args = {"P:\\Projekte\\SCCER\\switzerland_10pct\\switzerland_config_no_facilities.xml", "C:\\Projects\\spark\\green_class_swiss_triplegs.csv","C:\\Projects\\spark\\green_class_waypoints.csv","C:\\Projects\\SCCER_project\\output_gc"}
    val props_filename = args(0)


    val props = new Properties()
    props.load(new FileInputStream(props_filename))

    val logger = Logger.getLogger(this.getClass)
    val base_file_location = Paths.get(props.getProperty("base.data.location"))
    val save_mapmatched_traces = props.getProperty("save.mapmatched_traces", "false").equalsIgnoreCase("true")

    val matsim_config_location = base_file_location.resolve(Paths.get(props.getProperty("matsim.config.file")))
    val config = ConfigUtils.loadConfig(matsim_config_location.toString, new EmissionsConfigGroup)

    val costValuesFile = base_file_location.resolve(Paths.get(props.getProperty("cost.values.file")))
    val congestion_file = base_file_location.resolve(Paths.get(props.getProperty("congestion.file")))
    val output_dir = base_file_location.resolve(Paths.get(props.getProperty("output.dir")))
    val numCores = Option(props.getProperty("num.cores")).map(_.toInt).getOrElse(1)

    val events_folder = base_file_location.resolve(Paths.get(props.getProperty("events.folder")))


    val trips_folder = if (args.length > 1) Paths.get(args(1))
    else base_file_location.resolve(Paths.get(props.getProperty("trips.folder")))

    val _system = ActorSystem("MainEngineActor")
    val reaper = _system.actorOf(Props[Reaper], Reaper.name)

    //val writerActorProps = ExternalitiesWriterActor.buildDefault(output_dir)
    val dbProps = new HikariConfig(props.getProperty("database.properties.file"))

    val calculate_externalities : Boolean = props.getProperty("calculate.externalities", "true") == "true"
    val externalities_out_location = props.getProperty("write.externalities.to", "database")

    val writerActorProps : Option[Props] = if (calculate_externalities) {
      if (externalities_out_location.equals("database")) {
        Option(ExternalitiesWriterActor.buildMobis(dbProps))
      } else if (externalities_out_location.equals("file")) {
        Option(ExternalitiesWriterActor.buildDefault(output_dir.resolve("externalities")))
      } else if (externalities_out_location.equals("nowhere")) {
        Option(ExternalitiesWriterActor.buildDummy())
      } else {
        Option.empty
      }
    } else {
      Option.empty
    }


    val writerActorOption = writerActorProps.map(_system.actorOf(_, "DatabaseWriter"))

    implicit val ec: ExecutionContext = _system.dispatcher

    logger.info("Loading scenario")
    val scenario: Scenario = ScenarioUtils.loadScenario(config)

    logger.info("Loading vehicles")
    HelperFunctions.loadVehiclesFromDatabase(scenario, dbProps)

    logger.info("Adding hbefa mappings")
    val roadTypeMapping = OsmHbefaMapping.build()
    roadTypeMapping.addHbefaMappings(scenario.getNetwork)

    //  Option(gc_vehicles_file).foreach(vf => new VehicleReaderV1(scenario.getVehicles).readFile(vf.toString))

    logger.info("Build ProcessWaypoints Module (with graphhopper)")
    val gh_location = base_file_location.resolve(props.getProperty("graphhopper.graph.location"))
    val processWaypointsJson = new ProcessWaypointsJson(scenario, gh_location)

    logger.info("Build External Costs Module")
    val ecc = new ExternalityCostCalculator(costValuesFile.toString)

    val zonesShpFile = Paths.get(props.getProperty("pt.zones.shapefile"))
    val odPairsFile = Paths.get(props.getProperty("pt.zones.od_pairs"))
    val ptChargingZones = new PtChargingZones(scenario, zonesShpFile, odPairsFile)


    val externalitiyProcessorOption = writerActorOption.map(writerActor => {

      logger.info("Build Congestion Module")
      val congestionAggregator = if (props.getProperty("ignore.congestion", "false").equals("true")) {
        logger.warn("Using mock aggregate congestion values - ie 0.")
        new AggregateDataPerTimeMock()
      } else {
        CSVCongestionReader.forLink().read(congestion_file.toString, 900)
      }

      def me = () => new MeasureExternalities(scenario, congestionAggregator, ecc, ptChargingZones)

      val extProps = ExternalitiesActor.props(me, writerActor)
      logger.info("Preloadable Data (excluding emissions module) loaded")

      _system.actorOf(extProps, "ExternalityProcessor")
    })



    val traces_output_dir = props.getProperty("traces.folder")
    val eventWriterProps = EventsWriterActor.props(scenario, traces_output_dir)
    val eventWriterActor = if (save_mapmatched_traces) {
      Some(_system.actorOf(eventWriterProps, "EventWriterActor"))
    } else {
      None
    }


    val eventProps = EventActor.props(processWaypointsJson, externalitiyProcessorOption, eventWriterActor)
    val eventsActor = _system.actorOf(eventProps, name = "EventActor")

    val traceProps = TraceActor.props(processWaypointsJson, eventsActor)
    val traceProcessors = _system.actorOf(traceProps, name = "TraceActor")

    logger.info("actor system ready")
    Thread.sleep(5 * 1000)

    logger.info("processing waypoints in actor system")
    val jsons = processWaypointsJson.filterJsonFiles(trips_folder)
    jsons.map(JsonFile).foreach(traceProcessors ! _)

    //traceProcessors ! PoisonPill

  }



}

class ProcessWaypointsJson(scenario: Scenario, hopper_location: Path) {

  val logger = Logger.getLogger(this.getClass)
  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(scenario.getNetwork, new CH1903LV03PlustoWGS84, hopper_location)
  gh.getMatcher.setMeasurementErrorSigma(100)


  val json_matcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:**.json")

  def readJson(p: Path): List[TripRecord] = {
    logger.info(s"parsing $p")
    val json = Source.fromFile(p.toFile).getLines mkString "\n"
    if (json.isEmpty) {
      Nil
    }
    else {
      parse(json).extract[List[TripRecord]]
    }
  }


  def filterJsonFiles(triplegs_folder: Path): Stream[Path] = {
    Files.walk(triplegs_folder).iterator().asScala
      .filter(json_matcher.matches(_))
      .toStream

  }

  def processJson(tr : TripRecord): Stream[(String, Seq[Event], Geometry)] = {

    logger.info(s"\tprocessing ${tr.user_id}, ${tr.date}")

    val futures = tr.legs.toStream
        .filterNot(tl => "Activity".equals(tl.mode))
        .map { tl =>
          Try {
            val (events, linestring) = tripLegToEvents(tr, tl)
            logger.info(s"\t\tleg ${tl.leg_id} converted to ${events.size} events")
            (tl.leg_id, events, linestring)
          }.recoverWith { case ex : Throwable => logger.error(s"Error on leg ${tl.leg_id}", ex); Failure(ex) }
        }

      //return successes
      futures.flatMap(_.toOption).filterNot(_._2.isEmpty) //remove empty triplegs

  }

  def createLineString(tl: TripLeg, links: List[LinkGPXStruct]) : Geometry = {
    val linemerger = new LineMerger()

    links.foreach( x => {
      val ls : org.locationtech.jts.geom.LineString = GeometryUtils.createGeotoolsLineString(x.getLink)
      linemerger.add(ls)
    })
    val merged = new GeometryFactory().buildGeometry(linemerger.getMergedLineStrings)
    merged
  }



  def tripLegToEvents(tr : TripRecord, tl : TripLeg) : (Seq[Event], Geometry) = {
    val personId = Id.createPersonId(tr.user_id)
    val vehicleId = determineVehicleType(tr.user_id, tl.mode)
    val (links : scala.List[LinkGPXStruct], linkEvents : List[Event])  = if (tl.mode.equals(TransportMode.car)) {

      val start_pt = tl.getStartPoint
      val finish_pt = tl.getFinishPoint

      val entries = (start_pt +: tl.waypoints.map(_.toGPX) :+ finish_pt).asJava
      val links = gh.mapMatchWithTravelTimes(entries)
      val events = gh.linkGPXToEvents(links.iterator, vehicleId).asScala.toList
      (links.asScala.toList, events)
    } else (List.empty, List.empty)


    val events_full = bookendEventswithDepArr(gh, tl, personId, tl.mode, linkEvents)
    val linestring = createLineString(tl, links)
    (events_full, linestring)
  }

  private def determineVehicleType(user_id: String, mode: String) = {
    Id.createVehicleId(user_id.toString) // + mode)
  }

  def getEventLink(event: Event): Option[Id[Link]] = {
    Some(event).map { case e : HasLinkId => e.getLinkId }
  }


  def bookendEventswithDepArr(gh : GHtoEvents, tl: TripLeg,
                              personId:Id[Person], mappedMode : String,
                              linkEvents : List[Event]) : Seq[Event] = {


    val departureLink : Id[Link] = linkEvents.headOption.flatMap(getEventLink)
      .getOrElse(gh.getNearestLinkId(tl.start_point.toGPX))
    val arrivalLink : Id[Link] = linkEvents.lastOption.flatMap(getEventLink)
      .getOrElse(gh.getNearestLinkId(tl.finish_point.toGPX))

    if (departureLink == null) {
      logger.warn(s"the trip start for ${tl.leg_id} could not be matched to the matsim network")
    }
    if (arrivalLink == null) {
      logger.warn(s"the trip end for ${tl.leg_id} could not be matched to the matsim network")
    }
    if (departureLink == null || arrivalLink == null) {
      return Nil
    }

    val departureEvent = new ExtendedPersonDepartureEvent(tl.getStartedSeconds,
      personId, departureLink, mappedMode, tl.distance, tl.leg_id, tl.updated_at)

    val arrivalEvent = new PersonArrivalEvent(tl.getFinishedSeconds, personId, arrivalLink, mappedMode)
    departureEvent :: (linkEvents ::: List(arrivalEvent))
  }


}
