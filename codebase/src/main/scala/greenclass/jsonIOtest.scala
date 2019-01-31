package greenclass

package greenclass

  import java.io.File
  import java.nio.file._
  import java.sql.{DriverManager, ResultSet}
  import java.time.LocalDate
  import java.time.format.DateTimeFormatter

  import com.graphhopper.util.GPXEntry
  import ethz.ivt.graphhopperMM.{GHtoEvents, MATSimMMBuilder}
  import org.apache.log4j.{Level, Logger}
  import org.matsim.api.core.v01.{Id, Scenario, TransportMode}
  import org.matsim.contrib.emissions.utils.EmissionsConfigGroup
  import org.matsim.core.config.ConfigUtils
  import org.matsim.core.events.algorithms.EventWriterXML
  import org.matsim.core.scenario.ScenarioUtils
  import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84

  import scala.collection.JavaConverters._
  import scala.io.{BufferedSource, Source}
  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  import org.json4s.jackson.Serialization

  object jsonIOtest {

    Logger.getLogger("com.graphhopper.matching.MapMatching").setLevel(Level.WARN)
    Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

    val json_matcher: PathMatcher = FileSystems.getDefault.getPathMatcher("glob:**.json")

    def main(args: Array[String]) {

      //val args = {"P:\\Projekte\\SCCER\\switzerland_10pct\\switzerland_config_no_facilities.xml", "C:\\Projects\\spark\\green_class_swiss_triplegs.csv","C:\\Projects\\spark\\green_class_waypoints.csv","C:\\Projects\\SCCER_project\\output_gc"}


      val logger = Logger.getLogger(this.getClass)

      val json =
        """
          | {
          |		"user_id": "cE7fjaVW5Ck-11000934",
          |	    "trip_id": 1,
          |	    "date": {
          |	      "year": 2018,
          |	      "month": 3,
          |	      "day": 22
          |	    },
          |	    "legs": [{
          |           "leg_id": 0,
          |	          "started_at": "2018-03-21T19:09:19Z",
          |	          "finished_at": "2018-03-22T09:29:35Z",
          |           "start_point" : {
          |               "lat" : 47.2163,
          |               "lon" : 7.78345
          |           },
          |           "finish_point" : {
          |             "lat" : 46.94234,
          |             "lon" : 7.3892
          |           },
                  |	"mode": "Mode::activity",
                  |	"waypoints": [ [
                  |            8.509300802236313,
                  |            47.365270689916635,
                  |            1521586809527.0,
                  |            14.713000297546387
                  |            ]]
                  | }]
          | }
        """.stripMargin
      val tripleg = parse(json).extract[TripRecord]

      val tripsJSON = Serialization.writePretty(tripleg)

      println(tripsJSON)

    }
  }
