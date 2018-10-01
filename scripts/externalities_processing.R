
library(tidyverse)
library(tidyr)
library(ggplot2)
library(zoo)
library(xtable)


emissions <- read.csv("C:\\Projects\\SCCER_project\\output_gc\\gc_emissions.csv", sep=";",
                      colClasses = c("integer", "Date", "integer", "numeric", "numeric", "numeric", 
                                     "character", 
                                     "numeric","numeric", "numeric", "numeric", "numeric", 
                                     "numeric", "numeric", "numeric", "numeric"))

congestion <- read.csv("C:\\Projects\\SCCER_project\\output_gc\\gc_congestion.csv", sep=";",
                       colClasses = c("integer", "Date", "integer", "numeric", "numeric", "numeric", 
                                      "character", 
                                      "numeric", 
                                      "numeric", 
                                      "numeric", 
                                      "numeric", 
                                      "numeric"))

externalities = cbind(emissions, delay = congestion$delay_experienced, matsim_delay = congestion$matsim_delay)

externalities$duration = (externalities$EndTime - externalities$StartTime)
externalities$avg_speed = externalities$Distance / (externalities$duration / 3.6)

save(externalities, file = "C:\\Projects\\SCCER_project\\output_gc\\gc_externalities_rdata.Rdata")




ext_filtered <- externalities  %>% filter (avg_speed < 120 & duration > 60)

ext_filtered %>% count / externalities %>% count

ext_filtered$Date_trunc <- as.Date(cut(ext_filtered$Date,  "day"))



##average speed of map matched traces
hist(ext_filtered$avg_speed, main="", xlab="speed (km/h)")

mode_agg <- ext_filtered %>% select(c(7:16)) %>% 
  mutate_all(funs(replace(., is.na(.), 0))) %>% 
  group_by(Mode) %>% summarise_all(sum) %>%
  gather(Pollutant, value, -Mode)

mode_agg %>% spread(Mode, value)

ggplot(mode_agg) + geom_bar(aes(Pollutant, weight=value, fill=Mode))

#average C02 emissions for non-car period
start_ecar_date <-  ext_filtered %>% filter(Mode == "Ecar") %>% slice(which.min(Date)) %>% select(Date)
start_ecar_date <- as.Date(start_ecar_date$Date)

per_date_count <- ext_filtered %>% group_by(Date_trunc, Mode) %>% summarize(n = sum(CO2.total., na.rm=T))


pre_ecar_records <- per_date_count %>% filter(Mode == "Car" & Date_trunc < start_ecar_date & Date_trunc > as.Date("2016-11-15"))
pre_ecar_average_co2 <- mean(pre_ecar_records$n, na.rm = TRUE)

ppost_ecar_records <- per_date_count %>% filter(Mode == "Car" & Date_trunc >= start_ecar_date)
post_ecar_average_co2 <- mean(ppost_ecar_records$n, na.rm = TRUE)
ecar_reduction_pc <- post_ecar_average_co2/pre_ecar_average_co2

per_date_count$reduction <- per_date_count$n / pre_ecar_average_co2
per_date_count$is_pre_ecar <- per_date_count$Date_trunc <= start_ecar_date

############## Table 
externalities2 <- ext_filtered %>% mutate(is_pre_ecar = Date < start_ecar_date)


mode_agg <- externalities2 %>% 
  filter(!is_pre_ecar) %>%
  select(c(7:16)) %>% 
  mutate_all(funs(replace(., is.na(.), 0))) %>% 
  transmute(
    Mode,
    'CO (kg)' = CO / 10e3,
    'CO\\textsubscript{2} (T)' = CO2.total. / 10e6,
    'FC (T)' = FC / 10e6,
    'HC (kg)' = HC / 10e3,
    'NMHC (kg)' = NMHC / 10e3,
    'NO\\textsubscript{x} (kg)' = NOx / 10e3,
    'NO\\textsubscript{2} (kg)' = NO2 / 10e3,
    'PM (kg)' = PM / 10e3,
    'SO\\textsubscript{2} (kg)' = SO2 / 10e3
    
  ) %>%
  group_by(Mode) %>% summarise_all(sum) 

# first remember the names
n <- mode_agg$Mode

# transpose all but the first column (name)
mode_agg <- as.data.frame(t(mode_agg[,-1]))
colnames(mode_agg) <- n

mode_agg$'Reduction (\\%)' = (mode_agg$Ecar / (mode_agg$Ecar + mode_agg$Car)) * 100

mode_agg

print(xtable(mode_agg),sanitize.text.function=function(x){x})



# generate break positions
breaks = c(seq(0, 1, by=0.25), ecar_reduction_pc)
# and labels
labels = as.character(breaks)


####plot of before and after Co2
ggplot(per_date_count %>% filter(Mode == "Car")) + 
  geom_bar(aes(Date_trunc, reduction, fill=is_pre_ecar), stat="identity",width=1) +
  xlab("") + ylab(expression(paste("Percentage of pre-Ecar ", CO[2]))) +
  scale_x_date(date_breaks = "1 month", date_labels =  "%b %Y" ) +
  scale_y_continuous(labels = scales::percent, breaks=breaks) +
  geom_hline(yintercept = 1) + 
  geom_hline(yintercept = ecar_reduction_pc) + 
  theme_bw() +
  theme(axis.text.x=element_text(angle=60, hjust=1, vjust=0.5), legend.position="none") +
  scale_fill_manual(values=c("#56B4E9", "#999999"))



per_week_count <- ext_filtered %>% 
  group_by(week = cut(Date_trunc, 'week'), Mode=as.factor(Mode)) %>% 
  summarize(co2=sum(CO2.total., na.rm=T), Distance = sum(Distance, na.rm=T))

per_week_distance <- ext_filtered %>% 
  group_by(week = cut(Date_trunc, 'week')) %>% summarize(Distance = sum(Distance, na.rm=T))

per_week_count$Mode <- factor(per_week_count$Mode, levels = rev(levels(per_week_count$Mode)))
####plot of before and after Co2
ggplot(per_week_count) + 
  geom_area(aes(x=as.Date(week), y=co2/ 10e3, fill=Mode), stat="identity") +
  xlab("") + ylab(expression(paste("Weekly kg of ", CO[2], " produced (grey)"))) +
  scale_x_date(date_breaks = "1 month", date_labels =  "%b %Y" ) +
  theme(axis.text.x=element_text(angle=60, hjust=1, vjust=0.5), legend.position="none") +
  scale_fill_manual(values=c("sky blue", "dark grey", "black" )) +
  geom_line(data=per_week_distance, mapping=aes(x=as.Date(week), y=Distance/max(Distance)*200)) 


min5.congestion <- ext_filtered %>% mutate(avg.delay= delay/duration) %>%
  group_by( min5 = as.integer(StartTime) / (60)) %>% summarize(n =mean(avg.delay))

####plot of before and after Co2
ggplot(min5.congestion) + 
  geom_col(aes(x = min5, y=n)) 


#######compare time lost and model:
gc_congestion 
ggplot(ext_filtered %>% filter (matsim_delay < duration)) + geom_point(aes(x=matsim_delay, y=delay))

########flag trips where trip_time free_flow_time * 2
