package greenclass

import java.io.{File, FileInputStream, PrintWriter}
import java.nio.file.{Files, Paths}
import java.sql.{DriverManager, PreparedStatement}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import ethz.ivt.externalities.data.{LatLon, TripLeg, TripRecord}
import ethz.ivt.externalities.data.WaypointRecord
import org.apache.log4j.{Level, Logger}
import org.json4s.jackson.Serialization
import me.tongfei.progressbar.ProgressBar
import org.matsim.api.core.v01.TransportMode

import scala.io.Source
import scala.collection.JavaConverters._

object SplitWaypoints {


  Logger.getLogger("com.graphhopper.matching.MapMatchingUnlimited").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)

  // Setup the connection

  def getWaypoints(ds: HikariDataSource, waypoints_sql : String, user_id: String, leg: TripLeg): List[WaypointRecord] = {

    val conn = ds.getConnection()
    try {
      val query = conn.prepareStatement(waypoints_sql)

      query.setString(1, user_id)
      query.setString(2, leg.leg_id)

      val rs = query.executeQuery()
      val results: Iterator[WaypointRecord] = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
        WaypointRecord(rs.getDouble("longitude"), rs.getDouble("latitude"), rs.getLong("tracked_at_millis"), rs.getLong("accuracy"))
      }
      return results.toList
    } finally {
      conn.close()
    }
  }

  def parseDate(d: String): LocalDateTime = LocalDateTime.parse(d.replace(" ", "T"))
  //config.controler.setOutputDirectory(RUN_FOLDER + "aggregate/")

  def buildModeMap(props: Properties): Map[String, String] = {
    val m1 = props.stringPropertyNames().asScala.filter(_.startsWith("matsim.mode"))
    val modeMap = m1.flatMap { case matsim_mode_key =>
      val matsim_mode = matsim_mode_key.split('.').last
      props.getProperty(matsim_mode_key).split(';').map(mode => mode.trim -> matsim_mode)
    }.toMap

    modeMap.foreach(println)


    return modeMap
  }

  def main(args: Array[String]) {

    val props = new Properties()
    props.load(new FileInputStream(args(0)))
    val base_file_location = Paths.get(props.getProperty("base.data.location"))

    val trips_folder = base_file_location.resolve(Paths.get(props.getProperty("trips.folder")))
    val config = new HikariConfig(props.getProperty("database.properties.file"))
    config.setMinimumIdle(20)
    config.setMaximumPoolSize(20)

    val ds = new HikariDataSource(config)

    val trips_sql_file = Source.fromFile(base_file_location.resolve(Paths.get(props.getProperty("trips.sql.file"))).toFile)
    val triplegs_sql = trips_sql_file.getLines mkString "\n"
    trips_sql_file.close()

    val waypoints_sql_file = Source.fromFile(base_file_location.resolve(Paths.get(props.getProperty("waypoints.sql.file"))).toFile)
    val waypoints_sql = waypoints_sql_file.getLines mkString "\n"
    waypoints_sql_file.close()

    val logger = Logger.getLogger(this.getClass)

    val matsimModeMap = buildModeMap(props)

    System.out.println(trips_folder)

    val OUTPUT_DIR = trips_folder

    System.out.println(OUTPUT_DIR.toAbsolutePath)

    if (!Files.exists(OUTPUT_DIR)) {
      Files.createDirectory(OUTPUT_DIR)
    }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    case class TripRow(user_id: String, leg_id: Long, tripLeg: TripLeg)



    logger.info("Loading trip legs from database")

    //val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SS]")
    //read in user / date / mode / trip leg ids
    val conn1 = ds.getConnection()
    val triplegs_rs = conn1.createStatement().executeQuery(triplegs_sql)
    val triplegs = Iterator.continually(triplegs_rs).takeWhile(_.next()).map { rs =>
      TripRow(rs.getString("person_id"), 0,
        TripLeg(rs.getString("leg_id"),
          rs.getTimestamp("leg_date").toLocalDateTime, rs.getTimestamp("end_date").toLocalDateTime,
          LatLon(rs.getDouble("start_y"), rs.getDouble("start_x")),
          LatLon(rs.getDouble("finish_y"), rs.getDouble("finish_x")),
          rs.getDouble("distance"),
          matsimModeMap.getOrElse(rs.getString("mode_validated"), TransportMode.other),
          Nil
        )
      )
    }

    logger.info("creating person trip legs")

 //   val ids = "1647" :: "1681" :: "1595" :: "1596" :: "1607" :: Nil

    val personday_triplegs = triplegs.toStream
        .groupBy(tr => (tr.user_id, tr.tripLeg.started_at.toLocalDate))
        .par.map { case ((user_id, date), trs: Stream[TripRow]) =>
        TripRecord(user_id, 0, date, trs.map(_.tripLeg).toList)
      }
      .map( tr => tr.copy(legs = tr.legs   ))//.filter(tl => tl.mode == "Car" || tl.mode == "Ecar" )) )
      .filterNot(_.legs.isEmpty)
  //    .filter(tr => ids.contains(tr.user_id))
      .toList

    logger.info(s"${personday_triplegs.size} trip days loaded")
    logger.info(s"${personday_triplegs.map(_.legs.size).sum} triplegs loaded")
    conn1.close()

    val max_num_chunks : Int = props.getProperty("person.days.max.num.chunks", "20").toInt
    val min_person_days_per_group : Int = props.getProperty("person.days.min.chunk.size", "100").toInt

    val objects_per_group = personday_triplegs.size / max_num_chunks
    val group_size : Int = scala.math.max(objects_per_group, min_person_days_per_group)

    /////////// steps:
    // split waypoints into user_id and date
    //they will be ordered by user_id, split into files.
    //for each user id, load those waypoints
    // assign them to trip legs based on start and end trip_leg time.
    val pb = new ProgressBar("Test", personday_triplegs.size)
    try { // name, initial max
      personday_triplegs.grouped(group_size).zipWithIndex.foreach {
        case (trs, chunk_i) =>

          val chunk_folder_name = "chunk_" + chunk_i
          val sub_dir = OUTPUT_DIR.resolve(chunk_folder_name)

          trs.par.foreach { case tr =>

            val date1 = tr.date.format(dateFormatter)
            val outFile = sub_dir.resolve(s"${tr.user_id}_${date1}.json")


            //  logger.info("Creating:\t " + outFile.toAbsolutePath)

            val updatedLegs = tr.legs.map { leg =>
              leg.copy(waypoints = getWaypoints(ds, waypoints_sql, tr.user_id, leg))
            }
            val new_tr = List(tr.copy(legs = updatedLegs))
            val tripsJSON = Serialization.writePretty(new_tr)

            Files.createDirectories(sub_dir)
            val pw = new PrintWriter(outFile.toFile)
            try {
              pw.write(tripsJSON)
            } finally {
              pw.close()
            }

            pb.step(); // step by 1
            pb.setExtraMessage("Reading..."); // Set extra message to display at the end of the bar
          }


        }

    } finally {
      pb.close()
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

