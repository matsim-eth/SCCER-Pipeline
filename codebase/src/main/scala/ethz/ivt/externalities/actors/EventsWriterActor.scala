package ethz.ivt.externalities.actors

import java.io.File
import java.nio.file.{Files, Path, Paths}
import akka.actor.{Actor, ActorLogging, Props}
import ethz.ivt.externalities.counters.ExtendedPersonDepartureEvent
import ethz.ivt.externalities.data.TripRecord
import org.apache.log4j.Logger
import org.locationtech.jts.geom.Geometry
import org.matsim.api.core.v01.Scenario
import org.matsim.api.core.v01.events.Event
import org.matsim.core.events.algorithms.EventWriterXML


case class EventTriple(leg_id: String, events: Seq[Event], geometry: Geometry)
final case class EventList(tr: TripRecord, events : Stream[EventTriple])

class EventsWriterActor (scenario: Scenario, traces_output_dir: Path){

  val logger = Logger.getLogger(this.getClass)

  def unwrapDepartureEvents(event: Event): Event = {
    event match {
      case event1: ExtendedPersonDepartureEvent =>
        event1.getPersonDepartureEvent
      case e => e
    }
  }

  override def write = {
    case EventList(tr, legs) =>
      logger.info(s"writing ${legs.size} events on ${tr.date} for ${tr.user_id}")
      import collection.JavaConverters._
      //calculate externalities here
      val events : Seq[Event] = legs.flatMap(_.events)

      val date_folder = Files.createDirectories(traces_output_dir.resolve(s"${tr.date}"))

      val filename : String = s"events_${tr.user_id}_${tr.date}.xml"
      val eventWriter : EventWriterXML = new EventWriterXML(date_folder.resolve(filename).toFile.getAbsolutePath)

      events.map(unwrapDepartureEvents).foreach(eventWriter.handleEvent)

      eventWriter.closeFile()
  }
}
