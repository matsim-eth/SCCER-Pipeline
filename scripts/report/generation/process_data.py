import pprint
import random
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
from premailer import premailer


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
    mode_names = ['Car', 'Train', 'Bicycle', 'Walk']

    total_dist = modes_df['distance'].sum()
    mode_bar_chart = {}
    max_val = max(modes_df['distance'].max(),
                  norms_df['cluster_distance'].max(),
                  norms_df['my_distance'].max()) * 1.1

    barchart_width = 60


    for mode in mode_names:
        mode_bar_chart[mode] = {}

        pop_average = norms_df.loc[mode, 'cluster_distance']
        my_average = norms_df.loc[mode, 'my_distance']

        distance = modes_df.loc[mode, 'distance']

        mode_bar_chart[mode]['mode_image_src'] = get_mode_image_src(mode)

        mode_bar_chart[mode]['distance_pc'] = int(distance / total_dist * 100)
        mode_bar_chart[mode]['distance_str'] = format_unit(round(distance / 1000, 2), "kilometer", "short", locale=locale)

        values = sorted([("0", 0), ("distance", distance),
                         ("pop_average", pop_average), ("my_average", my_average),
                         ("max_val", max_val+10)], key=lambda x: x[1])
        values = [(k + " clear" if v > distance else k, v) for k,v in values]



        classes, values1 = zip(*values)
        bar_widths = list(zip(classes[1:], np.diff(values1)))

        mode_bar_chart[mode]['distance_bars'] = [(k, max(round(w / max_val * barchart_width), 1)) for k, w in bar_widths]
        mode_bar_chart[mode]['distance_bars'][-1] = (mode_bar_chart[mode]['distance_bars'][-1][0],
                           barchart_width - sum([v[1] for v in mode_bar_chart[mode]['distance_bars'][:-1]]))
        print(mode_bar_chart[mode]['distance_bars'])

    max_total = modes_df['total'].max() * 1.1
    min_health = abs(modes_df['health'].min())

    left_total_value = max(min_health, 0.1* max_total)

    total_table_width_pc = 70
    left_width_pc = round(left_total_value/(left_total_value + max_total)*total_table_width_pc)
    right_width_pc = total_table_width_pc - left_width_pc

    for mode in mode_names:

        right_total_value = (max(modes_df.loc[mode, 'health'], 0) +
                             modes_df.loc[mode, 'co2'] +
                             modes_df.loc[mode, 'environment'] +
                             modes_df.loc[mode, 'congestion'])


        left_padding_class = "left_padding clear"
        health_class = "health"


        co2_pc = math.ceil(abs(modes_df.loc[mode,  'co2']) / max_total * right_width_pc)
        environment_pc = math.ceil(abs(modes_df.loc[mode, 'environment']) / max_total * right_width_pc)
        congestion_pc = math.ceil(abs(modes_df.loc[mode, 'congestion']) / max_total * right_width_pc)

        right_padding_pc = right_width_pc - co2_pc - environment_pc - congestion_pc


        if modes_df.loc[mode, 'health'] < 0:
            health_pc = math.ceil(abs(modes_df.loc[mode, 'health']) / left_total_value * left_width_pc)
            left_padding = left_total_value + modes_df.loc[mode, 'health']
            left_padding_pc = left_width_pc - health_pc
            health_class += " right_border"
        else:
            left_padding_pc = left_width_pc
            health_pc = math.ceil(abs(modes_df.loc[mode, 'health']) / max_total * right_width_pc)
            left_padding_class += " right_border"
            right_padding_pc -= health_pc


        bars = [(left_padding_class, left_padding_pc),
                (health_class, health_pc),
                ("co2", co2_pc),
                ("environment", environment_pc),
                ("congestion", congestion_pc),
                ("clear", right_padding_pc)]

        bars = [(c, v) for c,v in bars if v > 0 or 'left_padding' in c] # remove bars with zero width


        mode_bar_chart[mode]['externalitiy_bars'] = bars

        mode_bar_chart[mode]['total_external_cost_str'] = \
            format_currency(right_total_value + modes_df.loc[mode, 'health'], "CHF", locale=locale)

    return (mode_bar_chart)


def build_externality_barchart(mode_values_df, norms_df, locale):
    ext_bars = {}

    norms_df_summed = norms_df.sum()

    extern_labels = ["health", 'co2', 'environment', 'congestion']
    max_val_modes = max([mode_values_df[k].sum() for k in extern_labels])
    max_val_my_norm = max([norms_df['my_'+k].sum() for k in extern_labels])
    max_val_cluster_norm = max([norms_df['cluster_'+k].sum() for k in extern_labels])

    max_val = max(max_val_modes,
                  max_val_my_norm,
                  max_val_cluster_norm) * 1.1

    barchar_width = 60

    for k in extern_labels:
        ext_v = abs(mode_values_df[k].sum())
        social_norm = norms_df_summed['cluster_'+k]
        my_norm = norms_df_summed['my_'+k]

        values = sorted([("0", 0), ("externality", ext_v),
                         ("pop_average", social_norm), ("my_average", my_norm),
                         ("max_val", max_val)], key=lambda x: x[1])
        values = [(k + " clear" if v > ext_v else k, v) for k, v in values]

        classes, values1 = zip(*values)
        bar_widths = zip(classes[1:], np.diff(values1))

        ext_bars[k] = [(k, max(round(w / max_val * barchar_width), 1)) for k, w in bar_widths]
        ext_bars[k][-1] = (ext_bars[k][-1][0],
                           barchar_width - sum([v[1] for v in ext_bars[k][:-1]]))

        print(sum([v[1] for v in ext_bars[k]]))
        print(ext_bars[k])

    return ext_bars
