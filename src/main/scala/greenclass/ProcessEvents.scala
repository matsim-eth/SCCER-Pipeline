package greenclass

import java.nio.file.Paths

import ethz.ivt.MeasureExternalitiesFromTraceEvents
import org.apache.log4j.Logger
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
import org.matsim.core.config.ConfigUtils


object ProcessEvents {


  def main(args : Array[String]) {

    val logger = Logger.getLogger(this.getClass)


    val config = ConfigUtils.loadConfig(args(0), new EmissionsConfigGroup)
    val events_file = args(1)
    val congestion_file = args(2)
    val outputFolder = args(3)
    val date = Paths.get(events_file).getParent.getFileName.toString
    val person_id = Paths.get(events_file).getFileName.toString.split("-").head

    //TODO: validate argumentes here

    //collect all events for one day
    val externalitiyCalculator = new MeasureExternalitiesFromTraceEvents(config, congestion_file)

    //combine events and interleave
    externalitiyCalculator.process(events_file)

    externalitiyCalculator.write(outputFolder, date, person_id)




  }
}
