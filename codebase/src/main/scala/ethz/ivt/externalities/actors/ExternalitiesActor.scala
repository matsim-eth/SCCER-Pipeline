package ethz.ivt.externalities.actors

import java.time.LocalDate

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import ethz.ivt.externalities.MeasureExternalities
import ethz.ivt.externalities.counters.LegValues
import ethz.ivt.externalities.data.TripRecord
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.events.Event
import org.matsim.api.core.v01.population.Person

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ExternalitiesActor {
  def props(meCreator: () => MeasureExternalities, writerActorProps : Props): Props =
    Props(new ExternalitiesActor(meCreator(), writerActorProps))

  final case class EventList(tr: TripRecord, events : Stream[Event])

}

class ExternalitiesActor(measureExternalities: MeasureExternalities, writerActorProps : Props) extends Actor with ActorLogging {

  import ExternalitiesActor._
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 5 minutes

  val writerActor : ActorRef = context.actorOf(writerActorProps, "Writer")

  override def receive: Receive = {
    case EventList(tr, events) => {
      log.info(s"processing ${events.size} on ${tr.date} for ${tr.user_id}")
      import collection.JavaConverters._
      //calculate externalities here
      val externalities = measureExternalities.process(events.toList.asJava, tr.date)
      writerActor ! Externalities(tr, externalities)

    }
  }

}


