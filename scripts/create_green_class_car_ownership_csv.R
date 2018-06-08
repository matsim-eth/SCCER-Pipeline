library(dplyr)

load("C:\\Projects\\SCCER_project\\Survey_Data_Joe.RData")

car_cols <- grep("car_\\d", colnames(survey_data))

filtered_data <- survey_data %>% select(id=`DailyTracks ID`, HHcars, drivlic, availability_car, car_cols)

write.csv(filtered_data, "C:\\Projects\\SCCER_project\\green_class_car_ownership.csv", na = "", row.names = F,fileEncoding = "UTF-8")
