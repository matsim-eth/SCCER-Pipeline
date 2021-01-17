package greenclass

import java.io.{FileInputStream, IOException}
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime
import java.util
import java.util.{Collections, HashSet, Properties, Set}

import akka.actor.{ActorSystem, PoisonPill, Props}
import com.opencsv.bean.CsvToBeanBuilder
import com.zaxxer.hikari.HikariConfig
import ethz.ivt.externalities.{HelperFunctions, MeasureExternalities}
import ethz.ivt.externalities.actors.TraceActor.JsonFile
import ethz.ivt.externalities.actors.{EventsWriterActor, ExternalitiesWriterActor, TraceActor}
import ethz.ivt.externalities.counters.{ExternalityCostCalculator, ExternalityCounter}
import ethz.ivt.externalities.data.congestion.PtChargingZones
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader
import ethz.ivt.externalities.data.{AggregateDataPerTimeMock, JITVehicleCreator, LatLon, TripLeg, TripRecord}
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping
import ethz.ivt.externalities.unchosenAlternatives.AlternativeRecord
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.events.{Event, PersonArrivalEvent, PersonDepartureEvent}
import org.matsim.api.core.v01.network.{Link, Network}
import org.matsim.api.core.v01.{Coord, Scenario, TransportMode}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.{Config, ConfigUtils}
import org.matsim.core.network.NetworkUtils
import org.matsim.core.network.algorithms.TransportModeNetworkFilter
import org.matsim.core.router.util.TravelTime
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime
import org.matsim.core.utils.collections.QuadTree
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84
import org.matsim.pt.transitSchedule.api.{TransitSchedule, TransitScheduleReader}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.util.Try

object ProcessCSVtrips {

  @throws[IOException]
  def main(args: Array[String]) = {

    val props_filename = args(0)
    val base_input_location = Paths.get(args(1))
    val input_filename = args(2)
    val output_filename = args(3)

    val props = new Properties()
    props.load(new FileInputStream(props_filename))

    val logger = Logger.getLogger(this.getClass)
    val base_file_location = Paths.get(props.getProperty("base.data.location"))


    val matsim_config_location = base_file_location.resolve(Paths.get(props.getProperty("matsim.config.file")))
    val config = ConfigUtils.loadConfig(matsim_config_location.toString, new EmissionsConfigGroup)
    logger.info("Loading scenario")
    val scenario: Scenario = ScenarioUtils.loadScenario(config)
    JITVehicleCreator.addDefaultVehicle(scenario)

    val tripsPath = "2019-09-02_2019-09-09/unchosen_alternatives.csv"
    Logger.getRootLogger.setLevel(Level.INFO)

    val gh_location = base_file_location.resolve(props.getProperty("graphhopper.graph.location"))

    val processWaypointsJson = new ProcessWaypointsJson(scenario, gh_location)

    val costValuesFile = base_file_location.resolve(Paths.get(props.getProperty("cost.values.file")))
    val congestion_file = base_file_location.resolve(Paths.get(props.getProperty("congestion.file")))

    //val writerActorPropsOption = ExternalitiesWriterActor.buildDefault(output_dir)
    val dbProps = new HikariConfig(base_file_location.resolve(props.getProperty("database.properties.file")).toString)

    logger.info("Loading vehicles")
    HelperFunctions.loadVehiclesFromDatabase(scenario, dbProps)

    logger.info("Adding hbefa mappings")
    val roadTypeMapping = OsmHbefaMapping.build()
    roadTypeMapping.addHbefaMappings(scenario.getNetwork)

    logger.info("Build External Costs Module")
    val ecc = new ExternalityCostCalculator(costValuesFile.toString)

    val zonesShpFile = base_file_location.resolve(props.getProperty("pt.zones.shapefile"))
    val odPairsFile = base_file_location.resolve(props.getProperty("pt.zones.od_pairs"))
    val ptChargingZones = new PtChargingZones(scenario, zonesShpFile, odPairsFile)


    logger.info("Build Congestion Module")
    val congestionAggregator = if (props.getProperty("ignore.congestion", "false").equals("true")) {
      logger.warn("Using mock aggregate congestion values - ie 0.")
      new AggregateDataPerTimeMock()
    } else {
      CSVCongestionReader.forLink().read(congestion_file.toString, 900)
    }

    def me = () => new MeasureExternalities(scenario, congestionAggregator, ecc, ptChargingZones)


    val processor = new ProcessCSVtrips(processWaypointsJson, me)

    Files.walk(base_input_location).iterator().asScala
        .filter(Files.isRegularFile(_))
        .filter(_.getFileName.toString.equals(input_filename))
        .toStream
        //.par
        .foreach(f => processor.processFile(f, f.resolveSibling(output_filename)))


  }
}

class ProcessCSVtrips(processWaypointsJson : ProcessWaypointsJson, me: () => MeasureExternalities) {

 // val carRouterFactory = buildCarRouterFactory(scenario.getNetwork)
/*
  def slotInCarEvents(tr: TripRecord, events: Seq[Event]): ( TripRecord, Seq[Event]) = {
    if (events.size > 2 || !tr.legs.headOption.map(_.mode).contains("car")) {
      return (tr, events)
    } else {
      //need to add in the missing events
      val router = carRouterFactory.create("car")
      val startLinkId = events.head.asInstanceOf[PersonDepartureEvent].getLinkId
      val endLinkId = events.head.asInstanceOf[PersonArrivalEvent].getLinkId

      val linksTravelled = router.routeAlternative(startLinkId, endLinkId, tr.legs.head.getStartedSeconds)
      val events = pro
    }
  }*/

  def processFile(inputFile : Path, outputFile : Path) {
    val reader = Files.newBufferedReader(inputFile)
    val tripRecordReader = new CsvToBeanBuilder[AlternativeRecord](reader).withType(classOf[AlternativeRecord]).build

    val cores = Runtime.getRuntime.availableProcessors

    val records = tripRecordReader.iterator().asScala.toStream.take(100)
      .flatMap(alternativeToTripRecord)
      .toList

    records
      .grouped(Math.ceil(records.size / cores).intValue())
      .toStream
      .par
      .map(trList => trList.map( tr => (tr.date, processWaypointsJson.tripLegToEvents(tr, tr.legs.head)._1)))
      .flatMap { trEventList => {
        val extProcessor = me()
        trEventList.map { case (date, events) => extProcessor.process(events.asJava, date.atStartOfDay()) }
      }}
      .map(_.simplifyExternalities())
      .seq
      .foreach(_.appendCsvFile(outputFile))
  }

  /*def buildCarRouterFactory(network : Network) : CarAlternativeRouter.Factory = {

    val roadNetwork = NetworkUtils.createNetwork
    new TransportModeNetworkFilter(network).filter(roadNetwork, Collections.singleton("car"))

    val bounds = NetworkUtils.getBoundingBox(roadNetwork.getNodes.values)
    val roadQuadTree = new QuadTree[Link](bounds(0), bounds(1), bounds(2), bounds(3))
    roadNetwork.getLinks.values.forEach((l: Link) => roadQuadTree.put(l.getCoord.getX, l.getCoord.getY, l))

    var travelTime = new FreeSpeedTravelTime

    // Get car travel times from MATSim events
    if (!config.carUseFreespeed) {
      val travelTimesFromNetwork = new MeasureAggregateTravelTimesFromNetwork(network, config.binSize)
      travelTimesFromNetwork.process(config.eventsPath)
      travelTime = travelTimesFromNetwork.getLinkTravelTimes
    }

    return new CarAlternativeRouter.Factory(roadNetwork, travelTime, roadQuadTree, 300.0)
  }*/

  def alternativeToTripRecord(ar : AlternativeRecord) : List[TripRecord] = {

    val coordTrans = new CH1903LV03PlustoWGS84
    val start = coordTrans.transform(new Coord(ar.getStart_x, ar.getStart_y))
    val end = coordTrans.transform(new Coord(ar.getEnd_x, ar.getEnd_y))
    val start_ll = LatLon(start.getY, start.getX)
    val end_ll = LatLon(end.getY, end.getX)

    if (ar.isTrainJourney) {
      ar.setMode("train")
    }

    val modes: List[String] = ar.getMode match {
      case "pt" => List("tram", "bus")
      case x: String => List(x)
    }

    val tripid = Try(ar.getTripId.toInt).getOrElse(0)
    val partialTripRecord = (ls : List[TripLeg]) => TripRecord(ar.getUserId, tripid, ar.getStarted_at.toLocalDate, ls)

    val tls = modes.map(m => {
      val l = TripLeg(
        ar.getTripId, ar.getStarted_at, ar.getFinished_at,
        start_ll, end_ll, ar.getDistance, m, LocalDateTime.now(), List.empty
      )
      partialTripRecord(List(l))
    })
    tls
  }
}
