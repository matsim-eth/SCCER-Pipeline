package ethz.ivt.externalities.actors

import java.io.File
import java.nio.file.{Path, Paths}
import akka.actor.{Actor, ActorLogging, Props}
import org.apache.log4j.Logger
import org.geotools.geojson.geom.GeometryJSON
import org.matsim.api.core.v01.Scenario
import org.geotools.feature.simple.SimpleFeatureTypeBuilder

class GeometryWriterActor (scenario: Scenario, traces_output_dir: Path) {
    val logger = Logger.getLogger(this.getClass)


  val b = new SimpleFeatureTypeBuilder

  def write(legs: Stream[EventTriple]) = {

    val geojson = new GeometryJSON()

    legs.filter(_.events.nonEmpty).foreach { case EventTriple(leg_id, _, geometry) =>
      val filename: String = s"geometry_${leg_id}.json"

      logger.info(s"writing ${legs.size} leg geometries to ${filename}")

      geojson.write(geometry, traces_output_dir.resolve(filename).toFile)
    }
  }
}
