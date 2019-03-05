select leg_mode, avg(health) as health, avg (distance) as distance, 
avg(environment) as environment, avg(co2) as co2, avg(congestion) as congestion, avg(total) as total
from (
	select EXTRACT(WEEK FROM leg_date) as week, leg_mode, sum(health) as health, sum (distance) as distance, 
	sum(environment) as environment, sum(co2) as co2, sum(congestion) as congestion, sum(total) as total
	from wide_externalities
	where person_id = '1649'
	group by week, leg_mode
) as ll
group by leg_mode