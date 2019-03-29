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

from generation.process_data import build_mode_bar_chart, build_externality_barchart, build_email, \
    generate_welcome_email, send_mail

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

    welcome_email = generate_welcome_email(person_id, language, connection)
    with open("M:/htdocs/reports/{}_welcome_email.html".format(person_id), "w", encoding="utf8") as file:
        file.write(welcome_email)



    new_html_email_text = build_email(report_details, language, connection)
    with open("M:/htdocs/reports/{}_report_week1.html".format(person_id), "w", encoding="utf8") as file:
        file.write(new_html_email_text)

    #write to webserver
    report_details['week_number'] = 2
    report_details['week_start_date'] += timedelta(weeks=1)
    new_html_email_text = build_email(report_details, language, connection)
    with open("M:/htdocs/reports/{}_report.html".format(person_id), "w", encoding="utf8") as file:
        file.write(new_html_email_text)


send_mail("joseph.molloy@ivt.baug.ethz.ch", "Welcome to MOBIS", welcome_email)
send_mail("joseph.molloy@ivt.baug.ethz.ch", "MOBIS week 1 report", new_html_email_text)

