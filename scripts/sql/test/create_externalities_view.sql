create or replace view wide_externalities as
select p.person_id, p.cluster_id, legs.leg_date, legs.leg_id, legs.leg_mode, distance, health, environment, co2, congestion, health+environment+co2+congestion as total
from (
	SELECT leg_id,
	COALESCE(
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'PM_health_costs' THEN val END), 'NaN'), 0) +
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'Noise_costs' THEN val END), 'NaN'), 0)+
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'NOx_costs' THEN val END), 'NaN'), 0) +
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'Active_costs' THEN val END), 'NaN'), 0)
		, 0)
	  AS health,

	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'PM_building_damage_costs' THEN val END), 'NaN'), 0) +
	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'Zinc_costs' THEN val END), 'NaN'), 0)
	  AS environment,

	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'CO2_costs' THEN val END), 'NaN'), 0)
	  AS co2,

	  COALESCE(NULLIF(MIN(CASE WHEN variable = 'delay_caused' THEN val END), 'NaN'), 0) * 26.1 / 3600
	  as congestion
	FROM externalities
	GROUP BY leg_id
) as aggs
join legs on legs.leg_id = aggs.leg_id
join participants as p on legs.person_id = p.person_id

