package greenclass

import com.graphhopper.util.GPXEntry

case class WaypointRecord(wp_id: Long, lon: Double, lat: Double, time: Long, accuracy : Long, tl_id: Long) {

  override def toString() = "%d,%f,%f,%d,%d,%d\n".format(wp_id, lon, lat, time, accuracy, tl_id)

  def toGPX = new GPXEntry(lat, lon, time)

}

object WaypointRecord {
    def parseFromCSV(ss : String, sep : Char) : WaypointRecord = ss.split(sep) match {
      case Array(wp_id1, long1, lat1, tracked_at1, accuracy1, tl_id1) =>
        WaypointRecord(wp_id1.toLong, lat1.toDouble, long1.toDouble, tracked_at1.toLong, accuracy1.toLong, tl_id1.toLong)
  }

}
