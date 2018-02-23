
setwd("P:/Projekte/OECD/matsim/Flottenmodel R/MATSim_Implementation/")

pp <- read.csv2("pop.csv") %>% mutate(householdId = as.character (householdId))
hh <- read.csv("hh.csv", sep = "\t") %>% mutate(HHNR = as.character (HHNR))

vehicles <- read.csv("Results_Lists/output.txt", sep = "\t") %>% 
  mutate(householdId = as.character (householdId)) %>%
  mutate(householdId = as.character (householdId)) %>%
  mutate(AnzAutos = AnzAutos + (AnzAutos == 0)) #if number of cars is zero, set to 1

   
   
vehicles$AnzAutos %>% sum()
  
head(c("Erstauto", "Zweitauto", "Drittauto"), 1)

getveh <- function (x) {
  hid = x
  if (any(vehicles$householdId == hid )) {
    num_cols <- vehicles[vehicles$householdId == hid,"AnzAutos"] #get the number of cars in the household
    hh_car_cols <- c(head(c("Erstauto", "Zweitauto", "Drittauto"), num_cols)) 
    
    #print (vehicles[vehicles$householdId == hid,hh_car_cols])
    car <- sample(vehicles[vehicles$householdId == hid,hh_car_cols], 1) #sample one car from the household cars
    if (num_cols > 1) { #if there was more than one car, then we need to select the chosen one from the dataframe
      car <- car[1,]
    }
    return (as.character(car)) #convert from factor to string
  } else {
    return (NA)
  }
  
}
getveh(1000011000)
getveh(4068000027100)
b <- getveh(2000001801)
b
a <- getveh(pp[984,]$householdId)
a


getveh(pp[984,])
pp$vehicle = apply(pp["householdId"],1, getveh)
pp
  
hid = 1000011000
hh_car_cols <- head(c("Erstauto", "Zweitauto", "Drittauto"), vehicles[vehicles$householdId == hid,"AnzAutos"])
as.character(vehicles[vehicles$householdId == hid,hh_car_cols])
