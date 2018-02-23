
###############################################################################
##											##
## Dieses Skript generiert die Daten Tabelle welche als Input dient		##
## für die Simulation des MDCEV Flottenwahl Model für Kanton Zürich und	##
## ganze Schweiz									##
##											##	
###############################################################################

rm (list = ls())
library(foreign)

																		##
KantNames <- c("ZH", "BE", "LU", "UR", "SZ", "OW", "NW", "GL", "ZG", "FR", "SO", "BS", "BL", "SH", "AR", "AI", "SG", "GR", "AG", "TG", "TI", "VD", "VS", "NE", "GE", "JU")

## Gemeinde Daten werden vorderhand aufbereitet ##
## ---------------------------------------

Path.GDS   <- "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/GemeindeDateien/GDESchluessel.sav"
GDS <- data.frame(read.spss(Path.GDS, use.value.labels = FALSE,trim.factor.names = FALSE))
Erreich <- data.frame(read.csv("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/GemeindeDateien/Err_Faktorisiert.csv", sep = ",", header=TRUE))

GDS <- aggregate(GDS, by = list(GDS$W_BFS), FUN = mean)
names(GDS)[1]<- "ZGDE"
GDS$UrbCi <- ifelse(GDS$W_STRUKTUR_AGG == 1,1,0)
GDS$W_BFS <- NULL
GDS$W_STRUKTUR_AGG <- NULL

Erreich[1] <- NULL
Erreich[1] <- NULL
Erreich[2] <- NULL
Erreich[2] <- NULL
Erreich[2] <- NULL
Erreich[4] <- NULL
names(Erreich)[1] <- "ZGDE"
names(Erreich)[2] <- "ERF1"
names(Erreich)[3] <- "ERF2"

## Über die Gemeinde Nummer werden die Bundessteuern pro Kopf (= Einkommensniveau) pro Gemeinde zugespielt ##
###### --- Die Steuerdaten pro Gemeinde werden hinzugefügt ---- ###########
############################################################################

Steuerdaten <- read.csv("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/GemeindeDateien/GemeindeSteuern08.csv", sep = ";", header = TRUE)#, colClasses = c("int", "integer", "character", "integer", "integer","integer"))
Steuerdaten <- subset(Steuerdaten, select = c("GEMNR", "Tax_Capita"))


Steuerdaten$Tax_Capita <- Steuerdaten$Tax_Capita/1000
names(Steuerdaten)[1] <- "ZGDE"
names(Steuerdaten)[2] <- "TaxMun"


## 5% Sample der Ganzen Schweiz wird gemacht. Jeweils von jedem Kanto 5 Protzent und dann zusammengesetzt. ##
## ---------------------------------------

HH_Swiss_5pr <- data.frame("ZGDE"=0,"householdId"=0,"FA.AgeC1_M"=0,"FA.AgeC1_F"=0,"FA.AgeC2"=0,"FA.AgeC3" =0, "AgeC1"=0,"AgeC2"=0,"AgeC3"=0,"Kid"=0,"ZPERS"=0,"AUSB"=0,
"EINK"=0,"AUTOS"=0,"VMT"=0,"UrbCi"=0,"ERF1"=0,"ERF2"=0,"Income"=0,"TaxMun"=0,"Fuel"=0,"Fuel2"=0,"D_00"=0,"D_01"=0,"D_02"=0,"D_03" =0,"D_04"=0,"D_05"=0,"D_06"=0,"B_00"=0,"B_01"=0,
"B_02" =0,"B_03"=0,"B_04"=0, "B_05"=0,"B_06"=0,"B_07"=0,"U_00"=0,"uno"=0,"sero"=0,"NR"=0)

for (i in 1:26){

KantMATS <- KantNames[i]
KantVZ <- i
Fuelprice <- 1.75


Path.Pers <- paste("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/Pers_Files/Pers_", KantMATS, ".txt", sep = "")       ## Das Persfile des jeweiligen Kantons. 
Path.HH   <- paste("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/HH_Files/KANT_", KantVZ, "_out.tab", sep = "")       ## Im Housholdfile stehen Einkommenslevel und anzahl Autos pro Haushalt drin. Generiert von Kirill Müller
Path.VZ   <- paste("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/VZ_Files/KANT_", KantVZ, ".sav", sep = "")

Pers <- read.table(Path.Pers, header = TRUE, sep = ";")
HH <-   read.table(Path.HH, header = TRUE, sep = "\t")
VZ.GD <- data.frame(read.spss(Path.VZ))






## Anzahl Menschen mit Fahrausweis pro Altersgruppe und Geschlecht und Menschen insgesammt pro Altersgruppe ##
## ---------------------------------------

Pers$FA.AgeC1_M <- ifelse(Pers$sex == "m" & Pers$age > 18 & Pers$age < 46 & Pers$license == "true",1,0)
Pers$FA.AgeC1_F <- ifelse(Pers$sex == "f" & Pers$age > 18 & Pers$age < 46 & Pers$license == "true",1,0)
Pers$FA.AgeC2 <- ifelse(Pers$age > 45 & Pers$age < 66 & Pers$license == "true",1,0)
Pers$FA.AgeC3 <- ifelse(Pers$age > 65 & Pers$license == "true",1,0)

Pers$Kid <- ifelse(Pers$age <19,1,0)
Pers$AgeC1 <- ifelse(Pers$age > 18 & Pers$age < 46,1,0)
Pers$AgeC2 <- ifelse(Pers$age > 45 & Pers$age < 66,1,0)
Pers$AgeC3 <- ifelse(Pers$age > 65,1,0)

Pers1 <- subset(Pers, select = c("householdId", "FA.AgeC1_M", "FA.AgeC1_F", "FA.AgeC2", "FA.AgeC3", "AgeC1", "AgeC2", "AgeC3", "Kid"))
Pers.aggr <- aggregate(Pers1, by = list(Pers1$householdId), FUN = sum)
Pers.aggr$householdId <- NULL

names(HH)[1] <- "householdId"
names(Pers.aggr)[1] <- "householdId"

head(Pers)

## Modell für die Fahrleistung (=VMT) wird angewandt ##
## ---------------------------------------

Intercept <- 14.928
LogInc <- 1.890
AnzAutos <- 4.236
AC1_M <- 1.538
AC1_F <- 0.514
AC2 <- 0.371
AC3 <- -1.013

Households <- merge(Pers.aggr,HH)
Households$VMT <- (Intercept + LogInc * log(Households$EINK) + AnzAutos * Households$AUTOS + AC1_M * Households$FA.AgeC1_M + AC1_F * Households$FA.AgeC1_F + AC2 * Households$FA.AgeC2 + AC3 * Households$FA.AgeC3)^3



## Über die Gemeinde Nummer wird die Variable UrbCi zugespielt ##
## ---------------------------------------

VZ.GD.aggr <- aggregate(VZ.GD, by = list(VZ.GD$HHNR), FUN = mean)
names(VZ.GD.aggr)[1]<- "householdId"
VZ.GD.aggr$KANT <- NULL
VZ.GD.aggr$HHNR <- NULL
VZ.GD.aggr$PERSON_ID <- NULL



Households <- merge(Households,VZ.GD.aggr)
Households <- merge(Households, GDS)
Households <- merge(Households, Erreich)


Households$Income <- 0

Households$Income <- ifelse(Households$EINK == 1,1,Households$Income)
Households$Income <- ifelse(Households$EINK == 2,3,Households$Income)
Households$Income <- ifelse(Households$EINK == 3,5,Households$Income)
Households$Income <- ifelse(Households$EINK == 4,1,Households$Income)
Households$Income <- ifelse(Households$EINK == 5,7,Households$Income)
Households$Income <- ifelse(Households$EINK == 6,9,Households$Income)
Households$Income <- ifelse(Households$EINK == 7,11,Households$Income)
Households$Income <- ifelse(Households$EINK == 8,15,Households$Income)
Households$Income <- ifelse(Households$EINK == 9,18,Households$Income)


Households <- merge(Households, Steuerdaten)

Households$Fuel <- Fuelprice
Households$Fuel2 <- Fuelprice^2

Households$D_00 <- Households$VMT/10000
Households$D_01 <- 0
Households$D_02 <- 0
Households$D_03 <- 0
Households$D_04 <- 0
Households$D_05 <- 0
Households$D_06 <- 0
Households$D_01 <- 0
Households$B_00 <- 0
Households$B_01 <- 0
Households$B_02 <- 0
Households$B_03 <- 0
Households$B_04 <- 0
Households$B_05 <- 0
Households$B_06 <- 0
Households$B_07 <- 0
Households$U_00 <- 0

Households$uno <- 1
Households$sero <- 0



Households$NR <- c(1:length(Households$uno))



Households$Zufall <- runif(length(Households$householdId))
HH_5P <- subset(Households, Households$Zufall <= 0.05)
HH_5P$Zufall <- NULL


HH_Swiss_5pr <- rbind(HH_Swiss_5pr, HH_5P)

}  ################### ENDE DER SCHLAUFE ############################################




## Ende der Konstruktion des Inputfiles ##
## ---------------------------------------

HH_Swiss_5pr <- subset(HH_Swiss_5pr, HH_Swiss_5pr$householdId > 0)

head(HH_Swiss_5pr)
HH_Swiss_5pr$NR <- c(1:length(HH_Swiss_5pr$uno))

setwd("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/Sim_Input_Data")
name_file_G <- "G_Swiss_5pr.dat"
name_file <- "Swiss_5pr.dat"
write.table(Households, file = name_file_G, quote = FALSE, sep = "\t", row.names = FALSE, col.names = FALSE)
write.table(Households, file = name_file, quote = FALSE, sep = "\t", row.names = FALSE)











