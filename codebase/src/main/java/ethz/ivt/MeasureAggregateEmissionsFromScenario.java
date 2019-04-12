package ethz.ivt;

import ethz.ivt.externalities.aggregation.EmissionsAggregator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Random;

public class MeasureAggregateEmissionsFromScenario {
    private final static Logger log = Logger.getLogger(MeasureExternalitiesFromTraceEvents.class);

    private static String RUN_FOLDER; // = "/home/ctchervenkov/Documents/projects/road_pricing/switzerland_10pct";
    private static String CONFIG_FILE; // = "switzerland_config_w_emissions.xml";
    private static String EVENTS_FILE; // = "20.events.xml.gz";

    private Config config;
    private EventsManagerImpl eventsManager;

    public static void main(String[] args) {
//        RUN_FOLDER = args[0];
//        CONFIG_FILE = args[1];
//        EVENTS_FILE = args[2];

        RUN_FOLDER = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pct/scenario/";
        CONFIG_FILE = "defaultIVTConfig_w_emissions.xml";
        EVENTS_FILE = "800.events.xml.gz";


        new MeasureAggregateEmissionsFromScenario().run();
    }

    public void run() {

        int bin_size_s = 3600;

        config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE, new EmissionsConfigGroup());
        config.controler().setOutputDirectory(RUN_FOLDER + "aggregate/");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        //add mappings to network
//        OsmHbefaMapping.build().addHbefaMappings(scenario.getNetwork());

        setUpVehicles(scenario);

        eventsManager = new EventsManagerImpl();

        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        // setup aggregators
        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);
        EmissionsAggregator emissionsAggregator = new EmissionsAggregator(scenario, v2deh);
        emissionModule.getEmissionEventsManager().addHandler(emissionsAggregator);

        // read MATSim events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        // save emissions data to csv files
        emissionsAggregator.aggregateEmissionsDataPerLinkPerTime.writeDataToCsv(config.controler().getOutputDirectory() + "emissions/40pcdiesel/");
        emissionsAggregator.aggregateEmissionsDataPerPersonPerTime.writeDataToCsv(config.controler().getOutputDirectory() + "emissions/40pcdiesel/");
        log.info("Emissions aggregation completed for MATSim scenario " + CONFIG_FILE + ".");

        eventsManager.finishProcessing();
    }

    public void reset() {
        eventsManager.resetHandlers(0);
    }

    private void setUpVehicles(Scenario scenario) {
        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        // Petrol car setup
        VehicleType petrol_car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car + "_petrol", VehicleType.class));
        petrol_car.setMaximumVelocity(100.0 / 3.6);
        petrol_car.setPcuEquivalents(1.0);
        petrol_car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(petrol_car);

        // Diesel car setup
        VehicleType diesel_car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car + "_diesel", VehicleType.class));
        diesel_car.setMaximumVelocity(100.0 / 3.6);
        diesel_car.setPcuEquivalents(1.0);
        diesel_car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;<1,4L;PC D Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(diesel_car);

        // percentage of diesel vehicles
        Random randomGenerator = new Random();
        double percentDiesel = 0.4;

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);

            //add petrol or diesel vehicles according to percentage
            double percent = randomGenerator.nextDouble();
            if (percent < percentDiesel) {
                Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, diesel_car);
                scenario.getVehicles().addVehicle(v);
            } else {
                Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, petrol_car);
                scenario.getVehicles().addVehicle(v);
            }

            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }

    }
}
