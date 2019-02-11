import pprint
import random
from types import SimpleNamespace

import numpy as np
import pandas as pd
from datetime import datetime
from os import path
import glob
import psycopg2 as pg
import pandas.io.sql as psql

from babel.units import format_unit
from babel.dates import *

from mako.template import Template
from mako.lookup import TemplateLookup
from premailer import premailer

locale = 'de'

import gettext
lang_de = gettext.translation('generation', localedir="generation/locale", languages=[locale])
lang_de.install()

to_datetime = lambda d: datetime.strptime(d, '%Y-%m-%d')


def get_emissions(value):
    return random.choice(["High", "Medium", "Low"])

def get_date(fname):
    location = path.split(fname)[0]
    date = path.split(location)[1]
    return date

def hms_string(sec_elapsed):
    h = int(sec_elapsed / (60 * 60))
    h_string = format_unit(h, 'hour', locale=locale, length="short") if h > 0 else ""
    m = int((sec_elapsed % (60 * 60)) / 60)
    m_string = format_unit(m, 'minute', locale=locale, length="short") if (h > 0 or m > 0) or m == 0 else ""

    s = sec_elapsed % 60.

    return h_string + m_string


def distance_string(dist):
    km = int(dist / 1000)
    km_string = format_unit(km, 'kilometer', locale=locale, length="short") if km > 0 else ""
    m = int(dist)
    m_string = format_unit(m, 'meter', locale=locale, length="short") if m < 1000 else ""

    return km_string + m_string


connection = pg.connect(host='localhost',  dbname="sbb-green", user="postgres", password='password')

person_details = pd.read_sql_query("SELECT * FROM participants where person_id = '1723'", connection).to_dict('records')[0]


leg_details = pd.read_sql_query("SELECT * FROM legs where person_id = '1723'", connection)
leg_in_list = ','.join(map(str, leg_details['leg_id']))

externalities = pd.read_sql_query("SELECT * FROM externalities where leg_id in (%s)" % leg_in_list, connection)
grouped_df = externalities.groupby("leg_id")

#create a dictionary of the legs
legs = {l : dict(zip(details.variable, details.val))
        for (l, details) in grouped_df}

pprint.pprint(person_details)

pprint.pprint(legs)

weekly_totals = leg_details.groupby('leg_mode')['distance'].sum().reset_index()
pprint.pprint(weekly_totals)
distance_week = weekly_totals['distance'].sum()

template_lookup = TemplateLookup(directories=['generation/templates'])

mytemplate = template_lookup.get_template("control.html")


weekly_hours = format_unit(2, 'hour', locale=locale)
weekly_stats = SimpleNamespace()

weekly_stats.start_date = date(2019, 2, 4) #
weekly_stats.start_date_string = format_date(weekly_stats.start_date, "long", locale=locale) #
weekly_stats.hours = weekly_hours
weekly_stats.car = 20
weekly_stats.bus = 30
weekly_stats.walk = 45


def get_mode_image_tag(mode):
    return '<img class="mode" src="images/mode_icons/{}_icon.png"> </img>'.format(mode.lower())

#test data

#format_unit(, "minute", "short", locale=locale)

modes = [SimpleNamespace(
    mode = mode,
    mode_image_url = get_mode_image_tag(mode),
    distance = random.uniform(1000, 10000),
    duration=random.randrange(1,60)
        ) for mode in ["Car", "Train", "Walk"]]

total_dist = sum([m.distance for m in modes])
total_duration = sum([m.duration for m in modes])
for m in modes:
    m.distance_pc = int(m.distance / total_dist * 100)
    m.duration_pc = int(m.duration / total_duration * 100)
    m.distance_str = format_unit(round(m.distance / 1000, 2), "kilometer", "short", locale=locale)
    m.duration_str = format_unit(round(m.duration, 2), "minute", "short", locale=locale)

html = mytemplate.render(title=_('report_title'),
                                            weekly_totals = weekly_totals,
                                            person = person_details,
                                            modes = modes,
                                            weekly_stats = weekly_stats, output_encoding='utf-8')
#, disable_basic_attributes=["width", "height", "valign", "align"]
inlined_css = premailer.Premailer(html, base_url="https://www.ivtmobis.ethz.ch/", strip_important=False).transform()

with open("generation/test_report.html", "w", encoding="utf8") as file:
    file.write(inlined_css)

