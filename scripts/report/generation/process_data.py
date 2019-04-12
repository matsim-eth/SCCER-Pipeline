import pprint
import random
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from types import SimpleNamespace
import math

import numpy as np
import pandas as pd
from os import path
import psycopg2 as pg
import pandas.io.sql as psql

from babel.units import format_unit
from babel.numbers import format_currency
from babel.dates import *

from mako.lookup import TemplateLookup
from num2words import num2words
from premailer import premailer

import pytracking
from pytracking.html import adapt_html

configuration = pytracking.Configuration(
    base_open_tracking_url="https://www.ivtmobis.ethz.ch/open/",
    base_click_tracking_url="https://www.ivtmobis.ethz.ch/click/",
    webhook_url="https://www.ivtmobis.ethz.ch/engagement_webhook",
    include_webhook_url=False)


def get_date(fname):
    location = path.split(fname)[0]
    date = path.split(location)[1]
    return date

def hms_string(sec_elapsed):
    h = int(sec_elapsed / (60 * 60))
    h_string = format_unit(h, 'hour', length="short") if h > 0 else ""
    m = int((sec_elapsed % (60 * 60)) / 60)
    m_string = format_unit(m, 'minute', length="short") if (h > 0 or m > 0) or m == 0 else ""

    s = sec_elapsed % 60.

    return h_string + m_string


def distance_string(dist):
    km = int(dist / 1000)
    km_string = format_unit(km, 'kilometer', length="short") if km > 0 else ""
    m = int(dist)
    m_string = format_unit(m, 'meter', length="short") if m < 1000 else ""

    return km_string + m_string

def aggregate_externalities(legs_df, externalities_df):
    pass

def build_weekly_stats (person, legs_df, externalities_df):
    weekly_totals = legs_df.groupby('leg_mode')['distance'].sum().reset_index()


    weekly_stats = {
        'start_date' : date(2019, 2, 4), #
        'hours_str' : format_unit(2, 'hour'),

        'distance_total' : legs_df['distance'].sum(),
    #weekly_stats.distance_str = format_unit(weekly_stats.distance, 'kilometer', locale=locale)
        'distance_modes': legs_df.groupby('mode')['distance'].sum().to_dict(),

        'ext_totals' : {
            'health' : 0,
            'co2': 0,
            'environment': 0,
            'congestion':  0
        }
    }


def get_mode_image_src(mode):
    return 'images/mode_icons/{}-solid.gif'.format(mode.lower())

#test data

#format_unit(, "minute", "short", locale=locale)

#    'odd': "odd" if i % 2 else "even",
#    'health': 5 * (((i % 2) * -2) + 1),

def structure_externality_information(modes_df, locale):
    externalities = {
            'externality': 30,
            "my_norm":  8,
            "social norm": 8,
            'max_val' : 10000
    }


def build_mode_bar_chart(modes_df, norms_df, locale):
    mode_names = ['Car', 'Train', 'PT', 'Bicycle', 'Walk']

    total_dist = modes_df['distance'].sum()
    mode_bar_chart = {}

    max_total = np.ceil(modes_df['total']).max()
    min_health = abs(modes_df['health'].min())

    left_total_value = max(min_health, 0.1* max_total)

    total_table_width_pc = 70
    left_width_pc = 50
    right_width_pc = 80

    for mode in mode_names:
#########distance
        mode_bar_chart[mode] = {}

        my_average = norms_df.loc[mode, 'distance']

        distance = modes_df.loc[mode, 'distance']

        mode_bar_chart[mode]['mode_image_src'] = get_mode_image_src(mode)

        if abs(distance - my_average) / my_average > 0.4:
            mode_bar_chart[mode]['my_norm_image_src'] = 'images/arrows/{}.png'.format(
                "up_right" if distance > my_average else "down_right")

        mode_bar_chart[mode]['distance_pc'] = int(distance / total_dist * 100)
        mode_bar_chart[mode]['distance_str'] = format_unit(round(distance / 1000, 2), "kilometer", 'short',
                                                           format=u'##', locale=locale)


############externalities
        right_total_value = (max(modes_df.loc[mode, 'health'], 0) +
                             modes_df.loc[mode, 'co2'] +
                             modes_df.loc[mode, 'congestion'])


        left_padding_class = "left_padding clear"
        health_class = "health"


        co2_pc = math.ceil(abs(modes_df.loc[mode,  'co2']) / max_total * right_width_pc)
        congestion_pc = math.ceil(abs(modes_df.loc[mode, 'congestion']) / max_total * right_width_pc)

        right_padding_pc = right_width_pc - co2_pc - congestion_pc


        if modes_df.loc[mode, 'health'] < 0:
            health_pc = math.ceil(abs(modes_df.loc[mode, 'health']) / left_total_value * left_width_pc)
            left_padding = left_total_value + modes_df.loc[mode, 'health']
            left_padding_pc = left_width_pc - health_pc
            health_class += " right_border"

            bars_left = [(left_padding_class, left_padding_pc), (health_class, health_pc)]
            bars_right = []
        else:
            left_padding_pc = left_width_pc
            health_pc = math.ceil(abs(modes_df.loc[mode, 'health']) / max_total * right_width_pc)
            left_padding_class += " right_border"
            right_padding_pc -= health_pc

            bars_left = [(left_padding_class, left_padding_pc)]
            bars_right = [(health_class, health_pc)]


        bars_right += [("co2", co2_pc), ("congestion", congestion_pc), ("clear", right_padding_pc)]


        bars_right = [(c, v) for c,v in bars_right if v > 0] # remove bars with zero width


        mode_bar_chart[mode]['externalitiy_bars_right'] = bars_right
        mode_bar_chart[mode]['externalitiy_bars_left'] = bars_left

        mode_bar_chart[mode]['total_external_cost_str'] = \
            format_currency(right_total_value + modes_df.loc[mode, 'health'], "CHF", u'¤ #,##0.00; ¤ -#,##0.00', locale=locale)

    return (mode_bar_chart)


def build_externality_barchart(mode_values_df, norms_person, norms_group_df, show_norms, locale):
    ext_bars = {}

    extern_labels = ["health", 'co2', 'congestion']
    max_val_modes = max([mode_values_df[k].sum() for k in extern_labels])
    if show_norms:
        max_val_my_norm = max([norms_person.loc['Total', k] for k in extern_labels])
        max_val_cluster_norm = max([norms_group_df.loc['3', k] for k in extern_labels])
    else:
        max_val_my_norm = np.nan
        max_val_cluster_norm = np.nan

    max_val = max(max_val_modes,
                  max_val_my_norm,
                  max_val_cluster_norm)

    barchar_width = 75

    for k in extern_labels:
        ext_v = abs(mode_values_df[k].sum())
        social_norm = norms_group_df.loc['3', k]
        my_norm = norms_person.loc['Total', k] if show_norms else np.nan

        values = sorted([("0", 0), ("externality", ext_v),
                         ("my_average", my_norm),
                         ("max_val", max_val)], key=lambda x: x[1])
        values = [(k + " clear" if v > ext_v else k, v) for k, v in values if not np.isnan(v) ]

        classes, values1 = zip(*values)
        bar_widths = zip(classes[1:], np.diff(values1))

        ext_bars[k] = {"bar_widths": [[k1, max(round(w / max_val * barchar_width), 1)] for k1, w in bar_widths]}
        ext_bars[k]["bar_widths"][-1][1] = barchar_width - sum([v[1] for v in ext_bars[k]["bar_widths"][:-1]])

        if show_norms:
            norm_width = max(round(social_norm / max_val * barchar_width), 1)
            norm_padding = barchar_width - norm_width

            ext_bars[k]["norm_width"] = norm_width
            ext_bars[k]["norm_padding"] = norm_padding

            if ext_v > norms_group_df.loc['4', k]:
                ext_bars[k]["emoji_src"] = "images/smilies/verysad.png"
            elif ext_v > norms_group_df.loc['3', k]:
                ext_bars[k]["emoji_src"] = "images/smilies/sad.png"
            elif ext_v > norms_group_df.loc['2', k]:
                ext_bars[k]["emoji_src"] = "images/smilies/neutral.png"
            elif ext_v > norms_group_df.loc['1', k]:
                ext_bars[k]["emoji_src"] = "images/smilies/happy.png"
            else:
                ext_bars[k]["emoji_src"] = "images/smilies/veryhappy.png"

        print(ext_bars[k])

    return ext_bars


def build_email( report_details, language, connection):

    locale = Locale(language)
    import gettext
    lang_de = gettext.translation('generation', localedir="generation/locale", languages=[language])
    lang_de.install()

    person_id = report_details['person_id']
    report_details['week'] = report_details['week_number']
    report_details['week_ordinal'] = num2words(report_details['week'], ordinal=True, lang=language)
    report_details['remaining_weeks'] = num2words(report_details['study_length'], lang=language)
    report_details['report_date_str'] = format_date(report_details['week_start_date'], 'dd MMM', locale=locale)

    person_details = \
    pd.read_sql_query("SELECT * FROM participants where person_id = '{}'".format(person_id), connection).to_dict(
        'records')[0]

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

    norms_person = pd.read_sql_query(norms_sql.format('person_id', "'{}'".format(person_id)), connection).set_index(
        'leg_mode')
    norms_group = pd.read_sql_query('select * from externality_norms', connection).set_index('quintile')

    mode_index = ['Car', 'Train', 'PT', 'Bicycle', 'Walk']
    mode_values = leg_ext.groupby(['leg_mode'])[['distance', 'health', 'co2', 'congestion', 'total']].sum()
    mode_values = mode_values[mode_values.index.isin(mode_index)].reindex(mode_index).fillna(0)

    ext_totals = mode_values.sum().apply(lambda v: format_currency(v, 'CHF', u'¤ ###,##0.00', locale=locale))

    mode_bar_chart = build_mode_bar_chart(mode_values, norms_person, locale)

    show_norms = person_details['treatment'] == 'Nudging'
    externality_bar_chart = build_externality_barchart(mode_values, norms_person, norms_group, show_norms, locale)

    # ext_summary = mode_values.sum()

    pprint.pprint(person_details)

    template_lookup = TemplateLookup(directories=['generation/templates'], strict_undefined=True)

    mytemplate = template_lookup.get_template("treatment_template.html")

    from mako import exceptions

    try:
        html = mytemplate.render(title=_('report_title'),
                                 report_details=report_details,
                                 person=person_details,
                                 mode_values=mode_values,
                                 mode_bar_chart=mode_bar_chart,
                                 externality_bar_chart=externality_bar_chart,
                                 ext_totals=ext_totals,
                                 output_encoding='utf-8')
    except:
        print(exceptions.text_error_template().render())

    import logging
    import cssutils
    cssutils.log.setLevel(logging.ERROR)

    # , disable_basic_attributes=["width", "height", "valign", "align"]
    inlined_html = premailer.Premailer(html, base_url="https://www.ivtmobis.ethz.ch/",
                                       strip_important=False).transform()

    new_html_email_text = adapt_html(
        inlined_html, extra_metadata={"partipant_id": person_id, "report_id": "report_{}".format(report_details['week']), "sent_at": datetime.now().isoformat()},
        click_tracking=True, open_tracking=True, configuration=configuration)

    return new_html_email_text


def generate_welcome_email(person_id, language, connection):

    locale = Locale(language)
    import gettext
    lang_de = gettext.translation('generation', localedir="generation/locale", languages=[language])
    lang_de.install()

    person_details = \
    pd.read_sql_query("SELECT * FROM participants where person_id = '{}'".format(person_id), connection).to_dict(
        'records')[0]

    template_lookup = TemplateLookup(directories=['generation/templates'], strict_undefined=True)

    mytemplate = template_lookup.get_template("welcome.html")

    from mako import exceptions

    try:
        html = mytemplate.render(title=_('report_title'),
                                 person=person_details,
                                 output_encoding='utf-8')
    except:
        print(exceptions.text_error_template().render())

    import logging
    import cssutils
    cssutils.log.setLevel(logging.ERROR)

    # , disable_basic_attributes=["width", "height", "valign", "align"]
    inlined_html = premailer.Premailer(html, base_url="https://www.ivtmobis.ethz.ch/",
                                       strip_important=False).transform()


    new_html_email_text = adapt_html(
        inlined_html, extra_metadata={"partipant_id": person_details['person_id'], "report_id": "welcome",
                                      "sent_at": datetime.now().isoformat()},
        click_tracking=True, open_tracking=True, configuration=configuration)

    return new_html_email_text


import smtplib


def send_mail(receiver, subject, html_email_text):
    try:
        server = smtplib.SMTP_SSL('smtp.gmail.com', 465)
        server.ehlo()
        server.login("ivtmobistest", "emailtesting")

        me = "ivtmobistest@gmail.com"
        you = receiver

        # Create message container - the correct MIME type is multipart/alternative.
        msg = MIMEMultipart('alternative')
        msg['Subject'] = subject
        msg['From'] = me
        msg['To'] = you

        part2 = MIMEText(html_email_text, 'html')

        msg.attach(part2)
        server.sendmail(me, you, msg.as_string())

    except Exception  as e:
        print('Something went wrong...')
        print(e)
