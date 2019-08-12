  select longitude, latitude,
       extract(epoch from to_timestamp(timestamp/1000)::time) as tracked_at_millis,
       accuracy
  from validation_outputtracking
  where id_user = ? and leg_id = ?
  order by timestamp