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

		Hier Name der gewünschten INPUT Datei Spezifizieren:									*/

dataset = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/Sim_Input_Data/G_HHs_JU_1.751.dat";

/*---------------------------------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------------------------------- 

##############################################################################################################################

-----------------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------------

		Hier Name der zu erstellenden OUTPUT Datei spezifizieren:								*/

output file = /Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/bjaeggi/MATSim_Implementation/Sim_Output_Data/HHS_JU_1.75.asc reset;

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

let	ivm1 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm2 = {	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm3 = {	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm4 = {	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm5 = {	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm6 = {	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm7 = {	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	Fuel2	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm8 = {	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm9 = {	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm10 = {	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm11 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm12 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm13 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm14 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm15 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	};
let	ivm16 = {	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	UNO 	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	sero	Income	Fuel	Kids	AC1	AC2	AC3	UrbCi	ERF1	ERF2	TaxMun	};

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
1.2088     	/* Kosntante für D1 Subcompact Diesel  */
0.65	/* Kosntante für D2 Compact Diesel  */
0.35		/* Kosntante für D3 MiniMVP Diesel  */
-1.1003	/* Kosntante für D4 MidSized Diesel  */
-0.2		/* Kosntante für D5 FullSized Diesel  */	
-0.8183	/* Kosntante für D6 LuxusSport Diesel  */
1.75		/* Kosntante für B0 Micro Benzin  */
2.5		/* Kosntante für B1 Subcompact Benzin  */
2.5		/* Kosntante für B2 Compact Benzin  */
0.15		/* Kosntante für B3 MiniMVP Benzin  */
2.1		/* Kosntante für B4 MidSized Benzin  */
0.7		/* Kosntante für B5 FullSized Benzin  */
0.3		/* Kosntante für B6 Luxus Benzin  */
1.43	 	/* Kosntante für B7 Sport Benzin  */
1.55		/* Kosntante für U0 Alternative Technologie  */
-0.0562
0.5047
0.3038
-0.0658
0.1736
-0.9012
-0.2052
-0.0864
0.2176
0.3935
-0.0786
0.0054
0.4256
0.7333
-0.4658
0.5564
-0.7203
0.2559
-0.0528
0.1443
0.049
-0.0754
-0.1005
0.6121
1.118
-0.188
0.24
-0.9263
0.4036
-0.2487
0.2356
0.6407
-0.1201
0.1577
1.0126
0.6302
-0.3635
0.2372
-0.1812
0.1015
-0.1842
0.0616
0.172
-0.1811
0.1244
-0.0833
-0.0239
0.1508
0.6914
-0.7357
0.1341
-0.1211
0.1479
0.7481
-0.0231
0.1157
0.7941
-0.2576
-0.4265
0.0943
0.0421
0.4377
-0.2331
0.3296
0.4403
-0.125
0.0041
-0.1211
0.612
0.3299
0.1235
-0.1829
0.2676
-0.2898
0.1749
0.2552
-0.1612
-0.2551
0.3942
0.3979
0.9111
0.4706
0.3064
-0.2025
0.2145
-0.0641
-0.0602
-0.3336
0.4407
0.0317
0.745
0.0302
0.2798
-0.2786
0.2199
0.4589
-0.0313
-0.4615
1.1762
0.9877
1.2463
1.2
0.9223
-0.1792
0.2278
-0.0924
0.0031
-0.3364
0.532
0.3206
0.5648
0.1387
0.5841
-0.2163
0.259
0.2626
0.0246
-0.5024
0.5995
0.8996
0.8337
0.1436
0.2191
-0.0943
0.2487
0.5756
0.14
-0.4568
0.5256
0.6855
0.9067
-0.4219
-0.1378
-0.1381
0.3404
0.3864
0.1569
-0.3841
0.411
-0.1945
-0.0721
-1.2895
0.2879
-0.1274
0.1637
0.3024
0.0173
0.1586
0.5467
-0.8016
-0.1309
-0.6394
0.536
-0.2082
0.3044
0.3974
-1000
0.9984
4.5902
4.2215
7.4314
0.9339
1.9938
-1.0573
1.1465
2.3902
3.3273
1.799
3.7623
3.5066
3.4412
0.8616
0.7552
2.2577
-0.3154
-0.1512
-0.5748
0.0638
-0.0093
0.4062
-0.1024
-0.1489
-0.1781
-0.0754
-0.1806
-0.1914
-0.2152
-0.0587
2.8332
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

