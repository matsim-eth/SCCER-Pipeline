package ethz.ivt.externalities.actors

import akka.actor.{Actor, ActorRef, Props}
import ethz.ivt.externalities.actors.ExternalitiesActor.EventList
import ethz.ivt.externalities.data.TripRecord
import greenclass.ProcessWaypointsJson


object EventActor {
  def props(pwj: ProcessWaypointsJson, externalitiesActor: ActorRef): Props =
    Props(new EventActor(pwj, externalitiesActor))

}

class EventActor (pwj: ProcessWaypointsJson, externalitiesActor : ActorRef) extends Actor {
  override def receive: Receive = {
    case tr : TripRecord => externalitiesActor ! EventList(tr, pwj.processJson(Stream(tr)))
  }
}
