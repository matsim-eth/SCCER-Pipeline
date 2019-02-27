update legs
set distance = val
from externalities
where variable = 'Distance' and legs.leg_id = externalities.leg_id and legs.leg_mode = 'Car';


update legs
set distance = floor(random()* (2000-400 + 1) + 400)
where leg_mode = 'Walk';

update legs
set distance = floor(random()* (10000-1000 + 1) + 1000)
where leg_mode = 'Bicycle';


update legs
set distance = floor(random()* (100000-10000 + 1) + 10000)
where leg_mode = 'Train';


update legs
set distance = floor(random()* (15000-5000 + 1) + 5000)
where leg_mode = 'Tram' or leg_mode = 'Bus' or leg_mode = 'pt' or leg_mode = 'PT';

select * from legs
