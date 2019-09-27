 SELECT 
	user_id as person_id, 
	mt_trip_id as leg_id, 
	started_at as leg_date, 
	finished_at as end_date, 
	mode  as mode_validated,
	ST_X(ST_StartPoint(geometry)) as start_x,
	ST_Y(ST_StartPoint(geometry)) as start_y,
	ST_X(ST_EndPoint(geometry)) as finish_x,
	ST_Y(ST_EndPoint(geometry)) as finish_y,
	length as distance

 FROM motion_tag_trips as l
 where type='Track'
 and length / 1000  < 1000 --dont include trips longer than 1000km
 and not misdetected_completely
 -- and person_id in (select person_id from legs_per_person where days_since_first_leg > 27 and valid_dates >= 7)
 --and person_id in (select distinct participant_id from vehicle_information)
 -- and mt_trip_id not in (select distinct (leg_id) from externalities_list)
 and not overseas
and user_id = 'BQCZK'

order by person_id, leg_date, leg_id;
