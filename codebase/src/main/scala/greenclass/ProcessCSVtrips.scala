package greenclass

import java.io.{FileInputStream, IOException}
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime
import java.util.Properties

import akka.actor.{ActorSystem, PoisonPill, Props}
import com.opencsv.bean.CsvToBeanBuilder
import com.zaxxer.hikari.HikariConfig
import ethz.ivt.externalities.{HelperFunctions, MeasureExternalities}
import ethz.ivt.externalities.actors.TraceActor.JsonFile
import ethz.ivt.externalities.actors.{EventsWriterActor, ExternalitiesWriterActor, TraceActor}
import ethz.ivt.externalities.counters.{ExternalityCostCalculator, ExternalityCounter}
import ethz.ivt.externalities.data.congestion.PtChargingZones
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader
import ethz.ivt.externalities.data.{AggregateDataPerTimeMock, LatLon, TripLeg, TripRecord}
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping
import ethz.ivt.externalities.unchosenAlternatives.AlternativeRecord
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.{Coord, Scenario, TransportMode}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84

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

    val tripsPath = "2019-09-02_2019-09-09/unchosen_alternatives.csv"
    Logger.getRootLogger.setLevel(Level.INFO)

    val gh_location = base_file_location.resolve(props.getProperty("graphhopper.graph.location"))

    val processWaypointsJson = new ProcessWaypointsJson(scenario, gh_location)

    val costValuesFile = base_file_location.resolve(Paths.get(props.getProperty("cost.values.file")))
    val congestion_file = base_file_location.resolve(Paths.get(props.getProperty("congestion.file")))

    else base_file_location.resolve(Paths.get(props.getProperty("trips.folder")))

    //val writerActorPropsOption = ExternalitiesWriterActor.buildDefault(output_dir)
    val dbProps = new HikariConfig(base_file_location.resolve(props.getProperty("database.properties.file")).toString)

    logger.info("Loading vehicles")
    HelperFunctions.loadVehiclesFromDatabase(scenario, dbProps)

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
        .par
        .foreach(f => processor.processFile(f, f.resolveSibling(output_filename)))


  }
}

class ProcessCSVtrips(processWaypointsJson : ProcessWaypointsJson, me: () => MeasureExternalities ) {

  def processFile(inputFile : Path, outputFile : Path) {
    val reader = Files.newBufferedReader(inputFile)
    val tripRecordReader = new CsvToBeanBuilder[AlternativeRecord](reader).withType(classOf[AlternativeRecord]).build

    tripRecordReader.iterator().asScala.toStream
      .par
      .flatMap(alternativeToTripRecord)
      .map(tr => (tr, processWaypointsJson.tripLegToEvents(tr, tr.legs.head)._1))
      .map{case (tr, events) => me().process(events.asJava, tr.date.atStartOfDay())}
      .map(_.simplifyExternalities()).foreach(_.appendCsvFile(outputFile))
  }

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
