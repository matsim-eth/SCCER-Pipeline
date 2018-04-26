package greenclass

import java.io.File
import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import com.graphhopper.util.GPXEntry
import ethz.ivt.graphhopperMM.{GHtoEvents, MATSimMMBuilder}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.{Id, Scenario}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.algorithms.EventWriterXML
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84

import scala.io.Source
import scala.collection.JavaConverters._

object ProcessWaypointsCsv {
  case class TripRecord(user_id: Int, date: LocalDateTime)
  case class TripLeg(leg_id: Int, mode: String)
  Logger.getLogger("com.graphhopper.matching.MapMatching").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

  def main(args : Array[String]) {
    val logger = Logger.getLogger(this.getClass)

    val config = ConfigUtils.loadConfig(args(0), new EmissionsConfigGroup)
    val triplegs_src = Source.fromFile(args(1))
    val waypoints_src = Source.fromFile(args(2))
    val OUTPUT_DIR = args(3)
    //config.controler.setOutputDirectory(RUN_FOLDER + "aggregate/")

    //val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SS]")
      //read in user / date / mode / trip leg ids
      val personday_triplegs = triplegs_src.getLines().drop(1).map(_.split(",").map(_.replace("\"", "")))
        .map { case Array(user_id, id, _, started_at, _, mode) =>
          TripRecord(user_id.toInt, LocalDateTime.parse(started_at.replace( " " , "T" ))) -> TripLeg(id.toInt, mode) //replace with t to let normal formatter do its work
        }
        .toStream
        .groupBy(_._1)
        //.map { case (k,v) =>  k -> v.map(_._2) } //just take the trip id from the pair of (trip record, tripleg_id

    logger.info(s"${personday_triplegs.map(_._2.size).sum} triplegs loaded")

    //return the time, but not date of the point, we can get that from the trip_leg id

    val tripleg_waypoints =  waypoints_src.getLines.drop(1)
      .map(_.split(","))
      .map { case Array(long, lat, tracked_at, tl_id) =>
          tl_id.toInt -> new GPXEntry(lat.toDouble, long.toDouble, tracked_at.toLong)
        }
      .toStream.groupBy(_._1)
      .map{ case (k, ss) => (k, ss.map(_._2).toList)}


    logger.info(s"${tripleg_waypoints.map(_._2.size).sum} waypoints loaded")

    val scenario: Scenario = ScenarioUtils.loadScenario(config)
    val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(scenario.getNetwork, new CH1903LV03PlustoWGS84)

    logger.info("network and graphhopper set up")


    //get the events for each person
    val person_events = personday_triplegs
          .filterKeys(_.date == new LocalDate("2017-08-31"))
          .par.map {
          case (tr, triplegs) =>
            val waypoints: Stream[(TripLeg, List[GPXEntry])] =
              triplegs.flatMap {tlr => tripleg_waypoints.get(tlr._2.leg_id).map(tlr._2 -> _) }
            val events = waypoints.flatMap { case (trip_id, wp2) =>
              gh.gpsToEvents(wp2.asJava, Id.createPersonId(tr.user_id), Id.createVehicleId(tr.user_id)).asScala //TODO keep vehicle type here (mode : e-car)
            }
            (tr, events)
        }

    logger.info("data joined, writing data")

    //TODO: run dates in batch
    //group events by date
      person_events.groupBy { case (tr,events) => tr.date } //group by date to
        .filterKeys(_.equals(LocalDateTime.parse(" 2016-12-03")))
        .foreach { case (date, xs) =>
        println(s"processing ${date.formatted("yyyy-MM-dd")}")
        new File(OUTPUT_DIR).mkdirs
        val eventWriter = new EventWriterXML(s"${OUTPUT_DIR}/${date.formatted("yyyy-MM-dd")}-events.xml")

        val interleaved_events = xs.flatMap{_._2}.toSeq.seq.sortBy(_.getTime) //interleave events
        interleaved_events.foreach(eventWriter.handleEvent)

        eventWriter.closeFile()

      }

    //group by user / date / mode

    //get waypoints for trip_legs --indexing?

    //one loop per day to process a person-day
    //process waypoints into events



  }

}
