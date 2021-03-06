PGDMP     /                    w         	   sbb-green    9.6.5    11.1                0    0    ENCODING    ENCODING        SET client_encoding = 'UTF8';
                       false                       0    0 
   STDSTRINGS 
   STDSTRINGS     (   SET standard_conforming_strings = 'on';
                       false                       0    0 
   SEARCHPATH 
   SEARCHPATH     8   SELECT pg_catalog.set_config('search_path', '', false);
                       false                       1262    191272 	   sbb-green    DATABASE     �   CREATE DATABASE "sbb-green" WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'German_Switzerland.1252' LC_CTYPE = 'German_Switzerland.1252';
    DROP DATABASE "sbb-green";
             postgres    false                        2615    2200    public    SCHEMA        CREATE SCHEMA public;
    DROP SCHEMA public;
             postgres    false                       0    0    SCHEMA public    COMMENT     6   COMMENT ON SCHEMA public IS 'standard public schema';
                  postgres    false    4            �            1259    290132    externalities    TABLE     ^   CREATE TABLE public.externalities (
    leg_id integer,
    variable text,
    val numeric
);
 !   DROP TABLE public.externalities;
       public         postgres    false    4            �            1259    290125    legs    TABLE     �   CREATE TABLE public.legs (
    leg_id integer NOT NULL,
    person_id text,
    leg_date timestamp without time zone,
    leg_mode text,
    distance numeric,
    added_on timestamp without time zone,
    geom public.geometry(LineString,4326)
);
    DROP TABLE public.legs;
       public         postgres    false    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4            �            1259    290123    legs_leg_id_seq    SEQUENCE     x   CREATE SEQUENCE public.legs_leg_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 &   DROP SEQUENCE public.legs_leg_id_seq;
       public       postgres    false    217    4                       0    0    legs_leg_id_seq    SEQUENCE OWNED BY     C   ALTER SEQUENCE public.legs_leg_id_seq OWNED BY public.legs.leg_id;
            public       postgres    false    216            �            1259    290115    participants    TABLE     �   CREATE TABLE public.participants (
    person_id text NOT NULL,
    last_name text,
    first_name text,
    email_address text,
    male boolean
);
     DROP TABLE public.participants;
       public         postgres    false    4            �            1259    284937 	   waypoints    TABLE     -  CREATE TABLE public.waypoints (
    longitude double precision,
    id bigint NOT NULL,
    accuracy double precision,
    new_track boolean,
    tracked_at timestamp without time zone,
    latitude double precision,
    created_at timestamp without time zone,
    user_id bigint,
    tl_id bigint
);
    DROP TABLE public.waypoints;
       public         postgres    false    4            �            1259    284947    waypoints_geom    VIEW     @  CREATE VIEW public.waypoints_geom AS
 SELECT waypoints.id,
    waypoints.user_id,
    waypoints.tracked_at,
    public.st_point(waypoints.longitude, waypoints.latitude) AS geom,
    waypoints.accuracy
   FROM public.waypoints
  WHERE ((waypoints.user_id = 1596) AND ((waypoints.tracked_at)::date = '2016-11-23'::date));
 !   DROP VIEW public.waypoints_geom;
       public       postgres    false    213    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    213    213    213    213    213    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4    4            �           2604    290128    legs leg_id    DEFAULT     j   ALTER TABLE ONLY public.legs ALTER COLUMN leg_id SET DEFAULT nextval('public.legs_leg_id_seq'::regclass);
 :   ALTER TABLE public.legs ALTER COLUMN leg_id DROP DEFAULT;
       public       postgres    false    217    216    217            �           2606    284941    waypoints none 
   CONSTRAINT     N   ALTER TABLE ONLY public.waypoints
    ADD CONSTRAINT "none" PRIMARY KEY (id);
 :   ALTER TABLE ONLY public.waypoints DROP CONSTRAINT "none";
       public         postgres    false    213            �           2606    290122    participants participants_pkey 
   CONSTRAINT     c   ALTER TABLE ONLY public.participants
    ADD CONSTRAINT participants_pkey PRIMARY KEY (person_id);
 H   ALTER TABLE ONLY public.participants DROP CONSTRAINT participants_pkey;
       public         postgres    false    215            �           1259    284942    idx_tracked_at    INDEX     S   CREATE INDEX idx_tracked_at ON public.waypoints USING btree (user_id, tracked_at);
 "   DROP INDEX public.idx_tracked_at;
       public         postgres    false    213    213            �           1259    284943    idx_user_id    INDEX     D   CREATE INDEX idx_user_id ON public.waypoints USING btree (user_id);
    DROP INDEX public.idx_user_id;
       public         postgres    false    213            �           1259    284946    waypoints_tracked_at_idx    INDEX     T   CREATE INDEX waypoints_tracked_at_idx ON public.waypoints USING btree (tracked_at);
 ,   DROP INDEX public.waypoints_tracked_at_idx;
       public         postgres    false    213            �           1259    284945    waypoints_user_id_idx    INDEX     N   CREATE INDEX waypoints_user_id_idx ON public.waypoints USING btree (user_id);
 )   DROP INDEX public.waypoints_user_id_idx;
       public         postgres    false    213            �           1259    284944    wp_index    INDEX     <   CREATE INDEX wp_index ON public.waypoints USING btree (id);
    DROP INDEX public.wp_index;
       public         postgres    false    213           