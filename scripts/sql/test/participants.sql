PGDMP                         w         	   sbb-green    9.6.5    11.1     �           0    0    ENCODING    ENCODING        SET client_encoding = 'UTF8';
                       false            �           0    0 
   STDSTRINGS 
   STDSTRINGS     (   SET standard_conforming_strings = 'on';
                       false            �           0    0 
   SEARCHPATH 
   SEARCHPATH     8   SELECT pg_catalog.set_config('search_path', '', false);
                       false            �           1262    191272 	   sbb-green    DATABASE     �   CREATE DATABASE "sbb-green" WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'German_Switzerland.1252' LC_CTYPE = 'German_Switzerland.1252';
    DROP DATABASE "sbb-green";
             postgres    false            �            1259    290115    participants    TABLE     �   CREATE TABLE public.participants (
    person_id text NOT NULL,
    last_name text,
    first_name text,
    email_address text,
    male boolean
);
     DROP TABLE public.participants;
       public         postgres    false            �          0    290115    participants 
   TABLE DATA               ]   COPY public.participants (person_id, last_name, first_name, email_address, male) FROM stdin;
    public       postgres    false    215   �       o           2606    290122    participants participants_pkey 
   CONSTRAINT     c   ALTER TABLE ONLY public.participants
    ADD CONSTRAINT participants_pkey PRIMARY KEY (person_id);
 H   ALTER TABLE ONLY public.participants DROP CONSTRAINT participants_pkey;
       public         postgres    false    215            �   O   x�3472���,���������/N-������ɯt�,+�KJ,M�K-ɨ�K��,�2435�t�O��J�K%�<�+F��� �, �     