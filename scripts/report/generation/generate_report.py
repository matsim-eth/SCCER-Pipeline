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
from babel.numbers import format_currency
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

weekly_stats.ext_totals = {}
weekly_stats.ext_totals['health'] = 0
weekly_stats.ext_totals['CO2'] = 0
weekly_stats.ext_totals['environment'] = 0
weekly_stats.ext_totals['congestion'] = 0


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
                            co2 = 30, environment = 20, health = 5 * (((i % 2)*-2)+1), congestion = 8 ),
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



min_health = abs(min([m.externalities.health for m in modes]))


max_total = max([abs(m.externalities.health) +
                       m.externalities.co2 +
                       m.externalities.environment +
                       m.externalities.congestion for m in modes])

right_width_value = max_total - min_health

total_table_width_pc = 70

for m in modes:
    e1 = m.externalities

    right_total_value = (max(m.externalities.health, 0) +
                       m.externalities.co2 +
                       m.externalities.environment +
                       m.externalities.congestion)

    if e1.health < 0:
        left_padding = min_health + e1.health
        left_padding_pc = left_padding / max_total
        health_pc = abs(e1.health) / max_total
        health_class = "health negative"
    else:
        left_padding = min_health
        left_padding_pc = min_health /max_total
        health_pc = abs(e1.health) / max_total
        health_class = "health positive"

    co2_pc = abs(e1.co2) / max_total
    environment_pc = abs(e1.environment) / max_total
    congestion_pc = abs(e1.congestion) / max_total
    right_padding_pc = (max_total - right_total_value) / max_total

    bars = [("left_padding clear", left_padding_pc * total_table_width_pc),
            (health_class, health_pc * total_table_width_pc),
            ("CO2", co2_pc * total_table_width_pc),
            ("environment", environment_pc * total_table_width_pc),
            ("congestion", congestion_pc * total_table_width_pc),
            ("clear", right_padding_pc * total_table_width_pc)]

    bars = [(c, v) for c,v in bars if v > 0] # remove bars with zero width
    print (bars)
    m.externalitiy_bar_widths = bars

    m.total_external_cost_str = format_currency(right_total_value + m.externalities.health, "CHF", locale=locale)

    weekly_stats.ext_totals['health'] += e1.health
    weekly_stats.ext_totals['CO2'] += e1.co2
    weekly_stats.ext_totals['environment'] += e1.environment
    weekly_stats.ext_totals['congestion'] += e1.congestion

for k,v in list(weekly_stats.ext_totals.items()):
    weekly_stats.ext_totals[k+'_str'] = format_currency(v, "CHF", locale=locale)


html = mytemplate.render(title=_('report_title'),
                                            weekly_stats = weekly_stats,
                                            person = person_details,
                                            modes = modes,
                                            output_encoding='utf-8')
#, disable_basic_attributes=["width", "height", "valign", "align"]
inlined_html = premailer.Premailer(html, base_url="https://www.ivtmobis.ethz.ch/", strip_important=False).transform()

import pytracking
from pytracking.html import adapt_html

configuration = pytracking.Configuration(
    base_open_tracking_url="https://www.ivtmobis.ethz.ch/engagement/",
    base_click_tracking_url="https://www.ivtmobis.ethz.ch/engagement/",
    webhook_url="https://www.ivtmobis.ethz.ch/engagement_webhook",
    include_webhook_url=False)


new_html_email_text = adapt_html(
    inlined_html, extra_metadata={"partipant_id": 1, "report_id": 1, "sent_at": datetime.now().isoformat()},
    click_tracking=True, open_tracking=True, configuration=configuration)


with open("generation/test_report.html", "w", encoding="utf8") as file:
    file.write(new_html_email_text)

#write to webserver
with open("M:/htdocs/test_report.html", "w", encoding="utf8") as file:
    file.write(new_html_email_text)