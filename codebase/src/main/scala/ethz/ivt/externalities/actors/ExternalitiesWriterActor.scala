package ethz.ivt.externalities.actors

import java.nio.file.{Path, Paths}
import java.util

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ethz.ivt.externalities.counters.{ExternalityCounter, LegValues}
import greenclass.TripRecord
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.Person

import scala.concurrent.Future

object ExternalitiesWriterActor {
  def buildDefault(outputFolder :Path): Props = Props(new DefaultExtWriter(outputFolder))
}

final case class Externalities(tr : TripRecord, externalitiesCounter : ExternalityCounter)

abstract class ExternalitiesWriterActor extends Actor with ActorLogging{
  override def receive: Receive =  {
    case ext : Externalities => {
      write(ext)
      sender() ! Future.successful("done")
    }
  }

  def write(externalities: Externalities)

}

class DefaultExtWriter(outputFolder : Path) extends ExternalitiesWriterActor {
  override def write(e:  Externalities): Unit = {
    log.info("writing externalities")
    val outputFile = outputFolder.resolve(e.tr.date + "_" +  e.tr.user_id + ".csv")
    e.externalitiesCounter.writeCsvFile(outputFile)
  }
}


