select 
	longitude, latitude,
   	extract(epoch from tracked_at::time) * 1000 as tracked_at_millis,
   	accuracy
from motion_tag_waypoint
where user_id = ? and trip_id = ?
order by tracked_at