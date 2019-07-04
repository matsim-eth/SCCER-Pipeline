package ethz.ivt.externalities.actors

import java.sql.SQLException
import java.time.LocalDate

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.util.Timeout
import org.locationtech.jts.geom.Geometry
import ethz.ivt.externalities.MeasureExternalities
import ethz.ivt.externalities.counters.LegValues
import ethz.ivt.externalities.data.TripRecord
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.events.Event
import org.matsim.api.core.v01.network.Link
import org.matsim.api.core.v01.population.Person

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Failure

object ExternalitiesActor {
  def props(meCreator: () => MeasureExternalities, writerActor : ActorRef): Props =
    Props(new ExternalitiesActor(meCreator, writerActor))

  final case class EventList(tr: TripRecord, events : Stream[(Long, Seq[Event], Geometry)])


}

class ExternalitiesActor(meCreator: () => MeasureExternalities, writerActor : ActorRef)
  extends Actor with ActorLogging with ReaperWatched {

  import ExternalitiesActor._
  import akka.pattern.ask
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 5 minutes

  var measureExternalities : MeasureExternalities = _

  override def preStart() = {
    super.preStart
    measureExternalities = meCreator() // this takes a few seconds to complete
  }

  override def receive: Receive = {
    case EventList(tr, legs) => {
      log.info(s"processing ${legs.size} on ${tr.date} for ${tr.user_id}")
      import collection.JavaConverters._
      //calculate externalities here
      val events : java.util.List[Event] = legs.unzip3._2.flatten.asJava
      try {
        val externalities = measureExternalities.process(events, tr.date.atStartOfDay())
        writerActor ? Externalities(tr, externalities) onComplete {
          case Failure(e: SQLException) => log.error(e, "Error writing externalities")
          case _ => log.info("Externalities written successfully")
        }
      } catch {
        case ex: RuntimeException => log.error(ex, ex.getMessage);
      }

    }
  }

}


