import java.time.{LocalDate, LocalDateTime}

import com.graphhopper.util.GPXEntry
import greenclass.WaypointRecord.WaypointRecordSerializer
import org.json4s.JsonAST.{JArray, JDouble, JLong, JObject}
import org.json4s.{CustomSerializer, DefaultFormats}
import org.matsim.api.core.v01.Coord

package object greenclass {
  implicit val formats = org.json4s.DefaultFormats ++
    org.json4s.ext.JavaTimeSerializers.all +
    new WaypointRecordSerializer

  case class LatLon(lat : Double, lon : Double) {
    def toGPX: GPXEntry = new GPXEntry(lat, lon, 0, 0)

  }

  case class TripLeg(
                      leg_id: Long,
                      started_at : LocalDateTime,
                      finished_at : LocalDateTime,
                      start_point : LatLon,
                      finish_point : LatLon,
                      mode: String,
                      waypoints : List[WaypointRecord]
                    ) {
    def getStartedSeconds: Double = started_at.toLocalTime.toSecondOfDay
    def getFinishedSeconds: Double = finished_at.toLocalTime.toSecondOfDay

  }

  case class TripRecord(user_id: String, trip_id: Int, date: LocalDate, legs: List[TripLeg])


}

