package greenclass

import java.io.{File, FileWriter}
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

object SplitWaypoints {

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

      import scala.concurrent._

      personday_triplegs.par.foreach {
        case (tr, tl_ids) =>
          println(s"processing ${tr.user_id}, ${tr.date}, ${tr.date.format(dateFormatter)}")
          val gpxEntry: Stream[(Int, Seq[GPXEntry])] =
            tl_ids.map {tlr => tlr._2.leg_id -> tripleg_waypoints.getOrElse(tlr._2.leg_id, Seq.empty)}

          val date_dir = s"$OUTPUT_DIR/${tr.date.format(dateFormatter)}/${tr.user_id}"
          new File(date_dir).mkdirs

          gpxEntry.foreach { case (tl_id, gs) =>
            val eventWriter = new FileWriter(new File(s"$date_dir/$tl_id-gpx.csv"))
            eventWriter.write("longitude,latitude,time\n")
            gs.foreach(g => eventWriter.write("%f, %f, %d\n".format(g.lon, g.lat, g.getTime)))
            eventWriter.close()
          }

      }



      //group by user / date / mode

      //get waypoints for trip_legs --indexing?

      //one loop per day to process a person-day
      //process waypoints into events



    }



}
