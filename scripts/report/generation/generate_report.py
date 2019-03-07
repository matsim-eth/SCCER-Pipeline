import pprint
import random
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from types import SimpleNamespace

import numpy as np
import pandas as pd
from os import path
import psycopg2 as pg
from datetime import date

from babel.units import format_unit
from babel.numbers import format_currency
from babel.dates import *
from babel import Locale
from num2words import num2words

from mako.lookup import TemplateLookup
from premailer import premailer

from generation.process_data import build_mode_bar_chart, build_externality_barchart

language = 'en_GB'
locale = Locale(language)
import gettext
lang_de = gettext.translation('generation', localedir="generation/locale", languages=[language])
lang_de.install()


person_id = '1649'
week_number = 1
study_length = 8

report_details = {}
report_details['week_start_date'] = date(2016, 11, 30)
report_details['report_date_str'] = format_date(report_details['week_start_date'], 'dd MMM', locale=locale)

report_details['week'] = week_number
report_details['week_ordinal'] = num2words(week_number, ordinal=True, lang=language)
report_details['remaining_weeks'] = num2words(study_length, lang=language)

connection = pg.connect(host='localhost',  dbname="sbb-green", user="postgres", password='password')

person_details = pd.read_sql_query("SELECT * FROM participants where person_id = '{}'".format(person_id), connection).to_dict('records')[0]

person_details['group'] = 'nudging'

leg_details = pd.read_sql_query("SELECT * FROM legs where person_id = '{}' "
                                "and to_char(leg_date, 'YYYY-IW') = '2016-50'".format(person_id), connection)
leg_in_list = ','.join(map(str, leg_details['leg_id']))

leg_ext = pd.read_sql_query("SELECT * FROM wide_externalities where leg_id in ({})".format(leg_in_list), connection)

norms_sql = '''
with ll as (
	select EXTRACT(WEEK FROM leg_date) as week, leg_mode, sum(health) as health, sum (distance) as distance, 
	 sum(co2) as co2, sum(congestion) as congestion, sum(total) as total
	from wide_externalities
	where {} = {}
	group by week, leg_mode
) 

select leg_mode, avg(health) as health, avg (distance) as distance, 
    avg(co2) as co2, avg(congestion) as congestion, avg(total) as total
from ll
group by leg_mode
union 
select 'Total' as leg_mode, avg(health) as health, avg (distance) as distance, 
    avg(co2) as co2, avg(congestion) as congestion, avg(total) as total
from ll
'''

norms_person = pd.read_sql_query(norms_sql.format('person_id', "'{}'".format(person_id)), connection).set_index('leg_mode')
norms_group = pd.read_sql_query('select * from externality_norms', connection).set_index('quintile')


mode_index = ['Car', 'Train', 'PT', 'Bicycle', 'Walk']
mode_values = leg_ext.groupby(['leg_mode'])[['distance', 'health', 'co2', 'congestion', 'total']].sum()
mode_values = mode_values[mode_values.index.isin(mode_index)].reindex(mode_index).fillna(0)


ext_totals = mode_values.sum().apply(lambda v : format_currency(v, 'CHF', u'¤ ###,##0.00', locale=locale))

mode_bar_chart = build_mode_bar_chart(mode_values, norms_person, locale)

externality_bar_chart = build_externality_barchart(mode_values, norms_person, norms_group, locale)

#ext_summary = mode_values.sum()

pprint.pprint(person_details)


template_lookup = TemplateLookup(directories=['generation/templates'],strict_undefined=True )

mytemplate = template_lookup.get_template("control.html")


from mako import exceptions

try:
    html = mytemplate.render(title=_('report_title'),
                                            report_details = report_details,
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

import smtplib

try:
    server = smtplib.SMTP_SSL('smtp.gmail.com', 465)
    server.ehlo()
    server.login("ivtmobistest", "emailtesting")

    me = "ivtmobistest@gmail.com"
    you = "ivtmobistest@gmail.com"

    # Create message container - the correct MIME type is multipart/alternative.
    msg = MIMEMultipart('alternative')
    msg['Subject'] = "Link"
    msg['From'] = me
    msg['To'] = you

    part2 = MIMEText(new_html_email_text, 'html')

    msg.attach(part2)
    server.sendmail(me, you, msg.as_string())

except :
    print ('Something went wrong...')