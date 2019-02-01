package ethz.ivt.externalities.actors

import java.util

import akka.actor.Status.Success
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ethz.ivt.externalities.counters.LegValues
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.Person

import scala.concurrent.Future

object ExternalitiesWriters {
  def buildDefault(): Props = Props(new DefaultExtWriter())
}

final case class Externalities(externalities : util.Map[Id[Person], util.List[LegValues]])

abstract class ExternalitiesWriterActor extends Actor with ActorLogging{
  override def receive: Receive =  {
    case Externalities(externalities) => {
      write(externalities)
      sender() ! Future.successful("done")
    }
  }

  def write(externalities:  util.Map[Id[Person], util.List[LegValues]])

}

class DefaultExtWriter extends ExternalitiesWriterActor {
  override def write(externalities:  util.Map[Id[Person], util.List[LegValues]]): Unit = {
    log.info("writing nothing")
  }
}


