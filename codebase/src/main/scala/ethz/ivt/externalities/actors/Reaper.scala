package ethz.ivt.externalities.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import ethz.ivt.externalities.actors.ReaperActor.Reap

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

case class KillSwitch()

trait SuicideActor extends  Actor with ActorLogging {

  def killSwitch(block: => Unit): Unit = {

    log.info(s"Actor ${self.path.name} is commencing suicide sequence...")
    context become PartialFunction.empty
    val children = context.children
    val reaper = context.system.actorOf(ReaperActor.props(self), s"ReaperFor${self.path.name}")
    reaper ! Reap(children.toSeq)
  }

  override def postStop(): Unit = log.debug(s"Actor ${self.path.name} is dead.")

}

object ReaperActor {

  case class Reap(underWatch: Seq[ActorRef])

  def props(supervisor: ActorRef): Props = {
    Props(new ReaperActor(supervisor))
  }
}

class ReaperActor(supervisor: ActorRef) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info(s"Reaper for ${supervisor.path.name} started")

  override def receive: Receive = {
    case Reap(underWatch) =>
      if (underWatch.isEmpty) {
        killLeftOvers
      } else {
        underWatch.foreach(context.watch)
        context become reapRemaining(underWatch.size)
        underWatch.foreach(_ ! PoisonPill)
      }
  }

  def reapRemaining(livingActorsNumber: Int): Receive = {
    case Terminated(_) =>
      val remainingActorsNumber = livingActorsNumber - 1
      if (remainingActorsNumber == 0) {
        killLeftOvers
      } else {
        context become reapRemaining(remainingActorsNumber)
      }
  }

  private def killLeftOvers = {
    log.debug(s"All children of ${supervisor.path.name} are dead killing supervisor")
    supervisor ! PoisonPill
    self ! PoisonPill
  }
}