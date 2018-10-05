import random

import numpy as np
import pandas as pd
from datetime import datetime
from os import path
import glob

from babel.units import format_unit
from babel.dates import format_date, format_datetime, format_time

from mako.template import Template
from mako.lookup import TemplateLookup

template_lookup = TemplateLookup(directories=['report/generation/templates'])

locale = 'de'

import gettext
lang_de = gettext.translation('generation', localedir="report/generation/locale", languages=[locale])
lang_de.install()

to_datetime = lambda d: datetime.strptime(d, '%Y-%m-%d')


files = glob.glob(r"C:\Projects\SCCER_project\output_gc\externalities/2016-12-*/1602_congestion.csv")

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

ordered_files = sorted([(get_date(fname), fname) for fname in files], key= (lambda x: x[0]))

most_recent_week = ordered_files[-7:]

[print (f) for f in most_recent_week]

df = pd.concat([pd.read_csv(f, sep=";", converters={'Date': to_datetime} ) for (date,f) in most_recent_week], ignore_index=True)
print(df.dtypes)
df["short_date"] = df.Date.apply(lambda d : format_date(d, 'd MMM', locale=locale))
df["display_time"] = (df.EndTime - df.StartTime).apply(hms_string)
df["display_distance"] = (df.matsim_delay).apply(distance_string)
df["display_exp_delay"] = (df.delay_experienced).apply(hms_string)
df["display_matsim_delay"] = (df.delay_caused).apply(hms_string)
df["emissions_string"] = (df.Mode).apply(get_emissions)
#df["short_date"] = df.Date.dt.strftime('%d %b')
print(df)

mytemplate = template_lookup.get_template("newsletter_template.html")


print(mytemplate.render(title="test", df=df))

with open("output/test_report.html", "w") as file:
    file.write(mytemplate.render(title=_('report_title'), df=df))

print (df.iloc[0])
