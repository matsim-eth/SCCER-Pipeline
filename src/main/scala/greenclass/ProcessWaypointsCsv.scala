package greenclass

import java.io.{File, FilenameFilter}
import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import com.graphhopper.util.GPXEntry
import ethz.ivt.graphhopperMM.{GHtoEvents, MATSimMMBuilder}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.events.Event
import org.matsim.api.core.v01.{Id, Scenario}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.algorithms.EventWriterXML
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84

import scala.io.Source
import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future, TimeoutException}

object ProcessWaypointsCsv {
  case class TripRecord(user_id: Int, date: LocalDate)
  case class TripLeg(leg_id: Int, mode: String)
  Logger.getLogger("com.graphhopper.matching.MapMatching").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

  def main(args : Array[String]) {

    //val args = {"P:\\Projekte\\SCCER\\switzerland_10pct\\switzerland_config_no_facilities.xml", "C:\\Projects\\spark\\green_class_swiss_triplegs.csv","C:\\Projects\\spark\\green_class_waypoints.csv","C:\\Projects\\SCCER_project\\output_gc"}


    val logger = Logger.getLogger(this.getClass)

    val config = ConfigUtils.loadConfig(args(0), new EmissionsConfigGroup)
    val triplegs_src = Source.fromFile(args(1))
    val waypoints_folder = args(2)
    val OUTPUT_DIR = args(3)
    val overwrite : Boolean = args.applyOrElse(4, (_ : Int) => "False" ).toBoolean

    new File(OUTPUT_DIR).mkdirs
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")


    //config.controler.setOutputDirectory(RUN_FOLDER + "aggregate/")

    //val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SS]")
      //read in user / date / mode / trip leg ids
      val personday_triplegs = triplegs_src.getLines().drop(1).map(_.split(",").map(_.replace("\"", "")))
        .map { case Array(user_id, id, _, started_at, _, mode) =>
          TripRecord(user_id.toInt, LocalDateTime.parse(started_at.replace( " " , "T" )).toLocalDate) -> TripLeg(id.toInt, mode) //replace with t to let normal formatter do its work
        }
        .toStream
        .groupBy(_._1)
        //.map { case (k,v) =>  k -> v.map(_._2) } //just take the trip id from the pair of (trip record, tripleg_id

    logger.info(s"${personday_triplegs.map(_._2.size).sum} triplegs loaded")


    val scenario: Scenario = ScenarioUtils.loadScenario(config)
    val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(scenario.getNetwork, new CH1903LV03PlustoWGS84)

    import scala.concurrent._

    def getCsvs(tr : TripRecord ) =  new File(s"$waypoints_folder/${tr.date}/${tr.user_id}-gpx.csv")

    def readCsv(f : File) = {
      Source.fromFile(f).getLines.drop(1)
        .map(_.split(","))
        .map { case Array(tr_id, long, lat, tracked_at) =>
          tr_id.trim.toLong -> new GPXEntry(lat.trim.toDouble, long.trim.toDouble, tracked_at.trim.toLong)
        }.toStream.groupBy(_._1).map{case (k,v) => (k, v.map(_._2))}

    }

    personday_triplegs
    //    .filterKeys(tr => tr.date == LocalDate.parse("2017-08-31")) //TODO: set up selectable filter from command line
      .par.foreach {
      case (tr, tl_ids) =>
        logger.info(s"processing ${tr.user_id}, ${tr.date}")
        val tripLegsCsv = getCsvs(tr)

        new File(getDateFolder(tr)).mkdirs

        def getDateFolder(tr: TripRecord) = s"$OUTPUT_DIR/events/${tr.date.format(dateFormatter)}/"

        def getEventFileName(tr: TripRecord) = s"${getDateFolder(tr)}/${tr.user_id}-events.xml"

        val eventLocation = new File(getEventFileName(tr))
        if (!eventLocation.exists() || overwrite) {

          val events =
            readCsv(tripLegsCsv)
              .map { case (_, wp2) => gh.gpsToEvents(wp2.asJava, Id.createPersonId(tr.user_id), Id.createVehicleId(tr.user_id)).asScala }
              .filterNot(_.isEmpty) //remove empty triplegs
              .toList.sortBy(_.head.getTime) //sort by the first event
              .flatten

          val eventWriter = new EventWriterXML(eventLocation.getAbsolutePath)
          events.foreach(eventWriter.handleEvent)
          eventWriter.closeFile()
        } else {
          logger.info(s"${eventLocation.getCanonicalPath} already exists, skipping")
      }

    }



    //group by user / date / mode

    //get waypoints for trip_legs --indexing?

    //one loop per day to process a person-day
    //process waypoints into events



  }

}
