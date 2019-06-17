package greenclass

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path, Paths}
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.time.format.DateTimeFormatter
import java.util.Properties

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import akka.util.Timeout
import com.graphhopper.util.GPXEntry
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import ethz.ivt.externalities.{HelperFunctions, MeasureExternalities}
import ethz.ivt.externalities.actors.TraceActor.JsonFile
import ethz.ivt.externalities.actors._
import ethz.ivt.externalities.aggregation.CongestionAggregator
import ethz.ivt.externalities.counters.{ExtendedPersonDepartureEvent, ExternalityCostCalculator}
import ethz.ivt.externalities.data.{AggregateDataPerTime, AggregateDataPerTimeImpl, AggregateDataPerTimeMock, TripLeg, TripRecord}
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader
import ethz.ivt.graphhopperMM.{GHtoEvents, LinkGPXStruct, MATSimMMBuilder}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.{Id, Scenario, TransportMode}
import org.matsim.contrib.emissions.utils.{EmissionUtils, EmissionsConfigGroup}
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
import akka.pattern.ask
import akka.util.Timeout
import com.esotericsoftware.kryo.io.Input
import com.vividsolutions.jts.geom.{Geometry, GeometryFactory, LineString}
import com.vividsolutions.jts.operation.linemerge.LineMerger
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping
import org.matsim.core.network.NetworkUtils
import org.matsim.core.utils.geometry.GeometryUtils

import scala.concurrent.duration._
import scala.util.{Failure, Success}

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
    val matsim_config_location = base_file_location.resolve(Paths.get(props.getProperty("matsim.config.file")))
    val config = ConfigUtils.loadConfig(matsim_config_location.toString, new EmissionsConfigGroup)
    val gc_vehicles_file = base_file_location.resolve(Paths.get(props.getProperty("vehicles.file")))

    val costValuesFile = base_file_location.resolve(Paths.get(props.getProperty("cost.values.file")))
    val congestion_file = base_file_location.resolve(Paths.get(props.getProperty("congestion.file")))
    val output_dir = base_file_location.resolve(Paths.get(props.getProperty("output.dir")))
    val numCores = Option(props.getProperty("num.cores")).map(_.toInt).getOrElse(1)

    val events_folder = base_file_location.resolve(Paths.get(props.getProperty("events.folder")))

    val trips_folder =  if (args.length > 1) Paths.get(args(1))
                        else base_file_location.resolve(Paths.get(props.getProperty("trips.folder")))

    val _system = ActorSystem("MainEngineActor")

    //val writerActorProps = ExternalitiesWriterActor.buildDefault(output_dir)
    val dbProps = new HikariConfig(props.getProperty("database.properties.file"))

    val writerActorProps = ExternalitiesWriterActor.buildMobis(dbProps)
    val writerActor = _system.actorOf(writerActorProps, "DatabaseWriter")

    implicit val ec: ExecutionContext = _system.dispatcher

    val scenario: Scenario = ScenarioUtils.loadScenario(config)

    HelperFunctions.createVehicleTypes(scenario: Scenario)
    HelperFunctions.loadVehiclesFromDatabase(scenario, dbProps)

    val roadTypeMapping = OsmHbefaMapping.build()
    roadTypeMapping.addHbefaMappings(scenario.getNetwork)

  //  Option(gc_vehicles_file).foreach(vf => new VehicleReaderV1(scenario.getVehicles).readFile(vf.toString))

    val processWaypointsJson = new ProcessWaypointsJson(scenario)

    val congestionAggregator = if (props_filename.contains("euler")) {
      CSVCongestionReader.forLink().read(congestion_file.toString, 900)
    } else {
      logger.warn("Using mock aggregate congestion values - ie 0.")
      new AggregateDataPerTimeMock()
    }

    val ecc = new ExternalityCostCalculator(costValuesFile.toString)
    def me = () => new MeasureExternalities(scenario, congestionAggregator, ecc)

    logger.info("Data loaded")

    val extProps = ExternalitiesActor.props(me, writerActor)
    val externalitiyProcessor = _system.actorOf(extProps, "ExternalityProcessor")

    val traces_output_dir = props.getProperty("traces.folder")
    val eventWriterProps = EventsWriterActor.props(scenario, traces_output_dir)
    val eventWriterActor = _system.actorOf(eventWriterProps, "EventWriterActor")


    val eventProps = EventActor.props(processWaypointsJson, externalitiyProcessor, eventWriterActor)
    val eventsActor = _system.actorOf(eventProps.withRouter(RoundRobinPool(numCores)), name = "EventActor")

    val traceProps = TraceActor.props(processWaypointsJson, eventsActor)
    val traceProcessors = _system.actorOf(traceProps, name = "TraceActor")

    logger.info("actor system ready")
    Thread.sleep(5*1000)

    logger.info("processing waypoints in actor system")
    val jsons = processWaypointsJson.filterJsonFiles(trips_folder)
    jsons.map(JsonFile).foreach(traceProcessors ! _)

    import akka.pattern.gracefulStop
    import scala.concurrent.duration._

//    gracefulStop(traceProcessors, 3 minutes, logger.info("stopped trace processors"))
//      .flatMap(_ => gracefulStop(eventsActor, 3 minutes, logger.info("stopped matsim mapmatching actor")))
//      .flatMap(_ => gracefulStop(externalitiyProcessor, 3 minutes, logger.info("stopped externality processors")))
//      .onComplete(_ => _system.terminate().onComplete(_ => logger.info("Actor system shut down")))
  }

}

class ProcessWaypointsJson(scenario: Scenario) {

  val logger = Logger.getLogger(this.getClass)
  val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(scenario.getNetwork, new CH1903LV03PlustoWGS84)
  gh.getMatcher.setMeasurementErrorSigma(500)

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

  def processJson(tr : TripRecord): Stream[(Long, Seq[Event], Geometry)] = {

    logger.info(s"\tprocessing ${tr.user_id}, ${tr.date}")

    tr.legs.toStream
        .filterNot(tl => "Activity".equals(tl.mode))
        .map { tl =>
          val (events, linestring) = tripLegToEvents(tr, tl)
          (tl.leg_id, events, linestring)
        }
        .filterNot(_._2.isEmpty) //remove empty triplegs

  }

  def createLineString(tl: TripLeg, links: List[LinkGPXStruct]) : Geometry = {
    val linemerger = new LineMerger()

    links.foreach( x => {
      val ls = GeometryUtils.createGeotoolsLineString(x.getLink)
      linemerger.add(ls)
    })
    val merged = new GeometryFactory().buildGeometry(linemerger.getMergedLineStrings)
    merged
  }

  def tripLegToEvents(tr : TripRecord, tl : TripLeg) : (Seq[Event], Geometry) = {
    val matsim_mode = mapMode(tl.mode)
    val personId = Id.createPersonId(tr.user_id)
    val vehicleId = determineVehicleType(tr.user_id, tl.mode)
    val (links : scala.List[LinkGPXStruct], linkEvents : List[Event])  = if (matsim_mode == TransportMode.car) {
      val entries = tl.waypoints.map(_.toGPX).asJava
      val links = gh.mapMatchWithTravelTimes(entries)
      val events = gh.linkGPXToEvents(links.iterator, vehicleId).asScala.toList
      (links.asScala.toList, events)
    } else (List.empty, List.empty)


    val events_full = bookendEventswithDepArr(gh, tl, personId, matsim_mode, linkEvents)
    val linestring = createLineString(tl, links)
    (events_full, linestring)
  }

  private def determineVehicleType(user_id: String, mode: String) = {
    Id.createVehicleId(user_id.toString) // + mode)
  }

  def getEventLink(event: Event): Option[Id[Link]] = {
    Some(event).map { case e : HasLinkId => e.getLinkId }
  }

  def mapMode(mode: String) : String = mode.toLowerCase match {
    case "ecar" | "car" => TransportMode.car
    case "bus" => TransportMode.pt
    case "tram" => TransportMode.pt
    case "train" => TransportMode.train
    case "walk"  => TransportMode.walk
    case "bicycle" => TransportMode.bike
    case _ => TransportMode.other


  }

  def bookendEventswithDepArr(gh : GHtoEvents, tl: TripLeg,
                              personId:Id[Person], mappedMode : String,
                              linkEvents : List[Event]) : Seq[Event] = {

    val departureLink : Id[Link] = linkEvents.headOption.flatMap(getEventLink)
      .getOrElse(gh.getNearestLinkId(tl.start_point.toGPX))
    val arrivalLink : Id[Link] = linkEvents.lastOption.flatMap(getEventLink)
      .getOrElse(gh.getNearestLinkId(tl.finish_point.toGPX))

    if (departureLink == null) {
      logger.error(s"the trip start for ${tl.leg_id} could not be matched to the matsim network")
    }
    if (arrivalLink == null) {
      logger.error(s"the trip end for ${tl.leg_id} could not be matched to the matsim network")
    }

    val departureEvent = new ExtendedPersonDepartureEvent(tl.getStartedSeconds,
      personId, departureLink, mappedMode, tl.distance, tl.leg_id)

    val arrivalEvent = new PersonArrivalEvent(tl.getFinishedSeconds, personId, arrivalLink, mappedMode)
    departureEvent :: (linkEvents ::: List(arrivalEvent))
  }


}
