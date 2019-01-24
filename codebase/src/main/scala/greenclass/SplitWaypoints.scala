package greenclass

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path, Paths}
import java.sql.{Connection, DriverManager, PreparedStatement, Statement}
import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter

import org.apache.log4j.{Level, Logger}

import scala.io.Source
import org.json4s.jackson.Serialization

object SplitWaypoints {


  Logger.getLogger("com.graphhopper.matching.MapMatching").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

  // Change to Your Database Config
  var properties = new java.util.Properties()
  properties.put("user", "postgres")
  properties.put("password", "password")
  properties.put("driver", "org.postgresql.Driver")
  val conn_str = "jdbc:postgresql://localhost:5432/sbb-green"


  // Load the driver
  classOf[org.postgresql.Driver]


  // Setup the connection

  def getWaypoints(query: PreparedStatement, user_id: Long, leg: TripLeg): List[WaypointRecord] = {
    val date1 = java.sql.Timestamp.valueOf(leg.started_at)
    val date2 = java.sql.Timestamp.valueOf(leg.finished_at)

    query.setLong(1, user_id)
    query.setTimestamp(2, date1)
    query.setTimestamp(3, date2)

    val rs = query.executeQuery()
    val results: Iterator[WaypointRecord] = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      WaypointRecord(rs.getDouble("longitude"), rs.getDouble("latitude"), rs.getLong("tracked_at_millis"), rs.getLong("accuracy"))
    }
    results.toList
  }


  def main(args: Array[String]) {

    val conn = DriverManager.getConnection(conn_str, properties)
    val s =
      s"""
         |  select longitude, latitude,
         |       cast(extract(epoch from tracked_at::time) * 1000 as bigint) as tracked_at_millis,
         |       accuracy
         |  from public.waypoints
         |  where user_id = ? and tracked_at between ? and ?
         |  order by tracked_at
      """.stripMargin

    val statement = conn.prepareStatement(s)

    val logger = Logger.getLogger(this.getClass)

    val triplegs_src = Source.fromFile(args(0))
    // val waypoints_src = Source.fromFile(args(1))
    val OUTPUT_DIR = args(1)

    new File(OUTPUT_DIR).mkdirs
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    case class TripRow(user_id: Int, leg_id: Long, trip_id: Int, started_at: LocalDateTime, finished_at: LocalDateTime, mode: String)
    def parseDate(d: String): LocalDateTime = LocalDateTime.parse(d.replace(" ", "T"))
    //config.controler.setOutputDirectory(RUN_FOLDER + "aggregate/")

    //val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SS]")
    //read in user / date / mode / trip leg ids
    val personday_triplegs = triplegs_src.getLines().drop(1).map(_.split(",").map(_.replace("\"", "")))
      .map { case Array(user_id, leg_id, trip_id, started_at, finished_at, mode) =>
        val tripLegs = null

        val tr = TripRow(user_id.toInt, leg_id.toInt, 0, parseDate(started_at), parseDate(finished_at), mode)
        tr
      }
      .toStream
      .groupBy(tr => (tr.user_id, tr.started_at.toLocalDate))
      .map { case ((user_id, date), trs: Stream[TripRow]) =>
        TripRecord(user_id, 0, date, trs.map(tr => TripLeg(tr.leg_id, tr.started_at, tr.finished_at, tr.mode, List.empty)).toList)
      }.toList

    logger.info(s"${personday_triplegs.size} trips loaded")
    logger.info(s"${personday_triplegs.map(_.legs.size).sum} triplegs loaded")

    /////////// steps:
    // split waypoints into user_id and date
    //they will be ordered by user_id, split into files.
    //for each user id, load those waypoints
    // assign them to trip legs based on start and end trip_leg time.

    try {
      personday_triplegs.foreach { tr =>
        val date1 = tr.date.format(dateFormatter)
        val date_dir = Paths.get(s"$OUTPUT_DIR/$date1/")
        val outFile = Paths.get(date_dir.toString, s"${tr.user_id}.json")

        if (Files.notExists(outFile)) {
          logger.info("Creating:\t " + outFile.toAbsolutePath)

          val updatedLegs = tr.legs.map { leg =>
            leg.copy(waypoints = getWaypoints(statement, tr.user_id, leg))
          }
          val new_tr = List(tr.copy(legs = updatedLegs))
          val tripsJSON = Serialization.writePretty(new_tr)

          Files.createDirectories(date_dir)

          val pw = new PrintWriter(outFile.toFile)
          pw.write(tripsJSON)
          pw.close()
        } else {
          logger.info("Skipping:\t " + outFile.toAbsolutePath)

        }

      }
    } finally {
      conn.close()
    }


    //    logger.info(s"${tripleg_waypoints.map(_._2.size).sum} waypoints loaded")

    import scala.concurrent._

    /*
      personday_triplegs.par.foreach {
        case (tr, tl_ids) =>
          println(s"processing ${tr.user_id}, ${tr.date}, ${tr.date.format(dateFormatter)}")
          val gpxEntrys: Stream[(Int, Seq[WaypointRecord])] =
            tl_ids.map { case ( tr, tl_ids) => tr.leg_id -> tripleg_waypoints.getOrElse(tlr._2.leg_id, Seq.empty)}

          val date_dir = s"$OUTPUT_DIR/json/${tr.date.format(dateFormatter)}/"
          new File(date_dir).mkdirs
          val outFile = new File(s"$date_dir/${tr.user_id}-gpx.csv")

          gpxEntrys.sortBy(_._1).foreach { case (tl_id, gs) =>
            gs.sortBy(_.time).foreach(g => eventWriter.write(g.toString()))
          }
          eventWriter.close()

      }
*/


    //group by user / date / mode

    //get waypoints for trip_legs --indexing?

    //one loop per day to process a person-day
    //process waypoints into events


  }


}

