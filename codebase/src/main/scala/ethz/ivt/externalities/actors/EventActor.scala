package ethz.ivt.externalities.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.routing.Broadcast
import ethz.ivt.externalities.actors.ExternalitiesActor.EventList
import ethz.ivt.externalities.data.TripRecord
import greenclass.ProcessWaypointsJson


object EventActor {
  def props(pwj: ProcessWaypointsJson, externalitiesPropsOption: Option[Props], eventWriterPropsOption:Option[Props]): Props =
    Props(new EventActor(pwj, externalitiesPropsOption, eventWriterPropsOption))

}

class EventActor (pwj: ProcessWaypointsJson,
                  externalitiesPropsOption : Option[Props],
                  eventWriterPropsOption : Option[Props])
    extends Actor with ActorLogging with ReaperWatched {

  val externalitiesActor = externalitiesPropsOption.map(context.actorOf(_, "ExternaltiesActor"))
  val eventWriterActor = eventWriterPropsOption.map(context.actorOf(_, "EventsWriterActor"))

  override def receive: Receive = {
    case tr : TripRecord => {
      try {
        val events = pwj.processJson(tr)
        externalitiesActor.foreach(a => a ! EventList(tr, events))
        eventWriterActor.foreach(a => a ! EventList(tr, events))
      } catch  {
        case ex : Exception => {
          System.out.println(ex)
          terminateOnError(ex)
        }

      }
    }
  }

}
