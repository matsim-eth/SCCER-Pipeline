package playground.ivt.proj_sccer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
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

public class MeasureAverageCausedDelay {
	private final static Logger log = Logger.getLogger(MeasureAverageCausedDelay.class);
	
    final private static String CONFIG_FILE = "defaultIVTConfig_w_emissions.xml"; // "defaultIVTConfig_w_emissions.xml";
    final private static String RUN_FOLDER = "P:\\Projekte\\SCCER\\zurich_1pc\\scenario\\";
    final private static String EVENTS_FILE = "800.events.xml.gz";
    
    private Config config;
    private EventsManagerImpl eventsManager;

    public static void main(String[] args) {
        new MeasureAverageCausedDelay().run();
    }
    
    public void run() {
    	int bin_size_s = 3600;
    	
    	// set up config
    	config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE);
    	config.controler().setOutputDirectory(RUN_FOLDER + "output\\");
    	Scenario scenario = ScenarioUtils.loadScenario(config);
    	
    	// set up event manager and handlers
    	eventsManager = new EventsManagerImpl();
    	MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
    	
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, bin_size_s);
        
        eventsManager.addHandler(v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        setUpVehicles(scenario);

        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        congestionAggregator.computeLinkAverageCausedDelays();
        congestionAggregator.writeCsvFile(config.controler().getOutputDirectory());

        log.info("Total delay: " + congestionHandler.getTotalDelay());
        eventsManager.finishProcessing();
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
