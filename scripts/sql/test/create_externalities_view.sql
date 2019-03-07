create or replace view wide_externalities as
select p.person_id, p.cluster_id, legs.leg_date, legs.leg_id, legs.leg_mode, distance, health, co2, congestion, health+co2+congestion as total
from (
	SELECT leg_id,
	COALESCE(
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'PM_health_costs' THEN val END), 'NaN'), 0) +
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'Noise_costs' THEN val END), 'NaN'), 0)+
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'NOx_costs' THEN val END), 'NaN'), 0) +
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'Active_costs' THEN val END), 'NaN'), 0)
		, 0)
	  AS health,
	
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'CO2_costs' THEN val END), 'NaN'), 0)
	  AS co2,

	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'delay_caused' THEN val END), 'NaN'), 0) * 26.1 / 3600
	  as congestion
	FROM externalities
	GROUP BY leg_id
) as aggs
join legs on legs.leg_id = aggs.leg_id
join participants as p on legs.person_id = p.person_id;


create or replace view externality_norms as
select 
	unnest( ARRAY['min','1','2','3','4','max']) as quintile,
	unnest( percentile_cont(ARRAY[0,0.2, 0.4, 0.6, 0.8,1]) within group ( order by health )) as health,
	unnest( percentile_cont(ARRAY[0,0.2, 0.4, 0.6, 0.8,1]) within group ( order by co2 )) as co2,
	unnest( percentile_cont(ARRAY[0,0.2, 0.4, 0.6, 0.8,1]) within group ( order by congestion )) as congestion,
	unnest( percentile_cont(ARRAY[0,0.2, 0.4, 0.6, 0.8,1]) within group ( order by total )) as total

																		 from (
	 select EXTRACT(WEEK FROM leg_date) as week, sum(health) as health, 
	 sum(co2) as co2, sum(congestion) as congestion, sum(total) as total
	from wide_externalities
	group by week, person_id
) as w_exts
																		 																	 
union
select	'mean' as quintile, avg(health) as health,
    	avg(co2) as co2, avg(congestion) as congestion, avg(total) as total
from (
	 select EXTRACT(WEEK FROM leg_date) as week, sum(health) as health, 
	 sum(co2) as co2, sum(congestion) as congestion, sum(total) as total
	from wide_externalities
	group by week, person_id
) as w_exts;




update legs
set leg_mode = 'PT'
where leg_mode = 'Bus' or leg_mode = 'Tram'