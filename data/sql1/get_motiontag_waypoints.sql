select 
	longitude, latitude,
   	extract(epoch from tracked_at::time) * 1000 as tracked_at_millis,
   	accuracy
from motion_tag_waypoint w
join motion_tag_trips t on w.trip_id = t.mt_trip_id
where w.user_id = ? and trip_id = ? and accuracy < 200
and ST_Distance(w.geom, t.geometry) < 0.0011
order by tracked_at