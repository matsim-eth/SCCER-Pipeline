package ethz.ivt.externalities.actors

import java.nio.file.Path

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ethz.ivt.externalities.actors.TraceActor.JsonFile
import greenclass.{ProcessWaypointsJson, TripRecord}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object TraceActor {
  def props(waypointProcessor: ProcessWaypointsJson, eventsActor: ActorRef): Props =
    Props(new TraceActor(waypointProcessor, eventsActor))

  final case class JsonFile(jsonFile : Path)

}

class TraceActor(waypointProcessor: ProcessWaypointsJson, eventsActor : ActorRef) extends Actor with ActorLogging {


  override def receive: Receive = {
    case JsonFile(path) =>
      log.info(s"processing folder $path")
      val trs = processJsonFile(path)
      trs.foreach(tr => {
        val jobid = tr.getIdentifier
        eventsActor ! tr
      })

  }

  private def  processJsonFile(jsonFile : Path) : Seq[TripRecord] = {
    val trList = waypointProcessor.readJson(jsonFile)
    log.info(s"reading path $jsonFile into ${trList.size} trips")
    trList
  }
}
