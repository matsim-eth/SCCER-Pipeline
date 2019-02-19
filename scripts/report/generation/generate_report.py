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


template_lookup = TemplateLookup(directories=['generation/templates'],strict_undefined=True )

mytemplate = template_lookup.get_template("control.html")


weekly_stats = SimpleNamespace()

weekly_stats.start_date = date(2019, 2, 4) #
weekly_stats.start_date_string = format_date(weekly_stats.start_date, "long", locale=locale) #
weekly_stats.hours_str = format_unit(2, 'hour', locale=locale)

weekly_stats.distance = weekly_totals['distance'].sum()
weekly_stats.distance_str = format_unit(weekly_stats.distance, 'kilometer', locale=locale)

weekly_stats.car = 20
weekly_stats.bus = 30
weekly_stats.walk = 45


def get_mode_image_src(mode):
    return 'images/mode_icons/{}-solid.gif'.format(mode.lower())

#test data

#format_unit(, "minute", "short", locale=locale)

modes = [SimpleNamespace(
    mode = mode,
    mode_image_src = get_mode_image_src(mode),
    distance = random.uniform(1000, 10000),
    duration=random.randrange(1,60),
    odd="odd" if i % 2 else "even",
    externalities = SimpleNamespace(
                            co2 = "30", pm = "20", health = "5", noise = "8" ),
    pop_average = random.uniform(3000, 6000),
    my_average = random.uniform(3000, 6000),
    max_val = 10000

) for i,mode in enumerate(["Car", "Train", "Bus", "bicycle", "Walk"])]

total_dist = sum([m.distance for m in modes])
total_duration = sum([m.duration for m in modes])
for m in modes:
    m.distance_pc = int(m.distance / total_dist * 100)
    m.duration_pc = int(m.duration / total_duration * 100)
    m.distance_str = format_unit(round(m.distance / 1000, 2), "kilometer", "short", locale=locale)
    m.duration_str = format_unit(round(m.duration, 2), "minute", "short", locale=locale)


    values = sorted([("0", 0), ("distance", m.distance),
                     ("pop_average", m.pop_average), ("my_average", m.my_average),
                     ("max_val", m.max_val)], key=lambda x: x[1])
    values = [(k + " clear" if v > m.distance else k, v) for k,v in values]

    classes, values1 = zip(*values)
    bar_widths = zip(classes[1:], np.diff(values1))


    m.bar_widths = [(k, max(w/m.max_val * 60, 1)) for k,w in bar_widths]


html = mytemplate.render(title=_('report_title'),
                                            weekly_totals = weekly_totals,
                                            person = person_details,
                                            modes = modes,
                                            weekly_stats = weekly_stats, output_encoding='utf-8')
#, disable_basic_attributes=["width", "height", "valign", "align"]
inlined_css = premailer.Premailer(html, base_url="https://www.ivtmobis.ethz.ch/", strip_important=False).transform()

with open("generation/test_report.html", "w", encoding="utf8") as file:
    file.write(inlined_css)

