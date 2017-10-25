package playground.ivt.proj_sccer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.data.NoiseLink;
import org.matsim.contrib.noise.handler.LinkSpeedCalculation;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.contrib.noise.handler.PersonActivityTracker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.ivt.proj_sccer.aggregation.CongestionAggregator;
import playground.ivt.proj_sccer.vsp.handlers.CongestionHandler;
import playground.ivt.proj_sccer.vsp.handlers.CongestionHandlerImplV3;

public class MeasureAverageScenarioExternalities {
	private final static Logger log = Logger.getLogger(MeasureAverageScenarioExternalities.class);
	
    final private static String CONFIG_FILE = "defaultIVTConfig_w_emissions.xml"; // "defaultIVTConfig_w_emissions.xml";
    final private static String RUN_FOLDER = "P:\\Projekte\\SCCER\\zurich_1pc\\scenario\\";
    final private static String EVENTS_FILE = "800.events.xml.gz";
    
    private Config config;
    private EventsManagerImpl eventsManager;
    private NoiseContext noiseContext;
    private NoiseTimeTracker noiseTimeTracker;
    private NoiseConfigGroup noiseParameters;

    public static void main(String[] args) {
        new MeasureAverageScenarioExternalities().run();
    }
    
    public void run() {

    	int bin_size_s = 3600;
    	
    	// set up config
    	config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE, new EmissionsConfigGroup(), new NoiseConfigGroup());
        config.controler().setOutputDirectory(RUN_FOLDER + "output\\");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        this.noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
        noiseParameters.setTimeBinSizeNoiseComputation(bin_size_s);
//        noiseParameters.setComputeAvgNoiseCostPerLinkAndTime(false);
        
    	
    	// set up event manager and handlers
    	eventsManager = new EventsManagerImpl();
    	MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        
        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh, bin_size_s);
        
        eventsManager.addHandler(v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        setUpVehicles(scenario);
        
//        setUpNoise(scenario);
//        NoiseAggregator noiseAggregator = new NoiseAggregator(scenario, v2deh, bin_size_s);

        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        congestionAggregator.computeLinkAverageCausedDelays();
        congestionAggregator.writeCsvFile(config.controler().getOutputDirectory(), "average_caused_delay.csv");
        
//        noiseAggregator.computeLinkId2timeBin2averageValues();
//        noiseAggregator.writeCsvFile(config.controler().getOutputDirectory() + "average_caused_noise.csv");

        log.info("Total delay: " + congestionHandler.getTotalDelay());
        eventsManager.finishProcessing();
    }
    
    public void setUpNoise(Scenario scenario) { //taken from NoiseOfflineCalculation

        noiseContext = new NoiseContext(scenario);
        noiseContext.getNoiseParams().setWriteOutputIteration(0); //avoid writing multiple output files!!!
        noiseContext.getNoiseParams().setThrowNoiseEventsCaused(true);
//        noiseParameters.setThrowNoiseEventsCaused(true);

        noiseTimeTracker = new NoiseTimeTracker();
        noiseTimeTracker.setNoiseContext(noiseContext);
        noiseTimeTracker.setEvents(eventsManager);
        //noiseTimeTracker.setOutputFilePath(outputFilePath);

        eventsManager.addHandler(noiseTimeTracker);

        if (noiseContext.getNoiseParams().isUseActualSpeedLevel()) {
            LinkSpeedCalculation linkSpeedCalculator = new LinkSpeedCalculation();
            linkSpeedCalculator.setNoiseContext(noiseContext);
            eventsManager.addHandler(linkSpeedCalculator);
        }

        if (noiseContext.getNoiseParams().isComputePopulationUnits()) {
            PersonActivityTracker actTracker = new PersonActivityTracker(noiseContext);
            eventsManager.addHandler(actTracker);
        }
    }
    
    private void setUpVehicles(Scenario scenario) {
        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);
            //easy option: add
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);

            scenario.getVehicles().addVehicle(v);
            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }

    }

}
