
#############################################################################################
##													##
## Dieses Skript bereitet die Daten aus dem Forecast auf und macht eine List			##
## auf der für jeden Haushalt des Kantons der Autotyp des erst, zweit und drittwagen	##
## und die dazugehörigen vmt (vehicle miles travelled = jaehrliche fahrleistung)		##
## eingetragen ist.											##	
#############################################################################################

library(foreign)

Res <- read.csv("P:/Projekte/OECD/matsim/Flottenmodel R/MATSim_Implementation/Sim_Output_Data/HHs_1pc_1.75_gamma.csv", header = TRUE)
Households <- read.table("P:/Projekte/OECD/matsim/Flottenmodel R/MATSim_Implementation/Sim_Input_Data/HHs_1pc_1.75.dat",header = TRUE)
name_file <- "P:/Projekte/OECD/matsim/Flottenmodel R/MATSim_Implementation/Results_Lists/output.txt"



MatchHouseholdsToAutos <- function(Res, householdIds) {     
  
  names(Res)[1] <- "NR"
  names(Res)[2] <- "Draw"
  names(Res)[3] <- "D_00"
  names(Res)[4] <- "D_01"
  names(Res)[5] <- "D_02"
  names(Res)[6] <- "D_03"
  names(Res)[7] <- "D_04"
  names(Res)[8] <- "D_05"
  names(Res)[9] <- "D_06"
  names(Res)[10] <- "B_00"
  names(Res)[11] <- "B_01"
  names(Res)[12] <- "B_02"
  names(Res)[13] <- "B_03"
  names(Res)[14] <- "B_04"
  names(Res)[15] <- "B_05"
  
  names(Res)[16] <- "B_06"
  names(Res)[17] <- "B_07"
  names(Res)[18] <- "U_00"
  
  
  
  b <- rep(c(1:(length(Res$NR)/10)), each=10)
  Res.mean <- aggregate(Res, by=list(b), FUN = mean)
  
  
  Res.mean$householdId <- Households$householdId
  Res.mean$Tot <- Res.mean$D_00 + Res.mean$D_01 + Res.mean$D_02 + Res.mean$D_03 + Res.mean$D_04 + Res.mean$D_05 + Res.mean$D_06 + Res.mean$B_00 + Res.mean$B_01 + Res.mean$B_02 + Res.mean$B_03 + Res.mean$B_04 + Res.mean$B_05 + Res.mean$B_06 + Res.mean$B_07 + Res.mean$U_00
  Res.mean$AnzAutos <- Households$AUTOS
  Res.mean$Test <- round(Households$VMT - Res.mean$Tot * 10000, digits = 3)
  max(Res.mean$Test)
  length(Res.mean[Res.mean$Test > 0.1,])
  
  
  
  Res.mean$Max <- apply(Res.mean[,4:19],1,max)
  
  Res.mean$Erstauto <-  names(Res.mean)[max.col(Res.mean[,4:19])+3]
  
  for (j in 4:19){
  	Res.mean[,j] <- ifelse(Res.mean[,j] == Res.mean$Max,-1,Res.mean[,j])
  	}
  
  Res.mean$Zweitauto <- ifelse(Res.mean$AnzAutos > 1, names(Res.mean)[max.col(Res.mean[,4:19])+3],0)
  Res.mean$Second <- apply(Res.mean[,4:19],1,max)
  Res.mean$Zweitauto <- ifelse(Res.mean$Second == 0 & Res.mean$AnzAutos > 1, Res.mean$Erstauto, Res.mean$Zweitauto)
  
  
  for (j in 4:19){
  	Res.mean[,j] <- ifelse(Res.mean[,j] == Res.mean$Second & Res.mean$Second > 0 ,-2,Res.mean[,j])
  	}
  Res.mean$Drittauto <- ifelse(Res.mean$AnzAutos > 2, names(Res.mean)[max.col(Res.mean[,4:19])+3],0)
  Res.mean$Third <- apply(Res.mean[,4:19],1,max)
  Res.mean$Drittauto <- ifelse(Res.mean$Third == 0 & Res.mean$AnzAutos > 2, Res.mean$Erstauto,Res.mean$Drittauto)
  
  
  for (j in 4:19){
  	Res.mean[,j] <- ifelse(Res.mean[,j] == -2,Res.mean$Second,Res.mean[,j])
  	Res.mean[,j] <- ifelse(Res.mean[,j] == -1,Res.mean$Max,Res.mean[,j])
  	}
  
  Res.mean$VMT_Erst <- 0
  Res.mean$VMT_Zweit <- 0
  Res.mean$VMT_Dritt <- 0
  Res.mean$VMT_Erst <- ifelse(Res.mean$AnzAutos <= 1,Res.mean$Tot * 10000,Res.mean$VMT_Erst)
  
  Res.mean$VMT_Erst <- ifelse(Res.mean$AnzAutos == 2,Res.mean$Tot * 10000 *(Res.mean$Max/(Res.mean$Max + Res.mean$Second)),Res.mean$VMT_Erst)
  Res.mean$VMT_Zweit <- ifelse(Res.mean$AnzAutos == 2,Res.mean$Tot * 10000 *(Res.mean$Second/(Res.mean$Max + Res.mean$Second)),Res.mean$VMT_Zweit)
  
  Res.mean$VMT_Erst <- ifelse(Res.mean$AnzAutos == 3,Res.mean$Tot * 10000 *(Res.mean$Max/(Res.mean$Max + Res.mean$Second + Res.mean$Third)),Res.mean$VMT_Erst)
  Res.mean$VMT_Zweit <- ifelse(Res.mean$AnzAutos == 3,Res.mean$Tot * 10000 *(Res.mean$Second/(Res.mean$Max + Res.mean$Second + Res.mean$Third)),Res.mean$VMT_Zweit)
  Res.mean$VMT_Dritt <- ifelse(Res.mean$AnzAutos == 3,Res.mean$Tot * 10000 *(Res.mean$Third/(Res.mean$Max + Res.mean$Second + Res.mean$Third)),Res.mean$VMT_Dritt)
  
  Res.mean$Max <- NULL
  Res.mean$Second <- NULL
  Res.mean$Third <- NULL
  

	Res.mean$VMT_Zweit <- ifelse(Res.mean$Erstauto == Res.mean$Zweitauto,Res.mean$VMT_Erst / 2,Res.mean$VMT_Zweit)		
	Res.mean$VMT_Erst  <- ifelse(Res.mean$Erstauto == Res.mean$Zweitauto,Res.mean$VMT_Erst / 2,Res.mean$VMT_Erst)		

	Res.mean$VMT_Dritt <- ifelse(Res.mean$Erstauto == Res.mean$Drittauto,Res.mean$VMT_Erst / 2,Res.mean$VMT_Dritt)		
	Res.mean$VMT_Erst  <- ifelse(Res.mean$Erstauto == Res.mean$Drittauto,Res.mean$VMT_Erst / 2,Res.mean$VMT_Erst)		

	Res.mean$VMT_Dritt <- ifelse(Res.mean$Erstauto == Res.mean$Drittauto & Res.mean$Erstauto == Res.mean$Zweitauto,Res.mean$Tot * 10000 / 3,Res.mean$VMT_Dritt)		
	Res.mean$VMT_Zweit <- ifelse(Res.mean$Erstauto == Res.mean$Drittauto & Res.mean$Erstauto == Res.mean$Zweitauto,Res.mean$Tot * 10000 / 3,Res.mean$VMT_Zweit)		
	Res.mean$VMT_Erst  <- ifelse(Res.mean$Erstauto == Res.mean$Drittauto & Res.mean$Erstauto == Res.mean$Zweitauto,Res.mean$Tot * 10000 / 3,Res.mean$VMT_Erst)		

	Ausgabe <- subset(Res.mean, select = c("NR", "householdId", "AnzAutos", "Erstauto", "Zweitauto","Drittauto", "VMT_Erst", "VMT_Zweit", "VMT_Dritt"))
	
  return (Ausgabe)
}

##  head(Res.mean)
Car_owners <- subset(Res.mean, Res.mean$AnzAutos > 0) 
Stats <- data.frame("Typ" = names(Res.mean)[4:19], "Anzahl" = 0, "T.Anzahl" = 0)
for (j in 1:length(Stats$T.Anzahl)){
Stats$T.Anzahl[j] <- length(Car_owners$NR[Car_owners$Erstauto==Stats$Typ[j]]) + length(Car_owners$NR[Car_owners$Zweitauto==Stats$Typ[j]]) + length(Car_owners$NR[Car_owners$Drittauto==Stats$Typ[j]])
}


Stats$Anzahl = Stats$Anzahl + Stats$T.Anzahl

Stats$Prozent <- round(Stats$Anzahl / sum(Stats$Anzahl) *100, digits = 2)
Stats$Ziel <- 0
Stats$Ziel[1] <- 3.70 * 0.1
Stats$Ziel[2] <- 19.00 * 0.1 
Stats$Ziel[3] <- 23.10 * 0.1
Stats$Ziel[4] <- 14.00 * 0.1
Stats$Ziel[5] <- 22.30 * 0.1
Stats$Ziel[6] <-  8.9 * 0.1
Stats$Ziel[7] <- (6.30 + 2.60) * 0.1
Stats$Ziel[8] <- 3.70 * 0.9
Stats$Ziel[9] <- 19.00 * 0.9 
Stats$Ziel[10] <- 23.10 * 0.9
Stats$Ziel[11] <- 14.00 * 0.9
Stats$Ziel[12] <- 22.30 * 0.9
Stats$Ziel[13] <-  8.9 * 0.9
Stats$Ziel[14] <- 6.30 * 0.9
Stats$Ziel[15] <- 2.60 * 0.9
Stats$Ziel[16] <- 0.4


Stats


Ausgabe <- subset(Res.mean, select = c("NR", "householdId", "AnzAutos", "Erstauto", "Zweitauto","Drittauto", "VMT_Erst", "VMT_Zweit", "VMT_Dritt"))



##setwd("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/Results_Lists")
##setwd(paste(homepath, "/P/Projekte/matsim/Flottenmodel/MATSim_Implementation/Results_Lists", sep = ""))

write.table(Ausgabe, file = name_file, quote = FALSE, sep = "\t", row.names = FALSE)
write.table(Stats, file = "LastStats", quote = FALSE, sep = "\t", row.names = FALSE)





