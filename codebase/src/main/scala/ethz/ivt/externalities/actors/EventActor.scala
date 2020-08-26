package ethz.ivt.externalities.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.routing.Broadcast
import ethz.ivt.externalities.actors.ExternalitiesActor.EventList
import ethz.ivt.externalities.data.TripRecord
import greenclass.ProcessWaypointsJson


object EventActor {
  def props(pwj: ProcessWaypointsJson, externalitiesActorOption: Option[ActorRef], eventWriterActor:Option[ActorRef]): Props =
    Props(new EventActor(pwj, externalitiesActorOption, eventWriterActor))

}

class EventActor (pwj: ProcessWaypointsJson, externalitiesActorOption : Option[ActorRef], eventWriterActor : Option[ActorRef])
    extends Actor with ActorLogging with ReaperWatched {
  override def receive: Receive = {
    case tr : TripRecord => {
      try {
        val events = pwj.processJson(tr)
        externalitiesActorOption.foreach(a => a ! EventList(tr, events))
        eventWriterActor.foreach(a => a ! EventList(tr, events))
      } catch  {
        case ex : Exception => {
          terminateOnError(ex)
        }

      }
    }
  }

  override def postStop(): Unit =  {
    log.info("Sending poison pill to writer Actors")
    externalitiesActorOption.foreach(a => a ! PoisonPill)
    eventWriterActor.foreach(a => a  ! PoisonPill)
    super.postStop()
  }

}
