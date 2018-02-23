
#############################################################################################
##													##
## Dieses Skript definiert den Kanton und das Fuel Price Level damit alle Dateien 		##
## die generiert werden einheitliche und erkennbare Namen haben und von den Skripten	##
## jeweils aufgerufen werden können. 								##	
#############################################################################################


rm (list = ls())
library(foreign)
homepath <- Sys.getenv("HOME")


#################################################################
##	Select Canton here:						##
##     Name 		:	MATSim		:   VZ2000		##
##	Zuerich	:	ZH		:	1 		##
##	Bern		:	BE		:	2		##
##	Luzern		:	LU		:	3		##
##	Uri		:	UR		:	4		##
##	Schwyz		:	SZ		:	5		##
##	Obwalden	:	OW		:	6		##
##	Nidwalden	:	NW		:	7		##
##	Glarus		:	GL		:	8		##
##	Zug		:	ZG		:	9		##
##	Fribourg	:	FR		:	10		##
##	Solothurn	:	SO		:	11		##
##	BaselStadt	:	BS		:	12		##
##	BaselLand	:	BL		:	13		##
##	Schaffhausen	:	SH		:	14		##
##	App Inner	:	AR		:	15		##
##	App Aussen	:	AI		:	16		##
##	St. Gallen	:	SG		:	17		##
##	Graubuenden	:	GR		:	18		##
##	Aargau		:	AG		:	19		##
##	Thurgau	:	TG		:	20		##
##	Tessin		:	TI		:	21		##
##	Waadt		:	VD		:	22		##
##	Wallis		:	VS		:	23		##
##	Neuenburg	:	NE		:	24		##
##	Genf		:	GE		:	25		##
##	Jura		:	JU		:	26		################
##											##		
## Hier bitte den Kanton und das Fuelprice Level (in CHF/Liter) eingeben.	##
## ----------------------------------------------------------------------	#########
##												##
KantMATS = "###Kantonskuerzel###"			## MATSim-Kantonsname eingeben, z.B. "ZH"			##
KantVZ   = ###Kantonsnummer###			## VZ2000 Kantonsnummer eingeben, z.B. 1			##
Fuelprice = ###Benzinpreis###			## Benzinpreis in CHF/Liter, Default ist 1.75		##
##												##
######################################################################################

Run_Names <- data.frame("KantMats" = "Empty", "KantVZ" = 00, "FP" = 0.0) 
Run_Names$KantMats <- KantMATS
Run_Names$KantVZ <- KantVZ
Run_Names$FP <- Fuelprice

setwd(paste(homepath, "/P/Projekte/matsim/Flottenmodel/R_Scripts", sep = "")
#setwd("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/R_Scripts")
write.table(Run_Names, file = "Spez_Names.txt", quote = FALSE, sep = "\t", row.names = FALSE)


Zatog <- data.frame("Text1" = c(1:4), "Text2" =0)
Zatog$Text1[1] <- "input"
Zatog$Text2[1] <- paste(homepath, "/P/Projekte/matsim/Flottenmodel/MATSim_Implementation/Sim_Input_Data/G_HHs_",KantMATS,"_",Fuelprice,".dat;",sep="")
Zatog$Text1[2] <- "output"
Zatog$Text2[2] <- paste(homepath, "/P/Projekte/matsim/Flottenmodel/MATSim_Implementation/Sim_Input_Data/G_HHs_",KantMATS,"_",Fuelprice,"1.dat;",sep="")
Zatog$Text1[3] <- "invar"
Zatog$Text2[3] <- "ZGDE hhId FAC1_M FAC1_F FAC2 FAC3 AC1 AC2 AC3 Kids zpers ausb eink autos VMT UrbCi ERF1 ERF2 Income TaxMun Fuel Fuel2 D_00 D_01 D_02 D_03 D_04 D_05 D_06 B_00 B_01 B_02 B_03 B_04 B_05 B_06 B_07 U_00 uno sero caseid;"
Zatog$Text1[4] <- "outvar"
Zatog$Text2[4] <- "ZGDE hhId FAC1_M FAC1_F FAC2 FAC3 AC1 AC2 AC3 Kids zpers ausb eink autos VMT UrbCi ERF1 ERF2 Income TaxMun Fuel Fuel2 D_00 D_01 D_02 D_03 D_04 D_05 D_06 B_00 B_01 B_02 B_03 B_04 B_05 B_06 B_07 U_00 uno sero caseid;"


setwd(paste(homepath, "/P/Projekte/matsim/Flottenmodel/MATSim_Implementation", sep = ""))
write.table(Zatog, file = "Zatog1.cmd", quote = FALSE, sep =" ", row.names = FALSE, col.names = FALSE)




