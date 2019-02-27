import pprint
import random
from types import SimpleNamespace

import numpy as np
import pandas as pd
from os import path
import psycopg2 as pg
import pandas.io.sql as psql

from babel.units import format_unit
from babel.numbers import format_currency
from babel.dates import *
from babel import Locale

from mako.lookup import TemplateLookup
from premailer import premailer

from generation.process_data import build_mode_bar_chart, build_externality_barchart

language = 'de'
locale = Locale(language)
import gettext
lang_de = gettext.translation('generation', localedir="generation/locale", languages=[language])
lang_de.install()


to_datetime = lambda d: datetime.strptime(d, '%Y-%m-%d')

person_id = '1649'
start_date = '2016-30-11'

connection = pg.connect(host='localhost',  dbname="sbb-green", user="postgres", password='password')

person_details = pd.read_sql_query("SELECT * FROM participants where person_id = '{}'".format(person_id), connection).to_dict('records')[0]


leg_details = pd.read_sql_query("SELECT * FROM legs where person_id = '{}' "
                                "and to_char(leg_date, 'YYYY-IW') = '2016-50'".format(person_id), connection)
leg_in_list = ','.join(map(str, leg_details['leg_id']))

externalities = pd.read_sql_query("SELECT * FROM externalities where leg_id in ({})".format(leg_in_list), connection)
grouped_df = externalities.groupby("leg_id")


wide_externalities = externalities.pivot(index='leg_id', columns='variable', values='val')

wide_externalities['health'] = wide_externalities['PM_health_costs'] + \
                               wide_externalities['Noise_costs'] + \
                               wide_externalities['NOx_costs']

wide_externalities['environment'] = wide_externalities['PM_building_damage_costs'] + \
                               wide_externalities['Zinc_costs']

wide_externalities['co2'] = wide_externalities['CO2_costs']
wide_externalities['congestion'] = wide_externalities['delay_caused'] * 26.1 / 3600



leg_ext = wide_externalities.merge(leg_details, on='leg_id')[
    ['leg_mode', 'distance', 'health','environment', 'congestion', 'co2']]

mode_values = leg_ext.groupby(['leg_mode']).sum()

mode_values['total'] = mode_values['health'] + \
                       mode_values['environment'] + \
                       mode_values['co2'] + \
                       mode_values['congestion']


ext_totals = mode_values.sum().apply(lambda v : format_currency(v, 'CHF', locale=locale))

mode_bar_chart = build_mode_bar_chart(mode_values, locale)

externality_bar_chart = build_externality_barchart(mode_values, locale)

#ext_summary = mode_values.sum()

pprint.pprint(person_details)


template_lookup = TemplateLookup(directories=['generation/templates'],strict_undefined=True )

mytemplate = template_lookup.get_template("control.html")


from mako import exceptions

try:
    html = mytemplate.render(title=_('report_title'),
                                            week_start_date = start_date,
                                            person = person_details,
                                            mode_values = mode_values,
                                            mode_bar_chart = mode_bar_chart,
                                            externality_bar_chart = externality_bar_chart,
                                            ext_totals = ext_totals,
                                            output_encoding='utf-8')
except:
    print(exceptions.text_error_template().render())

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