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
from babel.dates import format_date, format_datetime, format_time

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


# connection = pg.connect(host='localhost',  dbname="sbb-green", user="postgres", password='password')
connection = pg.connect(dbname="ctchervenkov", user="ctchervenkov", password='abc')

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

week = datetime(2007, 4, 1, 15, 30)
week =  format_date(week, "long", locale=locale)
weekly_hours = format_unit(2, 'hour', locale=locale)
weekly_stats = SimpleNamespace()
weekly_stats.hours = weekly_hours
weekly_stats.car = 20
weekly_stats.bus = 30
weekly_stats.walk = 50
weekly_stats.bike = 50

html = mytemplate.render(title=_('report_title'),
                                          weekly_totals = weekly_totals,
                                          person = person_details,
                                            week = week,
                                        weekly_stats = weekly_stats,
                                          distance_week = distance_string(distance_week))

inlined_css = premailer.transform(html, )

with open("generation/test_report.html", "w") as file:
    file.write(inlined_css)

