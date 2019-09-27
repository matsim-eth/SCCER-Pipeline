 SELECT 
 	person_id, 
 	id as leg_id, 
 	leg_date, leg_date+ interval '1 second' * duration as end_date, 
 	leg_mode_user  as mode_validated,
	ST_X(ST_StartPoint(geom)) as start_x,
	ST_Y(ST_StartPoint(geom)) as start_y,
	ST_X(ST_EndPoint(geom)) as finish_x,
	ST_Y(ST_EndPoint(geom)) as finish_y,
 distance

 FROM validation_legs as l
 where leg_mode_user not in ('???', 'overseas', 'Split', 'Activity')
 -- and person_id in (select person_id from legs_per_person where days_since_first_leg > 27 and valid_dates >= 7)
 and person_id in (select distinct participant_id from vehicle_information)
 and id not in (select distinct (leg_id) from validation_externalities)
 --and person_id = '70057062'

order by person_id, leg_date, leg_id;
