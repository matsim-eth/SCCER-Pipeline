<<<<<<< HEAD
select 
	longitude, latitude,
   	extract(epoch from tracked_at::time) * 1000 as tracked_at_millis,
   	accuracy
=======
select
        longitude, latitude,
        extract(epoch from tracked_at) * 1000 as tracked_at_millis,
        accuracy
>>>>>>> small bug fixes in node link timing matching
from motion_tag_waypoint w
join motion_tag_trips t on w.trip_id = t.mt_trip_id
where w.user_id = ? and trip_id = ? and accuracy < 200
and (ST_Distance(w.geom, t.geometry) < 0.0008 or ST_DistanceSphere(w.geom, t.geometry) < 100)
<<<<<<< HEAD
order by tracked_at
=======
order by tracked_at
>>>>>>> small bug fixes in node link timing matching
