package ethz.ivt.externalities.actors

import java.time.LocalDate
import java.util

import akka.actor.Status.{Failure, Success}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import ethz.ivt.externalities.MeasureExternalities
import ethz.ivt.externalities.counters.LegValues
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.events.Event
import org.matsim.api.core.v01.population.Person

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ExternalitiesActor {
  def props(me: MeasureExternalities): Props =
    Props(new ExternalitiesActor(me))

  final case class EventList(date: LocalDate, events : Stream[Event])

}

class ExternalitiesActor(measureExternalities: MeasureExternalities) extends Actor with ActorLogging {

  import ExternalitiesActor._
  import akka.pattern.{ ask, pipe }
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 5 minutes

  val writerActorProps : Props = ExternalitiesWriters.buildDefault()
  val writerActor : ActorRef = context.actorOf(writerActorProps, "Writer")


  override def receive: Receive = {
    case EventList(date, events) => {
      log.info(s"processing ${events.size}")
      import collection.JavaConverters._
      //calculate externalities here
      val externalities = measureExternalities.process(events.toList.asJava, date)
      writerActor ! Externalities(externalities)

    }
  }

  def processEvents(events: List[Event], date: LocalDate): Unit = {
    val externalities = measureExternalities.process(events.asJava, date)
    writerActor ! Externalities(externalities)
  }
}


