package greenclass

import java.io.File
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
    val waypoints_src = Source.fromFile(args(2))
    val OUTPUT_DIR = args(3)
    val timeout_duration = args(4).toInt

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

    //return the time, but not date of the point, we can get that from the trip_leg id

    val tripleg_waypoints : Map[Int, Seq[GPXEntry]] =  waypoints_src.getLines.drop(1)
      .map(_.split(","))
      .map { case Array(long, lat, tracked_at, tl_id) =>
          tl_id.toInt -> new GPXEntry(lat.toDouble, long.toDouble, tracked_at.toLong)
        }
      .toStream.groupBy(_._1)
      .map{ case (k, ss) => (k, ss.map(_._2))}


    logger.info(s"${tripleg_waypoints.map(_._2.size).sum} waypoints loaded")

    val scenario: Scenario = ScenarioUtils.loadScenario(config)
    val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(scenario.getNetwork, new CH1903LV03PlustoWGS84)

    import scala.concurrent._

    personday_triplegs.par.foreach {
      case (tr, tl_ids) =>
        println(s"processing ${tr.user_id}, ${tr.date}, ${tr.date.format(dateFormatter)}")
        val events: Stream[Event] =
          tl_ids.flatMap {tlr =>
            val waypoints2 : Seq[GPXEntry] = tripleg_waypoints.getOrElse(tlr._2.leg_id, Seq.empty)
            import ExecutionContext.Implicits.global

            val f = Future {
              val r = gh.gpsToEvents(waypoints2.asJava, Id.createPersonId(tr.user_id), Id.createVehicleId(tr.user_id)).asScala//TODO keep vehicle type here (mode : e-car)
              println(s"sucess on ${tr.user_id}, ${tr.date}")
              r
            }
            try {
              import scala.concurrent.duration._
              Await.result(f , timeout_duration seconds);
            } catch {
              case e: TimeoutException => println(s"error on ${tr.user_id}, ${tr.date}")
                Stream.Empty
            }
            }

        val date_dir = s"$OUTPUT_DIR/${tr.date.format(dateFormatter)}/"
        new File(date_dir).mkdirs

        val eventWriter = new EventWriterXML(s"$date_dir/${tr.user_id}-events.xml")

        events.foreach(eventWriter.handleEvent)

        eventWriter.closeFile()

    }



    //group by user / date / mode

    //get waypoints for trip_legs --indexing?

    //one loop per day to process a person-day
    //process waypoints into events



  }

}
