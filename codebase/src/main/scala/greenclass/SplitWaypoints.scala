package greenclass

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import java.sql.{DriverManager, PreparedStatement}
import java.time.{LocalDateTime}
import java.time.format.DateTimeFormatter

import ethz.ivt.externalities.data.{LatLon, TripLeg, TripRecord}
import ethz.ivt.externalities.data.WaypointRecord
import org.apache.log4j.{Level, Logger}

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

  def getWaypoints(query: PreparedStatement, user_id: String, leg: TripLeg): List[WaypointRecord] = {
    val date1 = java.sql.Timestamp.valueOf(leg.started_at)
    val date2 = java.sql.Timestamp.valueOf(leg.finished_at)

    query.setInt(1, user_id.toInt)
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

    val triplegs_sql =
      """
        |SELECT user_id, id, trip_id, started_at, finished_at,
        |	ST_X(ST_StartPoint(geometry)) as start_x,
        |	ST_Y(ST_StartPoint(geometry)) as start_y,
        |	ST_X(ST_EndPoint(geometry)) as finish_x,
        |	ST_Y(ST_EndPoint(geometry)) as finish_y,
        |	trim(leading 'Mode::' from mode_validated) as mode_validated
        |FROM version_20181213_switzerland.triplegs
        |WHERE id not in (SELECT id FROM version_20181213_switzerland.triplegs_anomalies)
        |order by user_id, started_at
        |
      """.stripMargin


    val waypoints_sql =
      s"""
         |  select longitude, latitude,
         |       cast(extract(epoch from tracked_at::time) * 1000 as bigint) as tracked_at_millis,
         |       accuracy
         |  from public.waypoints
         |  where user_id = ? and tracked_at between ? and ?
         |  order by tracked_at
      """.stripMargin

    val statement = conn.prepareStatement(waypoints_sql)

    val logger = Logger.getLogger(this.getClass)

    val OUTPUT_DIR = args(0)

    new File(OUTPUT_DIR).mkdirs
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    case class TripRow(
                        user_id: String,
                        leg_id: Long,
                        tripLeg: TripLeg) {

    }

    def parseDate(d: String): LocalDateTime = LocalDateTime.parse(d.replace(" ", "T"))
    //config.controler.setOutputDirectory(RUN_FOLDER + "aggregate/")

    logger.info("Loading trip legs from database")

    //val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SS]")
    //read in user / date / mode / trip leg ids
    val triplegs_rs = conn.createStatement().executeQuery(triplegs_sql)
    val triplegs = Iterator.continually(triplegs_rs).takeWhile(_.next()).map { rs =>
      TripRow(rs.getLong("user_id").toString, rs.getLong("trip_id"), TripLeg(rs.getLong("id"),
        rs.getTimestamp("started_at").toLocalDateTime, rs.getTimestamp("finished_at").toLocalDateTime,
        LatLon(rs.getDouble("start_y"), rs.getDouble("start_x")),
        LatLon(rs.getDouble("finish_y"), rs.getDouble("finish_x")), rs.getString("mode_validated"), Nil)
      )
    }

    val personday_triplegs = triplegs.toStream
      .groupBy(tr => (tr.user_id, tr.tripLeg.started_at.toLocalDate))
      .map { case ((user_id, date), trs: Stream[TripRow]) =>
        TripRecord(user_id, 0, date, trs.map(_.tripLeg).toList)
      }
      .map( tr => tr.copy(legs = tr.legs.filter(tl => tl.mode == "Car" || tl.mode == "Ecar" )) )
      .filterNot(_.legs.isEmpty)
      .toList

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

