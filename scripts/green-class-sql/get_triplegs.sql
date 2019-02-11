SELECT user_id, id, trip_id, started_at, finished_at, 
	ST_AsText(ST_StartPoint(geometry)) as start_point, ST_AsText(ST_EndPoint(geometry)) as end_point, 
	trim(leading 'Mode::' from mode_validated) as mode_validated
FROM version_20181213_switzerland.triplegs 
WHERE id not in (SELECT id FROM version_20181213_switzerland.triplegs_anomalies)
--and mode_validated ilike '%car';
order by user_id, started_at