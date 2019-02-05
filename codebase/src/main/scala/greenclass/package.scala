import ethz.ivt.externalities.data.WaypointRecord.WaypointRecordSerializer

package object greenclass {
  implicit val formats = org.json4s.DefaultFormats ++
    org.json4s.ext.JavaTimeSerializers.all + new WaypointRecordSerializer
}
