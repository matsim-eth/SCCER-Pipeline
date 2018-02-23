/* Efficient Forecasting Code with the MDCEV Model */
/* This code works for the case when there is NO outside good, and for the alpha profile utility function (i.e., when the alpha-sub-k parameters vary across alternatives) */
/* Reference: Pinjari and Bhat (2010) An Efficient Foreacsting Procedure for Kuhn-Tucker Consumer Demand Model Systems. */
/* This code does not consider random coefficients, but it is easy to extend it to do so */

print "Start with Program";
/* maxset; */

    gausset;
    _max_Options = 0;
    _max_Algorithm = 2;
    _max_LineSearch = 2;
    _max_CovPar = 1;
    _max_GradMethod = 1;
    _max_ParNames = 0;     /* parameter names */
    _max_GradTol = 1e-5;   /* convergence tolerance for gradient */
    _max_HessProc = 0;     /* procedure to compute hessian */
    _max_GradProc = 0;     /* procedure to compute gradient */
    _max_MaxIters = 1e+5;  /* maximum number of iterations */
    _max_IterData = {0,0,0}; /* elapsed time, # of iters, cov method */
    _max_RandRadius = .01; /* random line search */
    _max_UserSearch = 0;   /* interactive line search */
    _max_UserNumGrad = 0;
    _max_UserNumHess = 0;
    _max_Extrap = 2.0;   /* extrapolation constant */
    _max_interp = 0.25;  /* interpolation constant */
    _max_Delta = .1;
    _max_MaxTry = 100;
    _max_Lag = 0;         /* number of lags in model */
    _max_FinalHess = 0;
    _max_NumObs = 0;      /* number of observations */
    _max_HessCov = {.};     /* info matrix v-c matrix of par's */
    _max_XprodCov = {.};    /* cross proc matrix v-c matrix of par's  */
    _max_key = 1;
    _max_MaxTime = 1e5;   /* maximum time for bootstrapping */
    _max_Active = 1;
    _max_GradStep = {.};      /* size of increment for computing gradient */
    _max_GradCheckTol = 0;
    _max_Diagnostic = {.};
    _max_Alpha = .05;
    _max_Switch = 0;

    _max_BayesAlpha = 1.4;
    _max_PriorProc = {.};
    _max_CrossTab = { . };  /* crosstab of coef's output from cvhist */
    _max_CutPoint = { . };  /* cutting points for _max_xtab */
    _max_Increment = 0;     /* if nonzero, histogram increments */
    _max_Center = 0;        /* if nonzero, center points for histogram */
    _max_Width = 2;         /* width of histogram = _max_Width * sd's */
    _max_NumSample = 50;   /* bootstrap sample size */
    _max_NumCat = 16;       /* # of cat's for bootstrapped histogram */
    _max_Select = {.};

    _max_dsn = "";
    _max_dat = { . };
    _max_row = 0;


/*****************************************************************************
                  Global Variable Definition and Settings
*****************************************************************************/
clearg z,p,_dd,datatset,_outa,_dmsave,indxrc,rr,constr,eqmat,_unity,kk,seqq,nrcoef,nrep,bmdcev;
clearg nobs,_dd,_dataset,nc,nvar,nind,nrep,_outa,nrcoef,indxrc,_rcoef,_rcofnlos,lst,first,nlos,ndimp,ndim;
clearg qr,prec,err,stt;

/* __row    = 1;     // Number of rows to be read at a time by the log-likelihood function  */
nobs     = 1000;   /*  Number of observations for which you want to do the simulation  */
numout   = 0;       /*  Number of outside goods (i.e., always consumed goods)  */
_config  = 4;       /*  Utility specification configuration, possible values: 4,7  */
_alp0to1 = 1;       /*  1 if you want the Alpha values to be constrained between 0 and 1, 0 otherwise  */
_price   = 0;       /*  1 if there is price variation across goods, 0 otherwise  */
nc       = 16;       /*  Number of alternatives (in the universal choice set) including outside goods  */
_gumbel  = 1;       /*  1 if Gumbel error terms are used to simulate the unobserved heterogeneity (i.e., the error terms),   */
                    /* 0 if no error terms are used for forecasting.  */
                    /*  if _gumbel = 0, then set nrep = 1 below.  */

home = envget ("HOME");					

rndseed 373313211;	
					
nrep     = 10;       /*  Number of sets of error term Halton draws overwhich you want to simulate the unobserved heterogeneity  */
if (_gumbel  ==0);
  nrep = 1;
endif;


outhalt = "C:/gauss9.0/Data/simdata/haltbrat"; /*  pathru for the dataset that draws pesudo random Halton draws to generate the gumbel error terms  */
                                               /*  If you do not have the matrix, you can comment out this line and write code to generate random numbers on the fly  */
                                               /*  alternatively, you can uncomment this line and set _gumbel = 0 to avoid considering the unobserved heterogeneity  */

/* Forecasts are printed for each nrep for each individual. 
  Total number of rows printed = nrep*nobs (nrep number of rows for each observation)
  Number of columns printed = nc+2. 
        First column corresponds to the observation (or individual) number, 
        second column corresponds to the number of error draws set,
        The remianing columns correspond to the predicted CONSUMPTIONS for each alternative
  If you want the expenditures, simply multiply the consumptions by the corresponding unit prices
*/ 


/* --------------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------------
 
		Hier wird der Name der gewünschten INPUT Datei eingfügt vom Batchfile:									*/

dataset = home $+ "/Temp_Input.dat";

/*---------------------------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------------------------- 

##############################################################################################################################

-----------------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------------
output_path = home 	$+ "/Temp_Output.asc";
		Hier wird der Name der gewünschten OUTPUT Datei eingfügt vom Batchfile:								*/

output file = Temp_Output.asc reset;



/*---------------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------------*/ 



{ pointer,_po } = indices(dataset,"caseid"); /*  position of pointer to case number in data set,   */

/* { vnam,mean,var,std,min,max,valid,mis } = dstat(dataset,0);   uncomment this line if you want to take descriptive statistics of the dasaset  */

outwidth 256;
screen off;      /*    //uncomment this line if you do not want the outputs to be printed on the screen.  */

/* position of UNO variable in data set */
{ unov,ivuno } = indices(dataset,"uno");


/*****************************************************************************
                    Specification of variables area, similar to the variables specifiaction area in the model estimation code
*****************************************************************************/

/* Provide labels of dependent variables (i.e., the expenditure variables) below; */
{ choicm,f } = indices(dataset,"D_00"|"D_01"|"D_02"|"D_03"|"D_04"|"D_05"|"D_06"|"B_00"|"B_01"|"B_02"|"B_03"|"B_04"|"B_05"|"B_06"|"B_07"|"U_00"); 

/* definition of independent variables */
/* ivm1 has variables that influence baseline preference parameter psi, ivd1 has variables that influence the satiation parameter delta, and ivg1 has variabels that influence
   the translation parameter gamma; first numout goods are those always consumed, and first good is numeraire  */
/* Since gamma=0 for the outside goods, the first numout columns of the ivg vectors will be uno or sero, with no other independent variables in these first numout colums */

let	ivm1 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm2 = {	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	D_01_Act	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm3 = {	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	D_02_Act	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm4 = {	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	D_03_Act	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm5 = {	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	D_04_Act	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm6 = {	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	D_05_Act	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm7 = {	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	D_06_Act	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm8 = {	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_00_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm9 = {	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_01_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm10 = {	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_02_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm11 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_03_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm12 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_04_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm13 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_05_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm14 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_06_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm15 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	B_07_Act	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm16 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	U_00_Act	};

let ivd1  = { UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd2  = { sero 	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd3  = { sero 	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd4  = { sero 	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd5  = { sero 	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd6  = { sero 	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd7  = { sero 	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd8  = { sero 	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivd9  = { sero 	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	};
let ivd10 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	};
let ivd11 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	};
let ivd12 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	};
let ivd13 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	};
let ivd14 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	};
let ivd15 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	};
let ivd16 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	};


let ivg1  = { UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivg2  = { sero 	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivg3  = { sero 	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero    };
let ivg4  = { sero 	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivg5  = { sero 	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivg6  = { sero 	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivg7  = { sero 	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero 	sero	Income	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivg8  = { sero 	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero 	sero	sero	Income	sero	sero	sero	sero	sero	sero	sero	sero	};
let ivg9  = { sero 	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	sero	sero	sero	sero	sero	sero	sero	};
let ivg10 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero 	sero	sero	sero	sero	Income	sero	sero	sero	sero	sero	sero	};
let ivg11 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero 	sero	sero	sero	sero	sero	Income	sero	sero	sero	sero	sero	};
let ivg12 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero 	sero	sero	sero	sero	sero	sero	Income	sero	sero	sero	sero	};
let ivg13 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero 	sero	sero	sero	sero	sero	sero	sero	Income	sero	sero	sero	};
let ivg14 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero 	sero	sero	sero	sero	sero	sero	sero	sero	Income	sero	sero	};
let ivg15 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	sero	};
let ivg16 = { sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero 	sero	sero	sero	sero	sero 	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	};



/* Provide labels of price variables below; if no price variables, introduce UNO as the variable */
{ cprice,fp } = indices(dataset,"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno"|"uno");


/* associating columns with variable names */
flagchm = f';
flagavm = ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno~ivuno;
flagprcm = fp';

{ v1,ivmt1 } = indices(dataset,ivm1');
{ v2,ivmt2 } = indices(dataset,ivm2');
{ v3,ivmt3 } = indices(dataset,ivm3');
{ v4,ivmt4 } = indices(dataset,ivm4');
{ v5,ivmt5 } = indices(dataset,ivm5');
{ v6,ivmt6 } = indices(dataset,ivm6');
{ v7,ivmt7 } = indices(dataset,ivm7');
{ v8,ivmt8 } = indices(dataset,ivm8');
{ v9,ivmt9 } = indices(dataset,ivm9');
{ v10,ivmt10 } = indices(dataset,ivm10');
{ v11,ivmt11 } = indices(dataset,ivm11');
{ v12,ivmt12 } = indices(dataset,ivm12');
{ v13,ivmt13 } = indices(dataset,ivm13');
{ v14,ivmt14 } = indices(dataset,ivm14');
{ v15,ivmt15 } = indices(dataset,ivm15');
{ v16,ivmt16 } = indices(dataset,ivm16');


{ w1,ivdt1 } = indices(dataset,ivd1');
{ w2,ivdt2 } = indices(dataset,ivd2');
{ w3,ivdt3 } = indices(dataset,ivd3');
{ w4,ivdt4 } = indices(dataset,ivd4');
{ w5,ivdt5 } = indices(dataset,ivd5');
{ w6,ivdt6 } = indices(dataset,ivd6');
{ w7,ivdt7 } = indices(dataset,ivd7');
{ w8,ivdt8 } = indices(dataset,ivd8');
{ w9,ivdt9 } = indices(dataset,ivd9');
{ w10,ivdt10 } = indices(dataset,ivd10');
{ w11,ivdt11 } = indices(dataset,ivd11');
{ w12,ivdt12 } = indices(dataset,ivd12');
{ w13,ivdt13 } = indices(dataset,ivd13');
{ w14,ivdt14 } = indices(dataset,ivd14');
{ w15,ivdt15 } = indices(dataset,ivd15');
{ w16,ivdt16 } = indices(dataset,ivd16');


{ u1,ivgt1 } = indices(dataset,ivg1');
{ u2,ivgt2 } = indices(dataset,ivg2');
{ u3,ivgt3 } = indices(dataset,ivg3');
{ u4,ivgt4 } = indices(dataset,ivg4');
{ u5,ivgt5 } = indices(dataset,ivg5');
{ u6,ivgt6 } = indices(dataset,ivg6');
{ u7,ivgt7 } = indices(dataset,ivg7');
{ u8,ivgt8 } = indices(dataset,ivg8');
{ u9,ivgt9 } = indices(dataset,ivg9');
{ u10,ivgt10 } = indices(dataset,ivg10');
{ u11,ivgt11 } = indices(dataset,ivg11');
{ u12,ivgt12 } = indices(dataset,ivg12');
{ u13,ivgt13 } = indices(dataset,ivg13');
{ u14,ivgt14 } = indices(dataset,ivg14');
{ u15,ivgt15 } = indices(dataset,ivg15');
{ u16,ivgt16 } = indices(dataset,ivg16');


ivm = ivmt1'~ivmt2'~ivmt3'~ivmt4'~ivmt5'~ivmt6'~ivmt7'~ivmt8'~ivmt9'~ivmt10'~ivmt11'~ivmt12'~ivmt13'~ivmt14'~ivmt15'~ivmt16';
ivd = ivdt1'~ivdt2'~ivdt3'~ivdt4'~ivdt5'~ivdt6'~ivdt7'~ivdt8'~ivdt9'~ivdt10'~ivdt11'~ivdt12'~ivdt13'~ivdt14'~ivdt15'~ivdt16';
ivg = ivgt1'~ivgt2'~ivgt3'~ivgt4'~ivgt5'~ivgt6'~ivgt7'~ivgt8'~ivgt9'~ivgt10'~ivgt11'~ivgt12'~ivgt13'~ivgt14'~ivgt15'~ivgt16';
				

nvarm = cols(ivm1);
nvardel = cols(ivd1);
nvargam = cols(ivg1);

if _config==4;
     EQMATDEL = ones(1,nc);
     eqmatgam = eye(nvargam);
elseif _config==7;
     EQMATDEL = ones(1,nc);
     eqmatgam = eye(nc);
endif;

/*  provide the MDCEV parameters below. First, baseline utility parameters, then satiation parameters  */
/* Important Note: For the satiation parameters (alphas and gammas) do not provide the final values of alphas and gammas. 
                   Provide the values of the parameters that are actually estimated in the model. 
                   For example, gamma is parameterized as exp(theeta), and theeta is estimated. So provide the theeta values here.
                   Simialrly, Alpha is parameterized as 1-(1/exp(delta)). Provide the delta values here.
                   
                   To make it simple, the MDCEV model estimation code first estiamtes the theeta and delta values and in the next step
                    provides the gamma and alpha values in the second set of estimates. Please provide the first set of estiamtes here.
*/ 
bmdcev = { 
0.722
0.2134
-0.7118
-1.8579
-1.2708
-0.8307
0.2222
2.0065
1.1278
-2.5463
0.8836
-1.1022
-9.2906
-2.6321
1.5
-0.0603
0.5402
0.2783
0.0727
0.1933
-0.7211
-0.2047
-0.0719
0.209
0.3545
2.1248
-0.0852
0.0096
0.4928
0.782
-0.5079
0.5273
-0.526
0.3828
-0.1036
0.1177
0.0115
2.0413
-0.0851
-0.1196
0.6985
1.0698
0.2015
0.4546
0.0666
0.2428
-0.2362
0.2633
0.5341
3.1413
-0.1249
0.1481
1.0338
0.6259
-0.3419
0.4426
-0.2427
-0.0055
-0.266
0.1093
0.2924
2.8374
-0.1856
0.0578
0.2119
0.1181
0.2981
0.569
-0.5915
0.1859
-0.1158
0.1221
0.4809
3.7022
-0.0649
0.0589
0.8293
-0.1849
-0.4226
0.1373
-0.0261
0.9333
-0.2723
0.3191
0.0323
3.5224
-0.1316
0.0169
-0.0899
0.4363
0.2021
0.3022
0.1857
0.6128
-0.378
0.155
0.2022
3.712
-0.1276
-0.2503
0.6279
0.5014
0.9263
0.6775
0.2853
-0.1827
0.2512
0.1365
2.3593
-0.0326
-0.3578
0.4422
0.0327
0.5816
-0.3057
0.4234
-0.2663
0.189
0.513
3.0029
0.0386
-0.7138
0.476
0.738
0.9988
1.5664
1.0221
-0.0169
0.1334
0.1081
5.8992
-0.0125
-0.3775
0.3007
0.4444
0.4415
0.1306
0.6781
-0.2685
0.2128
0.2728
3.0125
-0.0807
-0.6705
0.3245
1.1535
1.2253
1.2652
-0.2356
-0.1832
0.4393
0.7172
4.9453
-0.0488
-0.7214
0.2446
0.1239
-0.2088
-1.9292
-0.4452
0.0679
0.5095
0.9175
15.6165
0.1889
-0.6938
0.323
0.384
0.1995
1.0002
0.1109
-0.037
0.1011
-0.0643
7.5959
-0.0496
0.1645
0.4828
-0.4059
0.2327
-0.2847
0.5961
-0.1749
0.3061
0.3343
4.4504
-1000
1.4068
4.5358
4.8248
7.6213
1.0364
2.6063
-0.2632
0.8391
1.9037
2.6062
1.0619
3.2176
3.025
1.9935
0.0445
0.9293
3.1683
-0.2958
-0.1577
-0.6256
0.0358
-0.123
0.2698
-0.1383
-0.1439
-0.1472
-0.0926
-0.1652
-0.2332
-0.1908
-0.1431
3.8377
1.0
};




bmdcev = bmdcev';

{data, dtind} = indices(dataset,0);

open fin = ^dataset;
do until eof(fin);
	dtamat = readr(fin,nobs);
	dtamat = dtamat[.,dtind];

	Forecasts = forec(bmdcev,dtamat);
	print Forecasts;
	/* print "Done!"; */
endo;

fin = close(fin);

/*  Forecasts are printed for each nrep for each individual. 
  Total number of rows printed = nrep*nobs (nrep number of rows for each observation)
  Number of columns printed = nc+2. 
        First column corresponds to the observation (or individual) number, 
        second column corresponds to the number of error draws set,
        The remianing columns correspond to the predicted CONSUMPTIONS for each alternative
  If you want the expenditures, simply multiply the consumptions by the corresponding unit prices
*/ 


/***************************************************************************************************/
/*  procedure for forecasting  */ 
proc forec(x,dta);
 local e1,popass,p0,xdel,xgam,v2,w2,u2,j,v,w,u,a,b,m,c,xsig,ylarge1,ylarge2,fin,ass,r,ylarge,vv,vlos,ut,p1,p2,p3,z,w1,z1,D;
 local xthet,ddd,gr,ppp2,pp,ppl,k,v2s,pplnsum,xtheta,xsigm,xsigmm,N,m1,p211,p212,p213,p22,f1; //uts;
 local verr, fin1, as, prices, vqr,alts, fc, lambda, nreps;
 e1 = rows(dta);
 popass = dta[.,_po];
 xdel = eqmatdel'*x[nvarm+1:nvarm+rows(eqmatdel)];
 xgam = eqmatgam'*x[nvarm+rows(eqmatdel)+1:nvarm+rows(eqmatdel)+rows(eqmatgam)];
 xsigm = x[nvarm+rows(eqmatdel)+rows(eqmatgam)+1];
 v2 = (ones(nc,1) .*. x[1:nvarm])*~(dta[.,ivm])';
 w2 = (ones(nc,1) .*. xdel)*~(dta[.,ivd])';
 u2 = (ones(nc,1) .*. xgam)*~(dta[.,ivg])'; 

 j=1;
 v = {};
 w = {};
 u = {};
 alts = zeros(1,nc);
 do until j == nc+1;
   v = v~(sumc(v2[(j-1)*nvarm+1:(j*nvarm),.]));
   w = w~(sumc(w2[(j-1)*nvardel+1:(j*nvardel),.]));
   u = u~(sumc(u2[(j-1)*nvargam+1:(j*nvargam),.]));
   alts[1,j] = j; //alternative number
   j = j+1;
 endo;
//// u[.,1:numout] = -1000*ones(e1,numout);
 clear v2,w2;
 v= exp(v);
 v = v.*.ones(nrep,1);

 if (_gumbel==1);
  /*    open fin1 = ^outhalt for read;  */ 
/*    call seekr(fin1,((popass[1]-1)*nrep+11));  */ 
/*    as = readr(fin1,nrep*e1);  */ 
/*    as = as[.,1:nc];  */ 
  as = rndu(nrep*e1,nc)./10;
  as = -ln(-ln(as)).*xsigm;
/*    fin1 = close(fin1); */
else;
  as = zeros(nrep*e1,nc);
 endif;
 v = v.*exp(as);
 v = v./(dta[.,flagprcm].*.ones(nrep,1)); /*   Price-normalized Baseline Utilities  */
 a = 1-(1./(1+exp(w))); /*    Alphas  */
 prices = dta[.,flagprcm];
 f1 = exp(u);           /*    Gammas */

 for i(1,e1,1);
   for r(1,nrep,1);
     fc = zeros(1,nc);
     vqr = {};
     vqr = alts|v[((i-1)*nrep+r),.]|prices[i,.]|f1[i,.];
     ////vqr = vqr[.,1:numout]~(rev(sortr(vqr[.,numout+1:nc],2)')');
     vqr = (rev(sortr(vqr[.,1:nc],2)')');
     ////m = numout;
     m =1;
     k = -1;
     ////N = sumc((prices[i,1:numout].*((vqr[2,1:numout]).^(ones(1,numout)-a[1,1:numout])))');
     N = sumc((vqr[4,1].*vqr[3,1].*((vqr[2,1]).^(1./(ones(1,1)-a[1,1]))))');
     ////D = sumc((dta[i,flagchm])');
     D = sumc((dta[i,flagchm])') + (vqr[4,1]*vqr[3,1]);
     lambda = (N/D)^(1-a[1,1]);
     ////if (vqr[2,numout+1]<lambda);
     if (vqr[2,2]<lambda);  
       ////fc[1,1:numout] = ((vqr[2,1:numout]./lambda).^(ones(1,numout)-a[1,1:numout]));
       fc[1,1] = (((vqr[2,1]./lambda).^(1./(ones(1,1)-a[1,1])))*vqr[4,1]) - vqr[4,1] ;
       ////fc[1,numout+1:nc] = zeros(1,nc-numout);
       fc[1,2:nc] = zeros(1,nc-1);
     else;
       do until k==m;
         m = m+1;
         if (m==nc);
           ////fc[1,1:numout] = ((vqr[2,1:numout]./lambda).^(1-a[1,1:numout]));
           ////fc[1,numout+1:nc] = (((vqr[2,numout+1:nc]./lambda).^(1-a[1,numout+1:nc]))-ones(1,nc-numout)).*(vqr[4,numout+1:nc]);
           fc[1,1:nc] = (((vqr[2,1:nc]./lambda).^(1./(1-a[1,1:nc])))-ones(1,nc)).*(vqr[4,1:nc]);
           fc[1,1:nc] = sumc((dta[i,flagchm])') .*fc[1,1:nc]./(sumc((fc[1,1:nc])'));
           k=m;
         elseif (m < nc);
           N = N + (vqr[4,m]*vqr[3,m]*(vqr[2,m]^(1-a[1,1])));
           D = D + (vqr[4,m]*vqr[3,m]);
           lambda = (N/D)^(1-a[1,1]);
           if (vqr[2,m+1]<lambda);
             ////fc[1,1:numout] = ((vqr[2,1:numout]./lambda).^(1-a[1,1:numout]));
             ////fc[1,numout+1:m] = (((vqr[2,numout+1:m]./lambda).^(1-a[1,numout+1:m]))-ones(1,m-numout)).*(vqr[4,numout+1:m]);
             fc[1,1:m] = (((vqr[2,1:m]./lambda).^(1./(1-a[1,1:m])))-ones(1,m)).*(vqr[4,1:m]);
             fc[1,m+1:nc] = zeros(1,nc-m);
             k = m;
           endif;  
         endif;
       endo;
     endif;
     vqr[2,1:nc] = fc;
     vqr = sortr(vqr,1);
     v[((i-1)*nrep+r),1:nc] = vqr[2,.];
   endfor;
 endfor;
 nreps = {};
 for i(1,nrep,1);
  nreps = nreps|i;
 endfor;
 z = (popass.*.ones(nrep,1))~(ones(rows(popass),1).*.nreps)~v;
 retp(z);
endp;

