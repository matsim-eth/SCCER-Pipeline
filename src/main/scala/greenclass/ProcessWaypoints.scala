package greenclass

import java.sql.{Connection, Date, DriverManager, ResultSet}
import javax.jws.WebParam.Mode

import com.graphhopper.util.GPXEntry
import ethz.ivt.graphhopperMM.{GHtoEvents, GPXEntryExt, MATSimMMBuilder}
import org.apache.log4j.{Level, Logger}
import org.matsim.api.core.v01.events.Event
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84

import scala.collection.MapLike




object ProcessWaypoints {
  case class TripRecord(user_id: Int, date: Date, mode: String)

  import scala.collection.JavaConverters._
  Logger.getLogger("com.graphhopper.matching.MapMatching").setLevel(Level.WARN)
  Logger.getLogger("ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper").setLevel(Level.WARN)


  val matsim_network_file = "P:\\Projekte\\SCCER\\switzerland_10pct\\switzerland_network.xml.gz"
  val gh: GHtoEvents = new MATSimMMBuilder().buildGhToEvents(matsim_network_file, new CH1903LV03PlustoWGS84)

  // Change to Your Database Config
  var properties = new java.util.Properties()
  properties.put("user", "postgres")
  properties.put("password", "password")
  properties.put("driver", "org.postgresql.Driver")
  val conn_str = "jdbc:postgresql://localhost:5432/green_class"

  // Load the driver
  classOf[org.postgresql.Driver]

  // Setup the connection
  val conn = DriverManager.getConnection(conn_str, properties)

  def getWaypoints ( tripleg_ids : Stream[Int] ): Map[Int, Stream[GPXEntry]] = {
    val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

    val id_list = tripleg_ids.mkString(",")
    val tripleg_query = s"""select w.longitude, w.latitude, tracked_at, t.tl_id
                          from swiss_car_tripleg_waypoints as t
                          join waypoints as w
                          on t.wp_id = w.id
                          where t.tl_id in ($id_list)
                          order by tracked_at"""

    println(tripleg_query)

    val rs = statement.executeQuery(tripleg_query)

    val personday_triplegs = results(rs) //This is nice code that does the same as while(rs.next)...
      {rs => rs.getInt("tl_id") -> new GPXEntry(rs.getDouble(1), rs.getDouble(2), rs.getTimestamp(3).getTime)}
      .toStream.groupBy(_._1) //group waypoints by tripleg
      //TODO: check ordering
      .mapValues{ ss => ss.map(_._2)}
      //  .mapValues(_.size)
        //do something here to process the GPX traces
    personday_triplegs

  }

  //This is nice code that does the same as while(rs.next)...
  def results[T](resultSet: ResultSet)(f: ResultSet => T) = {
    new Iterator[T] {
      def hasNext = resultSet.next()
      def next() = f(resultSet)
    }
  }

  def getEvents(x: Stream[GPXEntryExt]): Stream[Event] = ???

  def main(args : Array[String]): Unit = {

    try {
      // Configure to be Read Only
      val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

      //read in user / date / mode / trip leg ids
      val tripleg_query = """select user_id, started_at::date, mode_validated, id as tripleg_id
                            from swiss_car_triplegs
                            where user_id = 1595"""

      val rs = statement.executeQuery(tripleg_query)

      val personday_triplegs = results(rs)(
        x => TripRecord(x.getInt("user_id"), x.getDate("started_at"), x.getString("mode_validated")) -> x.getInt("tripleg_id")
      ).toStream
        .groupBy(_._1)
        .mapValues(_.map(_._2)) //just take the trip id from the pair of (trip record, tripleg_id

      val waypoints_query = """select w.longitude, w.latitude, tracked_at, t.tl_id
                              from swiss_car_tripleg_waypoints as t
                              join waypoints as w
                              on t.wp_id = w.id
                              where w.user_id = 1595"""

      val rs_waypoints = statement.executeQuery(waypoints_query)

      //get convert waypoints into trip_leg -> list(waypoints) map
      val tripleg_waypoints = results(rs_waypoints)(
        {rs => rs.getInt("tl_id") -> new GPXEntry(rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getTimestamp("tracked_at").getTime)}
      ).toList.groupBy(_._1)
        .map{ case (k, ss) => (k, ss.map(_._2))}


      //calculate the number of waypoints and links for each person and date
      val person_events = personday_triplegs.foreach{
        case (tr, triplegs) =>
          val waypoints : Map[Int, List[GPXEntry]] = triplegs.flatMap(tl => tripleg_waypoints.get(tl).map(tl -> _)).toMap
          val links = waypoints.mapValues( v => gh.mapMatchWithTravelTimes(v.asJava))
          val numWaypoints : Int = waypoints.foldLeft(0)((a,b) => b._2.size)
          val numLinks : Int = links.foldLeft(0)((a,b) => b._2.size)
          println("%d - %s - %s | tl: %d | w: %d | l: %d"
            .format(tr.user_id, tr.date.toString, tr.mode, triplegs.size, numWaypoints, numLinks))
      }

      //TODO: output data from postgres as binary parquet files for running on euler

      //TODO: integrate emissions analysis


    }
    finally {
      conn.close
    }

    //group by user / date / mode

    //get waypoints for trip_legs --indexing?

    //one loop per day to process a person-day
      //process waypoints into events



  }
}
