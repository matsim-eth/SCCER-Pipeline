package ethz.ivt.externalities.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.routing.Broadcast
import ethz.ivt.externalities.actors.ExternalitiesActor.EventList
import ethz.ivt.externalities.data.TripRecord
import greenclass.ProcessWaypointsJson


object EventActor {
  def props(pwj: ProcessWaypointsJson, externalitiesActor: ActorRef, eventWriterActor:ActorRef): Props =
    Props(new EventActor(pwj, externalitiesActor, eventWriterActor))

}

class EventActor (pwj: ProcessWaypointsJson, externalitiesActor : ActorRef, eventWriterActor : ActorRef)
    extends Actor with ActorLogging with ReaperWatched {
  override def receive: Receive = {
    case tr : TripRecord => {
      try {
        val events = pwj.processJson(tr)
        externalitiesActor ! EventList(tr, events.take(10))
        eventWriterActor ! EventList(tr, events)
      } catch  {
        case ex : Exception => {
          terminateOnError(ex)
        }

      }
    }
  }

  override def postStop(): Unit =  {
    log.info("Sending poison pill to writer Actors")
    externalitiesActor ! PoisonPill
    eventWriterActor ! PoisonPill
    super.postStop()
  }

}
