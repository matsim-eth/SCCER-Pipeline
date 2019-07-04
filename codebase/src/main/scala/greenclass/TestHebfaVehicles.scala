package greenclass

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
import akka.pattern.ask
import akka.util.Timeout
import com.esotericsoftware.kryo.io.Input
import org.locationtech.jts.geom.{Geometry, GeometryFactory, LineString}
import org.locationtech.jts.operation.linemerge.LineMerger
import ethz.ivt.JITvehicleCreator
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping
import org.matsim.contrib.emissions.EmissionModule
import org.matsim.core.events.{EventsManagerImpl, MatsimEventsReader}
import org.matsim.core.network.NetworkUtils
import org.matsim.core.utils.geometry.GeometryUtils

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TestHebfaVehicles {

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
    val trips_folder = base_file_location.resolve(Paths.get(props.getProperty("trips.folder")))
    val output_dir = base_file_location.resolve(Paths.get(props.getProperty("output.dir")))
    val numCores = Option(props.getProperty("num.cores")).map(_.toInt).getOrElse(1)

    val events_folder = base_file_location.resolve(Paths.get(props.getProperty("events.folder")))


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

    val eventsManager = new EventsManagerImpl

    // setup externality counters
    val ecg = scenario.getConfig.getModules.get(EmissionsConfigGroup.GROUP_NAME).asInstanceOf[EmissionsConfigGroup]
    //    ecg.setUsingDetailedEmissionCalculation(false);

    val emissionModule = new EmissionModule(scenario, eventsManager)
    scenario.getVehicles.getVehicles.asScala.foreach { case (id, v) =>
      val link = scenario.getNetwork.getLinks.values().stream().findAny().get()
      val emissions = emissionModule.getWarmEmissionAnalysisModule
        .checkVehicleInfoAndCalculateWarmEmissions(v, link, 1000)
      if (emissions.get("CO2(total)") == 0.0) {
        System.out.println(v.getType.getDescription)
      }
    }
  }
}
