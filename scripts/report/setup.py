import os
from setuptools import setup

# Utility function to read the README file.
# Used for the long_description.  It's nice, because now 1) we have a top level
# README file and 2) it's easier to type in the README file than to put a raw
# string in below ...
def read(fname):
    return open(os.path.join(os.path.dirname(__file__), fname)).read()

setup(
    name = "Emissions Feedback Generator",
    version = "0.1",
    author = "Molloy Joseph",
    author_email = "joseph.molloy@ivt.baug.ethz.ch",
    description = ("WIP."),
    license = "MIT",
    keywords = "",
    url = "",
    packages=['generation'],
    long_description=read('README'),
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Topic :: Utilities"
    ],
    install_requires=[
        "Mako", "Babel"
        ],
)