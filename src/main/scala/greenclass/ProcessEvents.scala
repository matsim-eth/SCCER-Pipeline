package greenclass

import java.nio.file.{Files, Paths}
import java.util
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

    //get number of cores n
    //create n externality calculators
    val calculators = 1 to ncores map { _ => new MeasureExternalitiesFromTraceEvents(scenario, aggregateCongestionDataPerLinkPerTime) }
    val calculatorsQueue = new java.util.concurrent.ArrayBlockingQueue[MeasureExternalitiesFromTraceEvents](ncores)
    calculatorsQueue.addAll(calculators.asJava)

    Files.walk(events_folder).filter(Files.isRegularFile(_)).filter(f => f.toString.contains("2017-07-19")).forEach {
      f =>
        val externalitiyCalculator = calculatorsQueue.take()
        externalitiyCalculator.process(f.toString)
        val date = f.getParent.getFileName.toString
        val person_id = f.getFileName.toString.split("-").head
        logger.debug(s"processing $date/$person_id")


        externalitiyCalculator.write(outputFolder, date, person_id)

        calculatorsQueue.put(externalitiyCalculator)

    }





  }
}
