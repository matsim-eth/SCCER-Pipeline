package greenclass

import com.graphhopper.util.GPXEntry
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JArray, JDouble, JLong}


case class WaypointRecord(lon: Double, lat: Double, time: Long, accuracy : Long) {

  override def toString() = "%f,%f,%d,%d,%d\n".format(lon, lat, time, accuracy)

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
