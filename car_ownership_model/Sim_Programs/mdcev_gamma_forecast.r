# Goal: To read in a simple data file, and look around it's contents.

# Suppose you have a file "x.data" which looks like this:
#        1997,3.1,4
#        1998,7.2,19
#        1999,1.7,2
#        2000,1.1,13
# To read it in --

library(miscTools)
library(maxLik)

parameter_ind <- function(ivgt, ind, nc, sero, ncoeffs) {
  
  i <- 1;
  while (i <= nc) {
    j <- 1;
    while (j <= ncoeffs[i]) {
      if (i > ind) {
        ivgt <- c(ivgt, sero);
      }
      if (i < ind) {
        ivgt <- c(sero, ivgt);
      }
      j <- j+1;
    }
    i <- i+1;
  }
  
  return (ivgt);
}

gamma_forecast <- function(Data, arg_inds, arg_vars, def, fp1, ivmts, ivdts, ivgts, ivmtcs, ivdtcs, ivgtcs) {
  
  require(compiler);
  enableJIT(3);
  
  
  Halton_File <- arg_vars[[1]];
  output <- arg_vars[[2]];
  
  numout <<- arg_inds[[1]];         # Number of outside goods (i.e., always consumed goods)
  config <- arg_inds[[2]];          # Utility specification configuration, possible values: 1,4,5,6,7
  alp0to1 <<- arg_inds[[3]];        # 1 if you want the Alpha values to be constrained between 0 and 1, 0 otherwise
  # putting _alp0to1 = 1 is recommended practice and can provide estimation stability
  price <- arg_inds[[4]];           # 1 if there is price variation across goods, 0 otherwise
  nc <<- arg_inds[[5]];             # Number of alternatives (in the universal choice set) including outside goods
  po <<- arg_inds[[6]];      
  
  ivuno <- arg_inds[[7]];    #"uno" 
  ivsero <- arg_inds[[8]];   #"sero" 
  
  nrep <- arg_inds[[9]];
  gumbel <- arg_inds[[10]];
  hal_startline <- arg_inds[[11]];
  
  tolel <- arg_inds[[12]];
  tolee <- arg_inds[[13]];
  avg <- arg_inds[[14]];
  
  
  f  <- def;
  fp <- fp1;  #fp_ind(ivuno,nc);
  
  
  if (gumbel == 0) {
    nrep <- 1;
  }
  
  
  #Associating columns with variable names
  flagchm <<- def;        
  flagavm <<- fp; #ivuno~ivuno~ivuno;  //Append as many "ivuno" as the number of alternatives
  flagprcm <<- fp;
  
  
  ########################################################
  #    Do not modify the line below####################### 
  
  b_ivmtc <- NULL; #c(ivmtcs[[2]]);
  for(i in 2:nc){
    b_ivmtc <- c(b_ivmtc, ivmtcs[[i]]);
  }
  
  b_ivgtc <- c(ivgtcs[[1]]);
  for(i in 2:nc){
    b_ivgtc <- c(b_ivgtc, ivgtcs[[i]]);
  }
  
  bmdcev <- matrix(c(b_ivmtc, ivdtcs, b_ivgtc, 1.0), ncol=1);
  
  
  #### Baseline utility equation
  ivmt <- ivmts;
  
  ncoeffs <- NULL;
  for(i in 2:nc){
    ncoeffs <- c(ncoeffs, length(ivmt[[i]]));
  }
  
  tmp1 <- c(ivsero);
  for(j in 2:sum(ncoeffs)){
    tmp1 <- c(tmp1,ivsero);
  }
  ivmt[[1]] <- tmp1;

  for(i in 2:nc){
    ivmt[[i]] <- parameter_ind(ivmt[[i]], (i-1), (nc-1), ivsero, ncoeffs);
  }
  
  
  #### Satiation parameter (alpha) specification
  ncoeffs <- c(length(ivdts[[1]]));
  for(i in 2:nc){
    ncoeffs <- c(ncoeffs, length(ivdts[[i]]));
  }
  
  ivdt <- list();
  for (i in 1:nc){
    ivdt1 <- ivdts[[i]];  
    ivdt[[i]] <- parameter_ind(ivdt1, i, nc, ivsero, ncoeffs);
  }
  
  
  #### translation parameter (gamma) specification
  ncoeffs <- NULL; 
  for(i in 1:nc){
    ncoeffs <- c(ncoeffs, length(ivgts[[i]]));
  }
  
  ivgt <- list();
  for(i in 1:nc){
    ivgt1 <- ivgts[[i]];
    ivgt[[i]] <- parameter_ind(ivgt1, i, nc, ivsero, ncoeffs);
  }
  
  
  ivm <<- c(ivmt[[1]]);
  for (i in 2:(length(ivmt))){
    ivm <<- c(ivm, ivmt[[i]]);
  }
  print(ivm);
  
  ivd <<- c(ivdt[[1]]);
  for (i in 2:(length(ivdt))){
    ivd <<- c(ivd, ivdt[[i]]);
  }
  
  ivg <<- c(ivgt[[1]]);
  for (i in 2:(length(ivgt))){
    ivg <<- c(ivg, ivgt[[i]]);
  }  
  
  
  nvarm <<- length(ivmt[[1]]);      #number of variables in baseline utility   = number of columns in ivm1, do not modify this
  nvardel <<- length(ivdt[[1]]);    #number of variables in satiation          = number of columns in ivd1, do not modify this
  nvargam <<- length(ivgt[[1]]);    #number of variables in translation        = number of columns in ivg1, do not modify this

  
  if (config == 4) {
    eqmatdel <- matrix(1,nrow=1,ncol=nc);
    eqmatgam <- diag(nvargam);
  }
  if (config == 7) {
    eqmatdel <- matrix(1,nrow=1,ncol=nc);
    eqmatgam <- diag(nc);
  }  
  
 
  lpr <- function(x) {
    
    e1 <- nrow(Data);
    popass <- as.matrix(Data[,..po]);
    
    
    xdel <- t(eqmatdel)%*%x[(nvarm+1):(nvarm+nrow(eqmatdel)),];
    xgam <- t(eqmatgam)%*%x[(nvarm+nrow(eqmatdel)+1):(nvarm+nrow(eqmatdel)+nrow(eqmatgam)),];
    xsigm <- x[(nvarm+nrow(eqmatdel)+nrow(eqmatgam)+1),];
    
    a <- matrix(c(rep(1,nc)), ncol=1)%x%x[1:nvarm,];
    b <- matrix(c(rep(1,nc)), ncol=1)%x%xdel;
    c <- matrix(c(rep(1,nc)), ncol=1)%x%xgam;
    
    v2 <- sweep(Data[, ..ivm],MARGIN=2,t(a),'*');
    w2 <- sweep(Data[, ..ivd],MARGIN=2,t(b),'*');
    u2 <- sweep(Data[, ..ivg],MARGIN=2,t(c),'*');
    rm(a, b, c);
    
    
    v <- NULL;
    w <- NULL;
    u <- NULL;
    alts <- matrix(0,nrow=1,ncol=nc);
    for (j in 1:nc) {
      v <- cbind(v, as.matrix(apply(v2[,((j-1)*nvarm+1):(j*nvarm)],1,sum),ncol=1));
      w <- cbind(w, as.matrix(apply(w2[,((j-1)*nvardel+1):(j*nvardel)],1,sum),ncol=1));
      u <- cbind(u, as.matrix(apply(u2[,((j-1)*nvargam+1):(j*nvargam)],1,sum),ncol=1));
      alts[1,j] <- j;
    }
    rm(v2, w2);
    v <- exp(v);
    v <- kronecker(v,matrix(1,nrow=nrep,ncol=1));
    
    
    if (gumbel==1) {
      print("Gumbel error terms are used to simulate the unobserved heterogeneity.");
      if (!is.null(Halton_File)) {
        as <- read.table(Halton_File, header=F, sep=",");
      } else {
        as <- randtoolbox::halton(e1*nrep+hal_startline,nc)
      }
      as <- as.matrix(as[hal_startline:(e1*nrep+(hal_startline-1)),1:nc]);
      as <- -log(-log(as))*xsigm;

      #open fin1 = ^outhalt for read;
      #call seekr(fin1,((popass[1]-1)*nrep+11));
      #as = readr(fin1,nrep*e1);
      #as = as[.,1:nc];
      #as = -ln(-ln(as)).*xsigm;
      #fin1 = close(fin1);
    }
    else {
      print("no error terms are used for forecasting.");
      as <- matrix(0,nrow=(nrep*e1),ncol=nc);
    }  
    
    
    v <- v*exp(as);
    tmp <- kronecker(as.matrix(Data[,..flagprcm]),matrix(1,nrow=nrep,ncol=1));
    v <- v/tmp;
    rm(tmp);
    
    a <- 1-(1/(1+exp(w)));
    prices <- as.matrix(Data[,..flagprcm]);
    f1 <- exp(u);
    
    
    for (i in 1:e1) {
      for (j in 1:nrep) {
        fc <- matrix(0,nrow=1,ncol=nc);
        vqr <- rbind(alts,rbind(v[((i-1)*nrep+j),],rbind(prices[i,],f1[i,])));
        vqr <- as.matrix(vqr[,order(vqr[2,],decreasing = TRUE)]);
        m <- 1;
        k <- -1;
        # N <- rowSums(t(prices[i,1:numout]*((vqr[2,1:numout])^(1/(matrix(1,nrow=1,ncol=numout)-a[1,1:numout])))));
        N <- rowSums(t(vqr[4,1]*vqr[3,1]*((vqr[2,1])^(1/(matrix(1,nrow=1,ncol=1)-a[1,1])))));
        # D <- colSums(t(Data[i,flagchm]));
        D <- colSums(t(Data[i,..flagchm])) + (vqr[4,1]*vqr[3,1]);
        
        lambda <- (N/D)^(1-a[1,1]);
        
        if (is.na(lambda)) {print("Error! May need more random draws")} 
        if (vqr[2,2] < lambda) {
          fc[1,1] <- (((vqr[2,1]/lambda)^(1/(matrix(1,nrow=1,ncol=1) - a[1,1])))*vqr[4,1]) - vqr[4,1];
          fc[1,2:nc] <- matrix(0,nrow=1,ncol=(nc-1));
        }
        else {
          while (m != k){
            
            m <- m+1;
            if (m == nc) {
              fc[1,1:nc] <- (((vqr[2,1:nc]/lambda)^(1/(1-a[1,1:nc])))-matrix(1,nrow=1,ncol=nc))*(vqr[4,1:nc]);
              fc[1,1:nc] <- colSums(t(Data[i,..flagchm]))*fc[1,1:nc]/rowSums(t(fc[1,1:nc]));
              k <- m;
            }
            else if (m < nc){
              N <- N + (vqr[4,m]*vqr[3,m]*(vqr[2,m]^(1-a[1,1])));
              D <- D + (vqr[4,m]*vqr[3,m]);
              lambda <- (N/D)^(1-a[1,1]);
              
              if (vqr[2,m+1] < lambda){
                fc[1,1:m] <- (((vqr[2,1:m]/lambda)^(1/(1-a[1,1:m])))-matrix(1,nrow=1,ncol=m))*(vqr[4,1:m]);
                fc[1,(m+1):nc] <- matrix(0,nrow=1,ncol=(nc-m));
                k <- m;
                
              }
            }
            
          }
        }
        
        vqr[2,1:nc] <- fc;
        vqr <- vqr[,order(vqr[1,])];
        v[((i-1)*nrep+j),1:nc] <- vqr[2,];
      }
      
    }
    print("vqr complete")
    
    nreps = NULL;
    for (i in 1:nrep) {
      nreps = rbind(nreps, i);
    }
    print("nreps done")
    z1 <- kronecker(popass,matrix(1,nrow=nrep,ncol=1));
    
    print("kronecker 1 done")
    
    z2 <- kronecker(matrix(1,nrow=nrow(popass),ncol=1),nreps);
    
    
    print("kronecker 2 done")
    
    z <- cbind(z1,z2);
    z <- cbind(z,v);
    rm(z1,z2);
    return(z);
    
  }
  
  
  
  Forecasts <- lpr(bmdcev)
  print("lpr finished")
  nobs <- nrow(Data)
  if (avg == 1) {
    mdcev <- NULL
    for (ii in 1:nobs) {
      mdcev <- rbind(mdcev,apply((Forecasts[(ii*nrep+1):(ii*nrep+nrep),]/nrep),2,sum)) 
    }
    mdcev <- as.data.frame(mdcev);
    #print(mdcev);
    print("Done!");
  }
  if (avg == 0) {
    #print(Forecasts);
    print("Done!");
    mdcev <- Forecasts;
    mdcev <- as.data.frame(mdcev);
  }
  rm(Forecasts);
  
  names(mdcev) <- c("ID","Replication",f);
  write.table(mdcev, file = output, row.names=FALSE, sep=",");
  
  return (mdcev)
  
}



