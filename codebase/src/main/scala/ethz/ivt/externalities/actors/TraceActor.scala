package ethz.ivt.externalities.actors

import java.nio.file.Path
import java.time.LocalDate
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import akka.routing.Broadcast
import akka.util.Timeout
import ethz.ivt.externalities.{MeasureExternalities, MeasureExternalitiesInterface}
import ethz.ivt.externalities.actors.TraceActor.JsonFile
import ethz.ivt.externalities.data.TripRecord
import greenclass.ProcessWaypointsJson
import org.locationtech.jts.geom.Geometry
import org.matsim.api.core.v01.events.Event

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Failure

object TraceActor {
  def props(waypointProcessor: ProcessWaypointsJson,
            eventWriterProps: Option[Props],
            geometryWriterOption: Option[GeometryWriterActor],
            meCreator: () => MeasureExternalitiesInterface,
            extWriterProps : Option[Props]
           ): Props =
    Props(new TraceActor(waypointProcessor, eventWriterProps, geometryWriterOption, meCreator, extWriterProps))

  final case class JsonFile(jsonFile : Path)

}

class TraceActor(waypointProcessor: ProcessWaypointsJson,
                 eventWriterProps : Option[Props],
                 geometryWriterOption: Option[GeometryWriterActor],
                 meCreator : () => MeasureExternalitiesInterface,
                 extWriterProps : Option[Props])
  extends Actor with ActorLogging with SuicideActor {

  val eventWriterActor = eventWriterProps.map(context.actorOf(_, "EventsWriterActor"))
  val writerActor = extWriterProps.map(context.actorOf(_, "ExternalitiesWriter"))
  val measureExternalities : MeasureExternalitiesInterface = meCreator()

  implicit val timeout: Timeout = 5 minutes
  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case JsonFile(path) =>
      log.info(s"processing folder $path")
      val trs = processJsonFile(path)
      trs
      //  .filter(tr => tr.date.isBefore(LocalDate.of(2020, 9, 1)))
        .foreach(tr => {
          val jobid = tr.getIdentifier
          val legs = waypointProcessor.processJson(tr)
          eventWriterActor.foreach( w =>  w ! legs )
          geometryWriterOption.foreach(_.write(legs))
        processExternalities(tr, legs)

      })
    case KillSwitch => killSwitch()

  }

  override def postStop() {
    context.system.terminate()
    log.info("Finished processing")
    System.exit(0)
  }

  private def processExternalities(tr : TripRecord, legs: Stream[EventTriple]): Unit = {
    log.info(s"processing ${legs.size} on ${tr.date} for ${tr.user_id}")
    import collection.JavaConverters._
    //calculate externalities here
    val events: java.util.List[Event] = legs.flatMap(_.events).asJava

    val externalities = measureExternalities.process(events, tr.date.atStartOfDay())
    writerActor.foreach(wa => {
      val writerResult = wa ? Externalities(tr, externalities)
      Await.ready(writerResult, Duration.Inf) onComplete {
        case Failure(e: Exception) => log.error(e, "Error writing externalities")
        case _ => log.info("Externalities written successfully")
      }
    })
  }

  private def  processJsonFile(jsonFile : Path) : Seq[TripRecord] = {
    val trList = waypointProcessor.readJson(jsonFile)
    log.info(s"read path $jsonFile into ${trList.size} trips")
    trList
  }

}
