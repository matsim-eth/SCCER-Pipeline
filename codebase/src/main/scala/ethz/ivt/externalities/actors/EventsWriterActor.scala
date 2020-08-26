package ethz.ivt.externalities.actors

import java.io.File
import java.nio.file.{Path, Paths}

import akka.actor.{Actor, ActorRef, Props}
import org.locationtech.jts.geom.GeometryFactory
import ethz.ivt.externalities.actors.ExternalitiesActor.EventList
import org.geotools.geojson.geom.GeometryJSON
import org.matsim.api.core.v01.Scenario

import scala.collection.JavaConverters._

object EventsWriterActor {
  def props(scenario: Scenario, traces_output_dir:String): Props =
    Props(new EventsWriterActor(scenario, Paths.get(traces_output_dir)))

}

class EventsWriterActor (scenario: Scenario, traces_output_dir: Path)
  extends Actor with ReaperWatched {

  import org.geotools.feature.simple.SimpleFeatureTypeBuilder

  val b = new SimpleFeatureTypeBuilder

  override def receive: Receive = {
    case EventList(tr, legs) => {
      log.info(s"writing ${legs.size} on ${tr.date} for ${tr.user_id}")
      import collection.JavaConverters._
      //calculate externalities here
      val events : Seq[Event] = legs.unzip3._2.flatten

      val filename : String = "events_${tr.user_id}_${tr.date}.xml"
      val eventWriter : EventWriterXML = new EventWriterXML(filename)

      events.foreach(e -> eventWriter.handleEvent(e))

      eventWriter.closeFile()
    }
  }
}
