import re
from lxml import etree


def file_len(fname):
    with open(fname) as f:
        for i, l in enumerate(f):
            pass
    return i + 1
reg = r"\s(\S*?)(?=\=)"
type_identifier = r'type="(.*?)"'

file  = r"C:\Projects\SCCER_project\scenarios\astra\20.events.xml"

set_of_variables  = set()

allowedTypes = {"left link", "entered link", "arrival", "departure"}
recordLen = []

from itertools import islice
with open(file) as myfile:
    head = list(islice(myfile, 1000000))

    for line in head[2:]:
        variables = re.findall(reg,line)
        type = re.findall(type_identifier,line)
        [[set_of_variables.add((t,v)) for t in type] for v in variables]

        root = etree.XML(line)
        xmldict = root.attrib
       # if xmldict['type'] in allowedTypes:
            #print xmldict
        recordLen.append(len(line))

set_of_variables = sorted(set_of_variables)

for v in set_of_variables:
    print v

print "max recordLength " +  str(sum(recordLen)/len(recordLen))
print "file length " + str(file_len(file))

