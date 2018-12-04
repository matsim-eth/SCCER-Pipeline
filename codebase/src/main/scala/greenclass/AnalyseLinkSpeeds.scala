package greenclass

import java.io.{BufferedWriter, File, FileWriter, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import java.util

import ethz.ivt.MeasureExternalitiesFromTraceEvents
import ethz.ivt.externalities.data.{AggregateDataPerTimeImpl, CongestionField}
import ethz.ivt.greenclass.LinkSpeedHandler
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.network.Link
import org.matsim.api.core.v01.{Id, Scenario}
import org.matsim.contrib.emissions.{WarmEmissionAnalysisModule, WarmEmissionHandler}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.{EventsManagerImpl, MatsimEventsReader}
import org.matsim.core.network.NetworkUtils
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.utils.gis.matsim2esri.network.FreespeedBasedWidthCalculator
import org.matsim.vehicles.VehicleType

import scala.collection.JavaConverters._
import scala.io.Source



object AnalyseLinkSpeeds {

  def main(args: Array[String]) {

    Logger.getLogger(classOf[WarmEmissionAnalysisModule]).setLevel(Level.ERROR)
    Logger.getLogger(classOf[WarmEmissionHandler]).setLevel(Level.ERROR)
    Logger.getLogger(classOf[EventsManagerImpl]).setLevel(Level.ERROR)

    val runner = new AnalyseLinkSpeeds()
    runner.run(args)

  }
}

class AnalyseLinkSpeeds {

  val logger = Logger.getLogger(this.getClass)
  val bin_size_s = 3600

  def run(args: Array[String]): Unit = {
    //TODO: validate argumentes here
    val config = ConfigUtils.loadConfig(args(0), new EmissionsConfigGroup)
    val events_folder = Paths.get(args(1))
    val congestion_file = args(2)
    val car_ownership_file = args(3)
    val outputFolder = args(4)
    val ncores = args.applyOrElse(5, (_: Int) => "1").toInt

    val scenario = ScenarioUtils.loadScenario(config)
    MeasureExternalitiesFromTraceEvents.addVehicleTypes(scenario)
    MeasureExternalitiesFromTraceEvents.setUpRoadTypes(scenario.getNetwork)

    val linkSpeedHandler = new LinkSpeedHandler()
    val eventsManager = new EventsManagerImpl
    eventsManager.addHandler(linkSpeedHandler)
    val reader = new MatsimEventsReader(eventsManager)

    logger.info("processing files")
    Files.walk(events_folder).iterator().asScala.toList
      .filterNot {
        Files.isDirectory(_)
      }
      //.filter(f => f.toString.contains("2017-05-08"))
      .filter(_.toString.endsWith(".xml"))
    //  .take(20)
      .foreach { eventsFile =>
        val person_id = eventsFile.getFileName.toString.split("-").head
        val date = eventsFile.getParent.getFileName.toString
        logger.debug(s"processing $date/$person_id")
        eventsManager.resetHandlers(0)
        reader.readFile(eventsFile.toString)

      }


    val link_records : Iterable[TimingSummary] = linkSpeedHandler.getLinkTimings.asScala.map{ case (link_id, list) =>
      val link = scenario.getNetwork.getLinks.get(link_id)
      val slist = list.asScala.map(t => (link.getLength / t.getTravelTime)  * 3.6)
      TimingSummary(link_id, slist.length, link.getFreespeed * 3.6, slist.min, slist.sum/slist.size, slist.max);
    }

    link_records.take(10).foreach(println(_))

    val num_records : Long = linkSpeedHandler.getLinkTimings.asScala.map(_._2.size()).sum
    println("number links " + link_records.size)
    println("number records " + num_records + ", average recs/link:" + (num_records/link_records.size))

    val num_slower_links = link_records.count(r => r.max < 0.8 * r.freespeed)
    println("percent slower links: " + (num_slower_links.toDouble / link_records.size * 100))

    val file = new File(Paths.get(outputFolder, "linkspeed.csv").toString)
    val bw = new BufferedWriter(new FileWriter(file))
    val DELIMITER = ";"
    bw.write(List("entry_time_s","link_id","roadType","length_m","freespeed_ms","actual_speed_ms").mkString(DELIMITER))
    bw.newLine()

    linkSpeedHandler.getLinkTimings.values.asScala.foreach { list =>
      list.asScala.foreach( ts => {
        val link = scenario.getNetwork.getLinks.get(ts.link_id)
        val roadType = NetworkUtils.getType(link)
        val actual_speed = link.getLength / ts.getTravelTime
        val rec = List(ts.entry_time, ts.link_id, roadType, link.getLength, link.getFreespeed, actual_speed)

        if (ts.entry_time.equals(Double.NaN) || ts.exit_time.equals(Double.NaN) || actual_speed.equals(Double.NaN) ) {
          println("SKIPPED: " + rec.toString())
        } else {
          bw.write(rec.mkString(DELIMITER))
          bw.newLine()
        }
      })
    }
    //time, link_id, road_type, link_length, travel time, ff_speed, actual speed
    bw.close()

  }


}

case class TimingSummary(link_id: Id[Link], num_records: Int, freespeed: Double, min: Double, average: Double, max: Double)
