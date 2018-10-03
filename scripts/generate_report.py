import numpy as np
import pandas as pd
from os import path

import glob
from mako.template import Template

files = glob.glob(r"C:\Projects\SCCER_project\output_gc\externalities/*/1602_congestion.csv")


def get_date(fname):
    location = path.split(fname)[0]
    date = path.split(location)[1]
    return date

ordered_files = sorted([(get_date(fname), fname) for fname in files], key= (lambda x: x[0]))

most_recent_week = ordered_files[-7:]

[print (f) for f in most_recent_week]

df = pd.concat([pd.read_csv(f, sep=";") for (date,f) in most_recent_week], ignore_index=True)

print(df)

mytemplate = Template(filename='myreport.html')

print(mytemplate.render(title="test", df=df))
