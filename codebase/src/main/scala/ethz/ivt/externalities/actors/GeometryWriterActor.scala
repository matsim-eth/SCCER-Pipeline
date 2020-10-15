package ethz.ivt.externalities.actors

import java.io.File
import java.nio.file.{Path, Paths}

import akka.actor.{Actor, Props}
import org.geotools.geojson.geom.GeometryJSON
import org.matsim.api.core.v01.Scenario

object GeometryWriterActor {
  def props(scenario: Scenario, traces_output_dir:String): Props =
    Props(new GeometryWriterActor(scenario, Paths.get(traces_output_dir)))

}

class GeometryWriterActor (scenario: Scenario, traces_output_dir: Path)
  extends Actor {

  import org.geotools.feature.simple.SimpleFeatureTypeBuilder

  val b = new SimpleFeatureTypeBuilder

  override def receive: Receive = {
    case EventList(tr, legs) =>
      val geojson = new GeometryJSON()

      legs.filter(_.events.nonEmpty).foreach{ case EventTriple(leg_id, _, geometry) =>
        val filename : String = s"geometry_${tr.user_id}_${tr.date}_${leg_id}.json"
        geojson.write(geometry, traces_output_dir.resolve(filename).toFile)
      }
  }
}
