 SELECT
        user_id as person_id,
        mt_trip_id as leg_id,
        started_at as leg_date,
        finished_at as end_date,
        updated_at,
        mode  as mode_validated,
        ST_X(ST_StartPoint(geometry)) as start_x,
        ST_Y(ST_StartPoint(geometry)) as start_y,
        ST_X(ST_EndPoint(geometry)) as finish_x,
        ST_Y(ST_EndPoint(geometry)) as finish_y,
        length as distance

 FROM (
         select
                ST_SETSRID(ST_MakeLine(array_agg((geom_part).geom order by (geom_part).path)), 4326) as geometry,
                user_id, mt_trip_id, started_at, finished_at, mode, length, updated_at
        from (
                 SELECT
                        user_id, mt_trip_id, started_at, finished_at, mode, length, coalesce(updated_at, created_at) as updated_at,
                        ST_dump(geometry) as geom_part
                 FROM motion_tag_trips as l
                 where type='Track'
                 and mode <> 'Mode::Airplane'
                 and length / 1000  < 1000 --dont include trips longer than 1000km
                 and not misdetected_completely
                 and merged_into_id is null
                 -- and person_id in (select person_id from legs_per_person where days_since_first_leg > 27 and valid_dates >= 7)
                 --and person_id in (select distinct participant_id from vehicle_information)
                 --and user_id in (select distinct user_id from participant_overview where time_in_survey > 26)
                 --and not exists (select  (leg_id) from externalities_list e where e.leg_id = l.mt_trip_id)
                 and mt_trip_id	= '13274754'
                and not overseas
                and not excluded

        ) a
        group by user_id, mt_trip_id, started_at, finished_at, mode, length, updated_at
) l
order by person_id, leg_date, leg_id;
