package ethz.ivt.externalities

import java.time.{LocalDate, LocalDateTime}

import com.graphhopper.util.GPXEntry
import ethz.ivt.externalities.data.WaypointRecord.WaypointRecordSerializer
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JArray, JDouble, JLong}

package object data {

  implicit val formats = org.json4s.DefaultFormats

  case class LatLon(lat : Double, lon : Double) {
    def toGPX: GPXEntry = new GPXEntry(lat, lon, 0, 0)

  }

  case class TripLeg(
                      leg_id: String,
                      started_at : LocalDateTime,
                      finished_at : LocalDateTime,
                      start_point : LatLon,
                      finish_point : LatLon,
                      distance: Double,
                      mode: String,
                      waypoints : List[WaypointRecord]
                    ) {
    def getStartedSeconds: Double = started_at.toLocalTime.toSecondOfDay
    def getFinishedSeconds: Double = finished_at.toLocalTime.toSecondOfDay

    override def toString() = "%s,%s,%s".format(leg_id, started_at, finished_at)


  }

  case class TripRecord(user_id: String, trip_id: Int, date: LocalDate, legs: List[TripLeg]) {
    def getIdentifier = date.toString + " - " + user_id
    override def toString() = "%d,%s,%s".format(trip_id, date)

  }

  case class WaypointRecord(lon: Double, lat: Double, time: Long, accuracy : Long) {

    override def toString() = "%f,%f,%d,%d\n".format(lon, lat, time, accuracy)

    def toGPX = new GPXEntry(lat, lon, time)

  }

  object WaypointRecord {
    def parseFromCSV(ss : String, sep : Char) : WaypointRecord = ss.split(sep) match {
      case Array(long1, lat1, tracked_at1, accuracy1) =>
        WaypointRecord(lat1.toDouble, long1.toDouble, tracked_at1.toLong, accuracy1.toLong)
    }

    class WaypointRecordSerializer extends CustomSerializer[WaypointRecord](format => (
      {
        case JArray(long :: lat :: time :: acc :: Nil) =>
          new WaypointRecord(long.extract[Double], lat.extract[Double], time.extract[Long], acc.extract[Long])
      },
      {
        case w: WaypointRecord =>
          JArray(JDouble(w.lon) :: JDouble(w.lat) :: JLong(w.time) :: JLong(w.accuracy) :: Nil)
      }
    ))

  }


}

