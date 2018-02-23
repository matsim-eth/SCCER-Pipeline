

source("Sim_Programs/mdcev_gamma_forecast.r");

forecastMDCEV <- function (Households, Halton_File, output) {

  Data <- Households
  # path for the dataset that draws pesudo random Halton draws to generate the gumbel error terms
  # If you do not have the matrix, you can comment out this line and write code to generate random numbers on the fly
  # alternatively, you can uncomment this line and set _gumbel = 0 to avoid considering the unobserved heterogeneity

  ivuno <- "uno";         # Column of ones
  ivsero <- "sero";       # Column of zeros
  
  numout <- 0;      # Number of outside goods (i.e., always consumed goods)
  config <- 4;      # Utility specification configuration, possible values: 4, 7
  alp0to1 <<- 1;    # 1 if you want the Alpha values to be constrained between 0 and 1, 0 otherwise
  price <- 0;       # 1 if there is price variation across goods, 0 otherwise
  nc <<- 16;         # Number of alternatives (in the universal choice set) including outside goods
  po <<- ncol(Data);         # position of pointer to case number in data set,
  
  nrep <- 10;        # Number of sets of error term Halton draws overwhich you want to simulate the unobserved heterogeneity
  
  # 1 if Gumbel error terms are used to simulate the unobserved heterogeneity (i.e., the error terms), 
  # 0 if no error terms are used for forecasting.
  # if _gumbel = 0, then set nrep = 1.
  gumbel <- 1;
  halton_startrow <- 22; # Number of row in dataset of pesudo random halton. It will read halton data from the row number
  
  tolel <- 0.00000001;
  tolee <- 0.01;
  avg <- 0;
  
  
  
  # Provide labels of dependent variables (i.e., the expenditure variables) below
  dep.variable.names <- c("D_00","D_01","D_02","D_03","D_04","D_05","D_06","B_00","B_01","B_02","B_03","B_04","B_05","B_06","B_07","U_00")
  def <- dep.variable.names # c("consume1","consume2","consume3");
  
  # Provide labels of price variables below; if no price variables, introduce UNO as the variable
  fp <- c(ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno, ivuno);
  
  bmdcev <- c( 1.2088     	# Kosntante für D1 Subcompact Diesel  */
               ,    0.65	#Kosntante für D2 Compact Diesel  */
               ,    0.35		# Kosntante für D3 MiniMVP Diesel  */
               ,    -1.1003	# Kosntante für D4 MidSized Diesel  */
               ,    -0.2		# Kosntante für D5 FullSized Diesel  */
               ,    -0.8183	# Kosntante für D6 LuxusSport Diesel  */
               ,    1.75		# Kosntante für B0 Micro Benzin  */
               ,    2.5		# Kosntante für B1 Subcompact Benzin  */
               ,    2.5		# Kosntante für B2 Compact Benzin  */
               ,    0.15		# Kosntante für B3 MiniMVP Benzin  */
               ,    2.1		# Kosntante für B4 MidSized Benzin  */
               ,    0.7		# Kosntante für B5 FullSized Benzin  */
               ,    0.3		# Kosntante für B6 Luxus Benzin  */
               ,    1.43	 	# Kosntante für B7 Sport Benzin  */
               ,    1.55		# Kosntante für U0 Alternative Technologie  */
               ,    -0.0562,  0.5047,  0.3038,  -0.0658,  0.1736,  -0.9012,  -0.2052,  -0.0864,  0.2176,  0.3935
               ,  -0.0786,  0.0054,  0.4256,  0.7333,  -0.4658,  0.5564,  -0.7203,  0.2559,  -0.0528,  0.1443
               ,  0.049,  -0.0754,  -0.1005,  0.6121,  1.118,  -0.188,  0.24,  -0.9263,  0.4036,  -0.2487,  0.2356
               ,  0.6407,  -0.1201,  0.1577,  1.0126,  0.6302,  -0.3635,  0.2372,  -0.1812,  0.1015,  -0.1842
               ,  0.0616,  0.172,  -0.1811,  0.1244,  -0.0833,  -0.0239,  0.1508,  0.6914,  -0.7357,  0.1341
               ,  -0.1211,  0.1479,  0.7481,  -0.0231,  0.1157,  0.7941,  -0.2576,  -0.4265,  0.0943,  0.0421
               ,  0.4377,  -0.2331,  0.3296,  0.4403,  -0.125,  0.0041,  -0.1211,  0.612,  0.3299,  0.1235
               ,  -0.1829,  0.2676,  -0.2898,  0.1749,  0.2552,  -0.1612,  -0.2551,  0.3942,  0.3979,  0.9111
               ,  0.4706,  0.3064,  -0.2025,  0.2145,  -0.0641,  -0.0602,  -0.3336,  0.4407,  0.0317,  0.745
               ,  0.0302,  0.2798,  -0.2786,  0.2199,  0.4589,  -0.0313,  -0.4615,  1.1762,  0.9877,  1.2463
               ,  1.2,  0.9223,  -0.1792,  0.2278,  -0.0924,  0.0031,  -0.3364,  0.532,  0.3206,  0.5648,  0.1387
               ,  0.5841,  -0.2163,  0.259,  0.2626,  0.0246,  -0.5024,  0.5995,  0.8996,  0.8337,  0.1436,  0.2191
               ,  -0.0943,  0.2487,  0.5756,  0.14,  -0.4568,  0.5256,  0.6855,  0.9067,  -0.4219,  -0.1378,  -0.1381
               ,  0.3404,  0.3864,  0.1569,  -0.3841,  0.411,  -0.1945,  -0.0721,  -1.2895,  0.2879,  -0.1274,  0.1637
               ,  0.3024,  0.0173,  0.1586,  0.5467,  -0.8016,  -0.1309,  -0.6394,  0.536,  -0.2082,  0.3044,  0.3974
               ,  -1000
               ,  0.9984,  4.5902,  4.2215,  7.4314,  0.9339,  1.9938,  -1.0573,  1.1465,  2.3902,  3.3273
               ,  1.799,  3.7623,  3.5066,  3.4412,  0.8616,  0.7552,  2.2577,  -0.3154,  -0.1512,  -0.5748,  0.0638
               ,  -0.0093,  0.4062,  -0.1024,  -0.1489,  -0.1781,  -0.0754,  -0.1806,  -0.1914,  -0.2152,  -0.0587,  2.8332
               ,  1.0);
  
  
  # definition of independent variables 
  # ivm[[1]] has variables that influence baseline preference parameter psi, ivd[[1]] has variables that influence the satiation parameter delta, 
  # and ivg[[1]] has variabels that influence the translation parameter gamma; 
  ivmts <- list();
  ivmtcs <- list();
  
  ivmts[[1]] <- c("");     # Do not modify this line because the first alternative is considered as base     
  ivmtcs[[1]] <- c(0.0);   # Do not modify this line because the first alternative is considered as base
  
  wColumns <- c("uno","Income",	"Fuel",	"Kid",	"AgeC1",	"AgeC2",	"AgeC3",	"UrbCi",	"ERF1",	"ERF2",	"TaxMun",	"Fuel2")
  
  #get coefficients from gauss list, little bit complicated, as the list is kostants:coeefs(l to r):xdel:xgamma(x32):xsigma
  i_s = nc
  for (i in 2:nc) {
    columns <- if (i <= 7) wColumns else head(wColumns,-1)
    num.coeffs <- length(columns) -1
    ivmts[[i]] <- columns
    
    konstant <- bmdcev[i-1]
  
    i_e <- i_s + num.coeffs -1
    b <- bmdcev[i_s: i_e]
    ivmtcs[[i]] <- c (konstant, b)
    print (ivmts[[i]])
    print (ivmtcs[[i]])
    i_s <- i_s + num.coeffs
    
  }
  
  # Important Note: For the satiation parameters (alphas and gammas) do not provide the final values of alphas and gammas. 
  # Provide the values of the parameters that are actually estimated in the model. 
  # For example, gamma is parameterized as exp(theeta), and theeta is estimated. So provide the theeta values here.
  # Simialrly, Alpha is parameterized as 1-(1/exp(delta)). Provide the delta values here.
  ivdts <- list();
  
  for (i in 1:nc) {
    ivdts[[i]] <- c("uno");
  }
  
  # The alpha values for all alternatives are restricted to -1000.
  ivdtcs <- c(-1000.0); 
  
     
  ivgts <- list();
  ivgtcs <- list();
  
  start.gamma.coeffs <- length(unlist(ivmtcs)) + 1 # add one for the delta coeff that we skip
  #gamma parameters are in order uno;uno;......; uno; income;income..... etc
  for (i in 1:nc) {
    ivgts[[i]] <- c("uno", "Income");
    uno.gamma.index <- start.gamma.coeffs + (i-1)
    income.gamma.index <- uno.gamma.index + nc
    ivgtcs[[i]] <- bmdcev[c(uno.gamma.index,income.gamma.index)]
    print (ivgtcs[[i]])
    
  }
  
  
  ########################################################################################################
  #  Do Not Modify Next Three Lines
  ########################################################################################################
  arg_inds <- list(numout, config, alp0to1, price, nc, po, ivuno, ivsero, nrep, gumbel, halton_startrow, tolel, tolee, avg);
  arg_vars <- list(Halton_File, output);
  fp1 <- fp
  
  result <- gamma_forecast(Data, arg_inds, arg_vars, def, fp, ivmts, ivdts, ivgts, ivmtcs, ivdtcs, ivgtcs);
  ########################################################################################################

  return(result)
}


MatchHouseholdsToAutos <- function(Res, hhs) {     
  
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
  
  
  Res.mean$householdId <- hhs$householdId
  Res.mean$Tot <- Res.mean$D_00 + Res.mean$D_01 + Res.mean$D_02 + Res.mean$D_03 + Res.mean$D_04 + Res.mean$D_05 + Res.mean$D_06 + Res.mean$B_00 + Res.mean$B_01 + Res.mean$B_02 + Res.mean$B_03 + Res.mean$B_04 + Res.mean$B_05 + Res.mean$B_06 + Res.mean$B_07 + Res.mean$U_00
  Res.mean$AnzAutos <- hhs$AUTOS
  Res.mean$Test <- round(hhs$VMT - Res.mean$Tot * 10000, digits = 3)
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
