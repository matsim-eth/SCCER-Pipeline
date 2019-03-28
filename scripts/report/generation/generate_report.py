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

from generation.process_data import build_mode_bar_chart, build_externality_barchart, build_email

language = 'en_GB'


connection = pg.connect(host='localhost',  dbname="sbb-green", user="postgres", password='password')

person_ids = ['1649', '1681']


for person_id in person_ids:
    report_details = {
        'person_id' : person_id,
        'week_number' : 1,
        'study_length' : 8,
        'week_start_date' : date(2016, 11, 30)
    }

    new_html_email_text = build_email(report_details, language, connection)
    with open("M:/htdocs/reports/{}_report_week1.html".format(person_id), "w", encoding="utf8") as file:
        file.write(new_html_email_text)

    #write to webserver
    report_details['week_number'] = 2
    report_details['week_start_date'] += timedelta(weeks=1)
    new_html_email_text = build_email(report_details, language, connection)
    with open("M:/htdocs/reports/{}_report.html".format(person_id), "w", encoding="utf8") as file:
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