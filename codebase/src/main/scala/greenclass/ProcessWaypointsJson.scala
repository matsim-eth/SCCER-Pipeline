package greenclass

import java.io.File
import java.nio.file.{Files, Path, Paths}
import java.sql.{DriverManager, ResultSet}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.graphhopper.util.GPXEntry
import ethz.ivt.graphhopperMM.{GHtoEvents, MATSimMMBuilder}
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
import org.matsim.api.core.v01.events.{Event, PersonArrivalEvent, PersonDepartureEvent}
import org.matsim.api.core.v01.population.Person

object ProcessWaypointsJson {

  Logger.getLogger("com.graphhopper.matching.MapMatching").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

  val logger = Logger.getLogger(this.getClass)

  def readJson(p: Path): List[TripRecord] = {
    val json = Source.fromFile(p.toFile).getLines mkString "\n"
    parse(json).extract[List[TripRecord]]
  }

  import java.nio.file.FileSystems
  import java.nio.file.PathMatcher

  val json_matcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:**.json")

  def loadJsonTrips(triplegs_folder: Path): List[TripRecord] = {
    Files.walk(triplegs_folder).iterator().asScala
      .filter(json_matcher.matches(_))
          .take(5)
      .flatMap(p => readJson(p))
      .toList
  }

  def mapMode(mode: String) : String = TransportMode.car

  def processNonCarTrip(gh : GHtoEvents, tl: TripLeg, personId:Id[Person]) : List[Event] = {
    val departureLink = gh.getNearestLink(tl.start_point.toGPX).orElse(null)
    val arrivalLink = gh.getNearestLink(tl.start_point.toGPX).orElse(null)

    if (departureLink == null) {
      logger.error(s"the trip start for $tl could not be matched to the matsim network")
    }
    if (arrivalLink == null) {
      logger.error(s"the trip end for $tl could not be matched to the matsim network")
    }

    val departureEvent = new PersonDepartureEvent(tl.getStartedMillis, personId, departureLink.getId, tl.mode)
    val arrivalEvent = new PersonArrivalEvent(tl.getFinishedMillis, personId, arrivalLink.getId, tl.mode)
    List(departureEvent, arrivalEvent)
  }

  def main(args: Array[String]) {

    //val args = {"P:\\Projekte\\SCCER\\switzerland_10pct\\switzerland_config_no_facilities.xml", "C:\\Projects\\spark\\green_class_swiss_triplegs.csv","C:\\Projects\\spark\\green_class_waypoints.csv","C:\\Projects\\SCCER_project\\output_gc"}


    val logger = Logger.getLogger(this.getClass)

    val config = ConfigUtils.loadConfig(args(0), new EmissionsConfigGroup)
    val trips_folder = Paths.get(args(1))
    val OUTPUT_DIR = args(2)
    val overwrite: Boolean = args.applyOrElse(3, (_: Int) => "False").toBoolean

    new File(OUTPUT_DIR).mkdirs
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val scenario: Scenario = ScenarioUtils.loadScenario(config)
    val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(scenario.getNetwork, new CH1903LV03PlustoWGS84)

    val personday_triplegs: List[TripRecord] = loadJsonTrips(trips_folder)

    personday_triplegs
      .par.foreach {
      case (tr: TripRecord) =>
        logger.info(s"processing ${tr.user_id}, ${tr.date}")

        new File(getDateFolder(tr)).mkdirs

        def getDateFolder(tr: TripRecord) = s"$OUTPUT_DIR/${tr.date.format(dateFormatter)}/"

        def getEventFileName(tr: TripRecord) = s"${getDateFolder(tr)}/${tr.user_id}-events.xml"

        val eventLocation = new File(getEventFileName(tr))
        if (!eventLocation.exists() || overwrite) {

          val events = tr.legs
              .filterNot(tl => "activity".equals(tl.mode) )
              .map { case tl : TripLeg =>
                val matsim_mode = mapMode(tl.mode)
                val personId = Id.createPersonId(tr.user_id)
              val vehicleId = determineVehicleType(tr.user_id, tl.mode)
              val eventList = if (matsim_mode == TransportMode.car) {
                gh.gpsToEvents(tl.waypoints.map(_.toGPX).asJava, personId, vehicleId, matsim_mode).asScala
              } else {
                processNonCarTrip(gh, tl, personId)
              }
              eventList

            }
            .filterNot(_.isEmpty) //remove empty triplegs
            .sortBy(_.head.getTime) //sort by the first event
            .flatten

          val eventWriter = new EventWriterXML(eventLocation.getAbsolutePath)
          events.foreach(eventWriter.handleEvent)
          eventWriter.closeFile()
        } else {
          logger.info(s"${eventLocation.getCanonicalPath} already exists, skipping")
        }


    }

    def determineVehicleType(user_id: String, mode: String) = {
      Id.createVehicleId(user_id.toString + mode)
    }
  }
}

class ProcessWaypointsJson {

}
