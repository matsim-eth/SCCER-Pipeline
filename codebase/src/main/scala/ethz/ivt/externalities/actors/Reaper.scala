package ethz.ivt.externalities.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object Reaper {
  val name = "reaper"
  // Used by others to register an Actor for watching
  case class WatchMe(ref: ActorRef)
  case class TerminateOnError(ref: ActorRef, ex: Exception)


}

class Reaper extends Actor with ActorLogging {
  import Reaper._

  // Keep track of what we're watching
  val watched = ArrayBuffer.empty[ActorRef]

  // Derivations need to implement this method.  It's the
  // hook that's called when everything's dead
  def allSoulsReaped() = {
    log.info("SYSTEM SHUTDOWN START")
    context.system.terminate()
    log.info("SYSTEM SHUTDOWN END")
  }

  // Watch and check for termination
  final def receive = {
    case WatchMe(ref) =>
      context.watch(ref)
      watched += ref
      log.info("added ref: " + ref.path)
    case Terminated(ref) =>
      watched -= ref
      log.info("terminating ref: " + ref.path)
      if (watched.isEmpty) allSoulsReaped()
    case TerminateOnError(ref, ex) =>
      context.system.registerOnTermination(System.exit(1))
      allSoulsReaped()
      log.error(ex, "Destroying actor system due to error on ", ref)
  }

  def terminateOnError = {
    allSoulsReaped()
  }
}

trait ReaperWatched { this: Actor =>
  override def preStart() {
    context.actorSelection("/user/" + Reaper.name) ! Reaper.WatchMe(self)
  }

  def terminateOnError(ex : Exception) {
    context.actorSelection("/user/" + Reaper.name) ! Reaper.TerminateOnError(self, ex)

  }
}
