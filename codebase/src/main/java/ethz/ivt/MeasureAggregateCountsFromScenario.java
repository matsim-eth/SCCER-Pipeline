package ethz.ivt;

import ethz.ivt.externalities.aggregation.CountPerLinkAggregator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class MeasureAggregateCountsFromScenario {
	private final static Logger log = Logger.getLogger(MeasureAggregateCountsFromScenario.class);

    private static String RUN_FOLDER; // = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pc/scenario/";
	private static String CONFIG_FILE; // = "defaultIVTConfig_w_emissions.xml";
    private static String EVENTS_FILE; // = "800.events.xml.gz";
    
    private Config config;
    private EventsManagerImpl eventsManager;
    protected int bin_size_s = 3600;

    public static void main(String[] args) {
        RUN_FOLDER = args[0];
        CONFIG_FILE = args[1];
        EVENTS_FILE = args[2];
        new MeasureAggregateCountsFromScenario().run();
    }
    
    public void run() {

    	// set up config
    	config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE, new EmissionsConfigGroup(), new NoiseConfigGroup());
        config.controler().setOutputDirectory(RUN_FOLDER + "aggregate/");
        Scenario scenario = ScenarioUtils.loadScenario(config);

    	// set up event manager and handlers
    	eventsManager = new EventsManagerImpl();

        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CountPerLinkAggregator countPerLinkAggregator = new CountPerLinkAggregator(scenario, v2deh);
        eventsManager.addHandler(countPerLinkAggregator);

        setUpVehicles(scenario);

        // read through MATSim events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        // save congestion data to single csv file
        countPerLinkAggregator.aggregateCountDataPerLinkPerTime.writeDataToCsv(config.controler().getOutputDirectory());
        eventsManager.finishProcessing();
    }
    
    private void setUpVehicles(Scenario scenario) {
        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(100.0 / 3.6);
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
