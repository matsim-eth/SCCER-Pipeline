PGDMP                         w         	   sbb-green    9.6.5    11.1 
    �           0    0    ENCODING    ENCODING        SET client_encoding = 'UTF8';
                       false            �           0    0 
   STDSTRINGS 
   STDSTRINGS     (   SET standard_conforming_strings = 'on';
                       false            �           0    0 
   SEARCHPATH 
   SEARCHPATH     8   SELECT pg_catalog.set_config('search_path', '', false);
                       false            �           1262    191272 	   sbb-green    DATABASE     �   CREATE DATABASE "sbb-green" WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'German_Switzerland.1252' LC_CTYPE = 'German_Switzerland.1252';
    DROP DATABASE "sbb-green";
             postgres    false            �            1259    290125    legs    TABLE     �   CREATE TABLE public.legs (
    leg_id integer NOT NULL,
    person_id text,
    leg_date timestamp without time zone,
    leg_mode text,
    distance numeric,
    added_on timestamp without time zone,
    geom public.geometry(LineString,4326)
);
    DROP TABLE public.legs;
       public         postgres    false            �            1259    290123    legs_leg_id_seq    SEQUENCE     x   CREATE SEQUENCE public.legs_leg_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 &   DROP SEQUENCE public.legs_leg_id_seq;
       public       postgres    false    217            �           0    0    legs_leg_id_seq    SEQUENCE OWNED BY     C   ALTER SEQUENCE public.legs_leg_id_seq OWNED BY public.legs.leg_id;
            public       postgres    false    216            n           2604    290128    legs leg_id    DEFAULT     j   ALTER TABLE ONLY public.legs ALTER COLUMN leg_id SET DEFAULT nextval('public.legs_leg_id_seq'::regclass);
 :   ALTER TABLE public.legs ALTER COLUMN leg_id DROP DEFAULT;
       public       postgres    false    217    216    217            �          0    290125    legs 
   TABLE DATA               _   COPY public.legs (leg_id, person_id, leg_date, leg_mode, distance, added_on, geom) FROM stdin;
    public       postgres    false    217   �	       �           0    0    legs_leg_id_seq    SEQUENCE SET     ?   SELECT pg_catalog.setval('public.legs_leg_id_seq', 216, true);
            public       postgres    false    216            �   �  x��Z�n1��"?��%�	Ҧ
�ƍ�*ȣp���|���� �`|��,9���J�D<(=�|`~$~,c�������]�����B?������I�yh������f�����ǟ7p���-����`F�]�,�ծ(�����U���<����r�|z~y��
��Q���6od�p�{�Dk��+(��i�I�����{n/[<�\��5�1h
\����9��;C��ip`;�;2��N��w���%v%4&�K�M���;�4�����q�1iq�gLZ�y��W����m��5���V??��h}-�����#�`�0WF���(xd$��@A�w$�Ԉ�5�P�	[ ���h���ѰA6l�[l�=5"b�}��Tܿ��q���#�,��� �A��E�� Pj�b� �A��С�Б�Б���#��#����������|p@pΫ��$#xs��g �o���}
d���ũ���7���bfB�S<�����cB 4��Ȅ(Ʉ�@&DK&d�a��QU� )3�&C�dF\�m�̐�;S�z��9����al�G.[kf�rbݍ5oŌ�6�B]}����L�
]���r��6�}��Ԍ8cn���
��B�9���~Ec��T�K���74V��9��rF�T�P�]���@w�:���P�]��� =wA��4����������Y��l���Qgc&�����n@�G���֝����Pu�'��=7��@�c�C�G��AC�kT�4��zΝChwHM��)�5�]�S��U��U�������zXy�ly��y�:�� Nkp�Y̠��6'r�u x��Z��j!��C�2�Lz=5K9��$bW�$�ZPPA1�e��[F�D��D��e,��1�$r���-��B��-���~.�	��'亟��~�.�	��'��?E��4t韠�mR���A������@���?m���?L��     