package greenclass

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import java.util
import java.util.stream.Collectors
import java.util.{LinkedList, List}

import ethz.ivt.MeasureExternalitiesFromTraceEvents
import ethz.ivt.externalities.data.{AggregateDataPerTimeImpl, CongestionPerLinkField}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.network.Link
import org.matsim.contrib.emissions.{WarmEmissionAnalysisModule, WarmEmissionHandler}
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils
import org.matsim.core.events.EventsManagerImpl
import org.matsim.core.scenario.ScenarioUtils

import scala.io.Source


object ProcessEvents {

  val bin_size_s = 3600

  def main(args : Array[String]) {

    val logger = Logger.getLogger(this.getClass)
    Logger.getLogger(classOf[WarmEmissionAnalysisModule]).setLevel(Level.ERROR)
    Logger.getLogger(classOf[WarmEmissionHandler]).setLevel(Level.ERROR)
    Logger.getLogger(classOf[EventsManagerImpl]).setLevel(Level.ERROR)

    //TODO: validate argumentes here
    val config = ConfigUtils.loadConfig(args(0), new EmissionsConfigGroup)
    val events_folder = Paths.get(args(1))
    val congestion_file = args(2)
    val outputFolder = args(3)
    val ncores = args.applyOrElse(4, (_ : Int) => "1").toInt

    logger.info("load aggregate congestion data")
    val scenario = ScenarioUtils.loadScenario(config)
    MeasureExternalitiesFromTraceEvents.addVehicleTypes(scenario)
    MeasureExternalitiesFromTraceEvents.setUpRoadTypes(scenario.getNetwork)

    // load precomputed aggregate data
    val attributes: util.List[String] = new util.LinkedList[String]
    attributes.add(CongestionPerLinkField.COUNT.getText)
    attributes.add(CongestionPerLinkField.DELAY.getText)
    val aggregateCongestionDataPerLinkPerTime = new AggregateDataPerTimeImpl[Link](bin_size_s, scenario.getNetwork.getLinks.keySet, attributes, null)
    aggregateCongestionDataPerLinkPerTime.loadDataFromCsv(congestion_file)


    import scala.collection.JavaConverters._

    //read list of already processed files, in case of failure
    val processedListFile = new File(outputFolder, "processed.txt" )
    if (!processedListFile.exists()) processedListFile.createNewFile()
    val filesToSkip :Set[Path] = Source.fromFile(processedListFile).getLines().map{ case x : String => Paths.get(x)}.toSet
    val pw = new PrintWriter(processedListFile)


    //get number of cores n
    //create n externality calculators
    logger.info(s"creating $ncores calculators")
    val calculators = 1 to ncores map { _ => new MeasureExternalitiesFromTraceEvents(scenario, aggregateCongestionDataPerLinkPerTime) }
    val calculatorsQueue = new java.util.concurrent.ArrayBlockingQueue[MeasureExternalitiesFromTraceEvents](ncores)
    calculatorsQueue.addAll(calculators.asJava)

    Files.walk(events_folder).iterator().asScala.toList
      .par
      .filterNot{ Files.isDirectory(_) }
      //.filter(f => f.toString.contains("2017-07-19"))
      .filter( _.toString.endsWith(".xml") )
      .foreach { f =>
        if (filesToSkip.contains(f)) logger.info(s"skipping $f")
        else {
          val externalitiyCalculator = calculatorsQueue.take()

          val person_id = f.getFileName.toString.split("-").head
          val date = f.getParent.getFileName.toString
          logger.debug(s"processing $date/$person_id")

          externalitiyCalculator.process(f.toString, date)

          externalitiyCalculator.write(outputFolder, date, person_id)
          pw.println(f)
          pw.flush()
          externalitiyCalculator.reset() //reset handlers before returning
          calculatorsQueue.put(externalitiyCalculator)
        }
      pw.close()

    }





  }
}
