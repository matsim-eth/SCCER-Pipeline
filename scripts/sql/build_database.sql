SET search_path TO public,version_20181213_switzerland ;


drop table if exists participants;
drop table if exists legs;
drop table if exists externalities;

create table participants (person_id text primary key, last_name text, first_name text, 
	email_address text, male bool, cluster_id integer);

insert into participants values ( '1723', 'Smith', 'John', 'joseph.molloy@ivt.baug.ethz.ch', true, 1);
insert into participants values( '1650', 'Doe', 'Jane', 'joseph.molloy@ivt.baug.ethz.ch', false, 1);

create table legs (leg_id serial, person_id text, leg_date timestamp, leg_mode text, distance numeric, added_on timestamp);
SELECT AddGeometryColumn('legs', 'geom', 4326, 'LINESTRING', 2);

create table externalities (leg_id integer, variable text, val numeric);
