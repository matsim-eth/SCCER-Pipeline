
###############################################################################
##											##
## Dieses Skript generiert die Daten Tabelle welche als Input dient		##
## für die Simulation des MDCEV Flottenwahl Model für Kanton Zürich und	##
## ganze Schweiz									##
##											##	
###############################################################################

rm (list = ls())
library(foreign)

library(data.table)
library(tidyr)

homepath <- Sys.getenv("HOME")

oldwd <- getwd();
setwd("C:\\Projects\\Flottenmodel R/")
model.data.location <- ""
output.location <- "C:\\Projects\\SCCER_project\\scenarios\\output\\"
################################################################################################################################
																		##
## Hier werden Kanton und das Fuelprice Level (in CHF/Liter) was in NameSpez.R definiert wurde eingelesen.			##
## -------------------------------------------------------------------------------------------------------			##
																	##
##Run_Names <- read.table("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/R_Scripts/Spez_Names.txt", header = TRUE)	##
#Run_Names <- read.table(paste0(model.data.location, "R_Scripts/Spez_Names.txt"), sep = "", header = TRUE)	##
																		##
#KantMATS = Run_Names$KantMats		## MATSim-Kantonsname 									##
#KantVZ   = Run_Names$KantVZ			## VZ2000 Kantonsnummer 									##
Fuelprice = 1.5			## Benzinpreis in CHF/Liter									##
																		##
################################################################################################################################


## EINLESEN ##
## ---------------------------------------

Path.Pers <- paste0(output.location,"pop.csv")
Path.HH   <- paste0(output.location, "hh.csv")       ## Im Housholdfile stehen Einkommenslevel und anzahl Autos pro Haushalt drin. Generiert von Kirill Müller

#Path.Pers <- paste0(model.data.location, "Sim_Programs/pop.csv")
#Path.HH   <- paste0(model.data.location, "Sim_Programs/hh.csv")       ## Im Housholdfile stehen Einkommenslevel und anzahl Autos pro Haushalt drin. Generiert von Kirill Müller
#Path.VZ   <- paste("Projekte/OECD/matsim/Flottenmodel/MATSim_Implementation/VZ_Files/KANT_", KantVZ, ".sav", sep = "")
Path.GDS   <-paste0(model.data.location, "GemeindeDateien/GDESchluessel.sav")


Pers <- fread(Path.Pers, header = TRUE, sep = ";",integer64="character")
HH <-   fread(Path.HH, header = TRUE, sep = "\t",integer64="character")
setkey(HH, householdId)
#VZ.GD <- data.frame(read.spss(Path.VZ))
Erreich <- fread(paste0(model.data.location, "GemeindeDateien/Err_Faktorisiert.csv"), sep = ",", header=TRUE)
setnames(Erreich, "GEMNR", "ZGDE")
setkey(Erreich, ZGDE)
Erreich <- Erreich[,c("ZGDE", "F1_ErrTot", "F2_IVlastig")]
setnames(Erreich, c("F1_ErrTot", "F2_IVlastig"), c("ERF1","ERF2"))

## Gemeinde Schlüssel: von Gemeinde Nummer zu Urbci ##
## ---------------------------------------
GDS <- read.spss(Path.GDS, use.value.labels = FALSE,trim.factor.names = FALSE, to.data.frame = TRUE)
GDS <- data.table(GDS)[,lapply(.SD, mean), by=W_BFS]
names(GDS)[1]<- "ZGDE"
GDS$UrbCi <- ifelse(GDS$W_STRUKTUR_AGG == 1,1,0)
#GDS$W_BFS <- NULL
GDS$W_STRUKTUR_AGG <- NULL
setkey(GDS, ZGDE)


## Anzahl Menschen mit Fahrausweis pro Altersgruppe und Geschlecht und Menschen insgesammt pro Altersgruppe ##
## ---------------------------------------
Pers$license <- ifelse(Pers$license == "yes", "true", "false")

Pers$FA.AgeC1_M <- ifelse(Pers$sex == "m" & Pers$age > 18 & Pers$age < 46 & Pers$license == "true",1,0)
Pers$FA.AgeC1_F <- ifelse(Pers$sex == "f" & Pers$age > 18 & Pers$age < 46 & Pers$license == "true",1,0)
Pers$FA.AgeC2 <- ifelse(Pers$age > 45 & Pers$age < 66 & Pers$license == "true",1,0)
Pers$FA.AgeC3 <- ifelse(Pers$age > 65 & Pers$license == "true",1,0)

Pers$Kid <- ifelse(Pers$age <19,1,0)
Pers$AgeC1 <- ifelse(Pers$age > 18 & Pers$age < 46,1,0)
Pers$AgeC2 <- ifelse(Pers$age > 45 & Pers$age < 66,1,0)
Pers$AgeC3 <- ifelse(Pers$age > 65,1,0)

agg_cols <- c("FA.AgeC1_M", "FA.AgeC1_F", "FA.AgeC2", "FA.AgeC3", "AgeC1", "AgeC2", "AgeC3", "Kid")
Pers.aggr <- Pers[,lapply(.SD, sum), by = householdId, .SDcols=agg_cols]
setkey(Pers.aggr, householdId)
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

Households <- Pers.aggr[HH]
Households$VMT <- (Intercept + LogInc * log(Households$EINK) + AnzAutos * Households$AUTOS + AC1_M * Households$FA.AgeC1_M + AC1_F * Households$FA.AgeC1_F + AC2 * Households$FA.AgeC2 + AC3 * Households$FA.AgeC3)^3


Households$ZGDE <- as.numeric(Households$ZGDE)
setkey(Households, ZGDE)
Households <- GDS[Households]
Households <- Erreich[Households]

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

## Über die Gemeinde Nummer werden die Bundessteuern pro Kopf (= Einkommensniveau) pro Gemeinde zugespielt ##
###### --- Die Steuerdaten pro Gemeinde werden hinzugefügt ---- ###########
############################################################################

Steuerdaten <- fread(paste0(model.data.location, "GemeindeDateien/GemeindeSteuern08.csv"), sep = ";", header = TRUE)#, colClasses = c("int", "integer", "character", "integer", "integer","integer"))
Steuerdaten <- Steuerdaten[,c("GEMNR", "Tax_Capita")]
setnames(Steuerdaten, "GEMNR", "ZGDE")

setkey(Households, ZGDE)
setkey(Steuerdaten, ZGDE)
head(Steuerdaten)
head(Households)

Steuerdaten$Tax_Capita <- Steuerdaten$Tax_Capita/1000
setnames(Steuerdaten, "Tax_Capita", "TaxMun")
Households <- Steuerdaten[Households]


setcolorder(Households, c("householdId","FA.AgeC1_M","FA.AgeC1_F","FA.AgeC2","FA.AgeC3","AgeC1","AgeC2","AgeC3","Kid","ZPERS","AUSB","EINK","AUTOS","ZGDE","VMT","Income","UrbCi",	"ERF1",	"ERF2",	"TaxMun"))
setkey(Households, householdId)

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



###   Households$Zufall <- runif(length(Households$householdId))
###   HH_5P <- subset(Households, Households$Zufall <= 0.05)
###   HH_5P$Zufall <- NULL
###   HH_5P$caseId <- 1:length(HH_5P$uno)
###   hhid_Liste <- HH_5P$householdId
###   setwd("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/Sim_Input_Data")
###   write.table(HH_5P, file = "HHS_5P.dat", quote = FALSE, sep = "\t", row.names = FALSE)
###   write.table(HH_5P, file = "G_HHS_5P.dat", quote = FALSE, sep = "\t", row.names = FALSE, col.names = FALSE)
###   write.table(hhid_Liste, file = "Liste_5P.dat", quote = FALSE, sep = "\t", row.names = FALSE)





## Ende der Konstruktion des Inputfiles ##
## ---------------------------------------


Households$NR <- c(1:length(Households$uno))
name_file_G <- paste(output.location, "G_HHs_",Fuelprice,".dat", sep = "")
name_file <- paste(output.location, "HHs_",Fuelprice,".dat", sep = "")
write.table(Households, file = name_file_G, quote = FALSE, sep = "\t", row.names = FALSE, col.names = FALSE)
write.table(Households, file = name_file, quote = FALSE, sep = "\t", row.names = FALSE)

#Run MDCEV Forecast

Halton_File <- NULL #"Sim_Programs/haltonout_matsim.csv"
output <- paste0(output.location, "MDCEV_result.csv")
source("Sim_Programs/original.r")
result <- forecastMDCEV(Households, Halton_File, output)


#Collect Cars for each household
AutoListByHH <- MatchHouseholdsToAutos(result, Households)
AutoListByHH <- data.table(AutoListByHH)

getAutosforHHid <- function(hhid) {AutoListByHH %>% filter(householdId == hhid) %>% select(Erstauto,Zweitauto,Drittauto)}
padVector <- function(v, len) {c(head(v,len), rep(0, len-length(head(v,len))))}
padVector3 <- function(v) padVector(v,3)

#get the list of car users from a household (in a datatable subset), and return them as a list of exactly length 3
orderCarUsers <- function(data) { data %>% 
    filter(!Kid & car_avail != 'never') %>% 
    arrange(desc(age)) %>% select(personId) %>% t %>% 
    as.vector %>% 
    padVector3 %>%
    as.integer %>%
    as.list 
  
}

#Get the car users from the household, order them by importance (age)
Pers2 <- Pers[, c("d1","d2", "d3") := orderCarUsers(.SD), by=householdId][,.SD[1L],by=householdId, .SDcols=c("d1","d2","d3")]
#Merge Autos and ordered drivers in household
setkey(Pers2, householdId)
setkey(AutoListByHH, householdId)
DriversAndCars <- Pers2[AutoListByHH[,c('householdId','Erstauto', 'Zweitauto', 'Drittauto')]]
#Merge drivers and car columns for each of three cars
DriversAndCarsCombined <- DriversAndCars[, c("p1","p2","p3") := list(paste(d1,Erstauto), paste(d2,Zweitauto), paste(d3,Drittauto))]
#Convert to long format
DriversAndCarsLong <- melt(DriversAndCarsCombined, id.vars=c("householdId"), measure.vars=c("p1", "p2", "p3"))
#Split driver_cartype column and filter out null records and unncessary columns
DriversAndCarsLong <- DriversAndCarsLong[, c("personId", "auto") := tstrsplit(value, " ")][personId != 0 & auto != 0, -c("variable", "value")]

write.csv(DriversAndCarsLong, file=paste0(output.location, "assignedVehicleTypes.csv"), row.names=F, quote=F)
