package greenclass

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.stream.Collectors
import java.util.{LinkedList, List}

import ethz.ivt.MeasureExternalitiesFromTraceEvents
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
    val ncores = args.applyOrElse(5, (_: Int) => "1").toInt

    val scenario = ScenarioUtils.loadScenario(config)
    MeasureExternalitiesFromTraceEvents.addVehicleTypes(scenario)
    MeasureExternalitiesFromTraceEvents.setUpRoadTypes(scenario.getNetwork)

    if (car_ownership_file != "") addVehiclesToScenario(scenario, car_ownership_file)

    //add cars from csv file

    // load precomputed aggregate data
    val attributes: util.List[String] = new util.LinkedList[String]
    attributes.add(CongestionField.COUNT.getText)
    attributes.add(CongestionField.DELAY_CAUSED.getText)
    attributes.add(CongestionField.DELAY_EXPERIENCED.getText)
    val aggregateCongestionDataPerLinkPerTime = new AggregateDataPerTimeImpl[Link](bin_size_s, scenario.getNetwork.getLinks.keySet, attributes, null)
    logger.info("load aggregate congestion data")
    aggregateCongestionDataPerLinkPerTime.loadDataFromCsv(congestion_file)


    //read list of already processed files, in case of failure
    new File(outputFolder).mkdir()
    val processedListFile = new File(outputFolder, "processed.txt")
    if (!processedListFile.exists()) processedListFile.createNewFile()
    val filesToSkip: Set[Path] = Source.fromFile(processedListFile).getLines().map { case x: String => Paths.get(x) }.toSet
    val pw = new PrintWriter(processedListFile)


    //get number of cores n
    //create n externality calculators
    logger.info(s"creating $ncores calculators")
    val externalitiyCalculator = new MeasureExternalitiesFromTraceEvents(scenario, aggregateCongestionDataPerLinkPerTime)

    logger.info("processing files")
    Files.walk(events_folder).iterator().asScala.toList
      .filterNot {
        Files.isDirectory(_)
      }
      //.filter(f => f.toString.contains("2017-05-08"))
      .filter(_.toString.endsWith(".xml"))
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


  def addVehiclesToScenario(scenario: Scenario, car_ownership_file: String) : Unit = {
    val input = Source.fromFile(car_ownership_file).getLines().map(_.split(",").map(_.stripPrefix("\"").stripSuffix("\"")))
    val headers = input.next().tail
    val car_ownership_ds = input.map(ss => ss.head -> headers.zip(ss.tail).toMap).toMap

    //add vehicle for id, and first car ->  car_1_engine
    val vehicleTypes = scenario.getVehicles.getVehicleTypes

    car_ownership_ds.foreach { case (k, value_map) =>
      val vid = Id.createVehicleId(k)
      val vid_ecar = Id.createVehicleId(vid + "Mode::Ecar")

      val benzinTypeId = Id.create(value_map.getOrElse("car_1_engine", "Benzin"), classOf[VehicleType])

      val engineTypeId = Id.create(value_map.getOrElse("car_1_engine", "Benzin"), classOf[VehicleType])

      vehicleTypes.asScala.get(engineTypeId)
        .foreach{ engineType =>
          val v = scenario.getVehicles.getFactory.createVehicle(vid, engineType)
          val v_ecar = scenario.getVehicles.getFactory.createVehicle(vid_ecar, engineType)
          scenario.getVehicles.addVehicle(v)
          scenario.getVehicles.addVehicle(v_ecar)
        }
      //default back to petrol if not found.
    }
  }

}
