package greenclass

import java.io.{File, FileInputStream, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.stream.Collectors
import java.util.{LinkedList, List}

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import ethz.ivt.MeasureExternalitiesFromTraceEvents
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader
import ethz.ivt.externalities.data.{AggregateDataPerTimeImpl, CongestionField}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.{Id, Scenario}
import org.matsim.api.core.v01.network.Link
import org.matsim.contrib.emissions.{WarmEmissionAnalysisModule, WarmEmissionHandler}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.EventsManagerImpl
import org.matsim.core.scenario.ScenarioUtils
import org.matsim.vehicles.VehicleType

import scala.io.Source
import scala.collection.JavaConverters._


object ProcessEvents {


  def main(args : Array[String]) {

    Logger.getLogger(classOf[WarmEmissionAnalysisModule]).setLevel(Level.ERROR)
    Logger.getLogger(classOf[WarmEmissionHandler]).setLevel(Level.ERROR)
    Logger.getLogger(classOf[EventsManagerImpl]).setLevel(Level.ERROR)

    val runner = new ProcessEvents()
    runner.run(args)

  }
}

class ProcessEvents {

  val logger = Logger.getLogger(this.getClass)
  val bin_size_s = 3600

  def run(args: Array[String]): Unit = {
    //TODO: validate argumentes here
    val config = ConfigUtils.loadConfig(args(0), new EmissionsConfigGroup)
    val events_folder = Paths.get(args(1))
    val congestion_file = args(2)
    val car_ownership_file = args(3)
    val outputFolder = args(4)
    val costValuesFile = args(5)
    val ncores = args.applyOrElse(6, (_: Int) => "1").toInt

    val scenario = ScenarioUtils.loadScenario(config)
    MeasureExternalitiesFromTraceEvents.addVehicleTypes(scenario)
    MeasureExternalitiesFromTraceEvents.setUpRoadTypes(scenario.getNetwork)

    if (car_ownership_file != "") HelperFunctions.addVehiclesToScenario(scenario, car_ownership_file)

    //add cars from csv file

    // load precomputed aggregate data
    logger.info("load aggregate congestion data")
    val aggregateCongestionDataPerLinkPerTime = CSVCongestionReader.forLink().read(congestion_file, bin_size_s)

    //read list of already processed files, in case of failure
    new File(outputFolder).mkdir()
    val processedListFile = new File(outputFolder, "processed.txt")
    if (!processedListFile.exists()) processedListFile.createNewFile()
    val filesToSkip: Set[Path] = Source.fromFile(processedListFile).getLines().map { case x: String => Paths.get(x) }.toSet
    val pw = new PrintWriter(processedListFile)


    //get number of cores n
    //create n externality calculators
    logger.info(s"creating $ncores calculators")
    val externalitiyCalculator = new MeasureExternalitiesFromTraceEvents(scenario, aggregateCongestionDataPerLinkPerTime, costValuesFile)


    logger.info("processing files")
    Files.walk(events_folder).iterator().asScala.toList
      .filterNot {
        Files.isDirectory(_)
      }
      //.filter(f => f.toString.contains("2017-05-08"))
      .filter(f => f.toString.endsWith(".xml") || f.toString.endsWith(".xml.gz"))
      .foreach { f =>
        if (filesToSkip.contains(f)) logger.info(s"skipping $f")
        else {
          val person_id = f.getFileName.toString.split("-").head
          val date = f.getParent.getFileName.toString
          logger.debug(s"processing $date/$person_id")

          externalitiyCalculator.process(f.toString, date, person_id)

          externalitiyCalculator.write(outputFolder, date, person_id)
          pw.println(f)
          pw.flush()
          externalitiyCalculator.reset() //reset handlers before returning
        }
        pw.close()

      }

  }


}
