import java.time.{LocalDate, LocalDateTime}

import greenclass.WaypointRecord.WaypointRecordSerializer
import org.json4s.DefaultFormats

package object greenclass {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JavaTimeSerializers.all + new WaypointRecordSerializer

  case class TripLeg(leg_id: Long, started_at : LocalDateTime, finished_at : LocalDateTime, mode: String, waypoints : List[WaypointRecord])
  case class TripRecord(user_id: Int, trip_id: Int, date: LocalDate, legs: List[TripLeg])


}
