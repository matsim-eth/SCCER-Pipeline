import xlrd
import csv
import sys
import os

def csv_from_excel(input, sheet_index=0):
	output = os.path.splitext(input)[0] + ".csv"

	wb = xlrd.open_workbook(input)
	sh = wb.sheet_by_index(sheet_index)
	your_csv_file = open(output, 'w', encoding='utf-8', newline='')
	wr = csv.writer(your_csv_file, delimiter=';')

	for rownum in range(sh.nrows):
		wr.writerow(sh.row_values(rownum))

	your_csv_file.close()

csv_from_excel(sys.argv[1])