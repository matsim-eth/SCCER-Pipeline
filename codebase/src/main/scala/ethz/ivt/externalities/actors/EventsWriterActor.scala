package ethz.ivt.externalities.actors

import java.io.File
import java.nio.file.{Path, Paths}

import akka.actor.{Actor, ActorRef, Props}
import com.vividsolutions.jts.geom.GeometryFactory
import ethz.ivt.externalities.actors.ExternalitiesActor.EventList
import ethz.ivt.externalities.data.TripRecord
import greenclass.ProcessWaypointsJson
import org.geotools.geojson.feature.FeatureJSON
import org.geotools.geojson.geom.GeometryJSON
import org.matsim.api.core.v01.Scenario

import scala.collection.JavaConverters._

object EventsWriterActor {
  def props(scenario: Scenario, traces_output_dir:String): Props =
    Props(new EventsWriterActor(scenario, Paths.get(traces_output_dir)))

}

class EventsWriterActor (scenario: Scenario, traces_output_dir: Path)
  extends Actor with ReaperWatched {

  import org.geotools.feature.simple.SimpleFeatureTypeBuilder

  val b = new SimpleFeatureTypeBuilder

  override def receive: Receive = {
    case EventList(tr, legs) => {
      val gf = new GeometryFactory()
      val geojson = new GeometryJSON()

      legs.filter(_._2.nonEmpty).foreach{ case (leg_id, _, geometry) =>
        geojson.write(geometry, traces_output_dir.resolve("geometry_" + leg_id + ".json").toFile)
      }
    }
  }
}
