package ethz.ivt.externalities.actors

import akka.actor.{Actor, Props, Terminated}


class ParentActor extends Actor {
  val child = context.actorOf(Props.empty, "child")
  context.watch(child) // <-- this is the only call needed for registration
  var lastSender = context.system.deadLetters

  def receive = {
    case "kill" =>
      context.stop(child); lastSender = sender()
    case Terminated(`child`) => lastSender ! "finished"
  }

  def initialize = {

  }
}
