package ethz.ivt.externalities.actors

import akka.actor.{Actor, ActorRef, Props}
import ethz.ivt.externalities.actors.ExternalitiesActor.EventList
import ethz.ivt.externalities.data.TripRecord
import greenclass.ProcessWaypointsJson


object EventActor {
  def props(pwj: ProcessWaypointsJson, externalitiesActor: ActorRef, eventWriterActor:ActorRef): Props =
    Props(new EventActor(pwj, externalitiesActor, eventWriterActor))

}

class EventActor (pwj: ProcessWaypointsJson, externalitiesActor : ActorRef, eventWriterActor : ActorRef) extends Actor {
  override def receive: Receive = {
    case tr : TripRecord => {
      val events = pwj.processJson(tr)
      externalitiesActor ! EventList(tr, events)
      eventWriterActor ! EventList(tr, events)
    }
  }
}
