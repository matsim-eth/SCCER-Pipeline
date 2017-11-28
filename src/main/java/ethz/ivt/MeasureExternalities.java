package ethz.ivt;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.counters.NoiseCounter;
import ethz.ivt.externalities.data.AggregateCongestionData;
import ethz.ivt.externalities.data.AggregateNoiseData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import ethz.ivt.externalities.counters.EmissionsCounter;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.externalities.counters.CongestionCounter;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;

/**
 * Created by molloyj on 17.07.2017.
 */
public class MeasureExternalities {
    private final static Logger log = Logger.getLogger(MeasureExternalities.class);

    private static String RUN_FOLDER; // = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pc/scenario/";
    private static String CONFIG_FILE; // = "defaultIVTConfig_w_emissions.xml"; // "defaultIVTConfig_w_emissions.xml";
    private static String EVENTS_FILE; // = "20171117_events.xml.gz"; // "test.events.xml.gz"
    private static String CONGESTION_FILE; // = "aggregate/congestion/aggregate_delay.csv";
    private static String NOISE_FILE; // = "aggregate/noise/aggregate_noise.csv";

    private Config config;
    private NoiseContext noiseContext;
    private NoiseTimeTracker noiseTimeTracker;
    private EventsManagerImpl eventsManager;

    public static void main(String[] args) {
        RUN_FOLDER = args[0];
        CONFIG_FILE = args[1];
        EVENTS_FILE = args[2];
        CONGESTION_FILE = args[3];
        NOISE_FILE = args[4];
        new MeasureExternalities().run();
    }

    public void run() {

        int bin_size_s = 3600;
        String date = ExternalityUtils.getDate(EVENTS_FILE);

        config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE, new EmissionsConfigGroup(), new NoiseConfigGroup());
        config.controler().setOutputDirectory(RUN_FOLDER + "output/");
        Scenario scenario = ScenarioUtils.loadScenario(config);
//        ((NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME)).setTimeBinSizeNoiseComputation(bin_size_s);

        setUpVehicles(scenario);
        
        eventsManager = new EventsManagerImpl();
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();

//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);

        // load precomputed aggregate data
        AggregateCongestionData aggregateCongestionData = new AggregateCongestionData(scenario, bin_size_s);
        aggregateCongestionData.loadDataFromCsv(RUN_FOLDER + CONGESTION_FILE);

        AggregateNoiseData aggregateNoiseData = new AggregateNoiseData(scenario, bin_size_s);
        aggregateNoiseData.loadDataFromCsv(RUN_FOLDER + NOISE_FILE);

        // setup externality counters
        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);
        EmissionsCounter emissionsCounter = new EmissionsCounter(scenario, v2deh, date);
        CongestionCounter congestionCounter = new CongestionCounter(scenario, v2deh, date, aggregateCongestionData);
        NoiseCounter noiseCounter = new NoiseCounter(scenario, v2deh, date, aggregateNoiseData);

        // add event handlers
        eventsManager.addHandler(v2deh);
//        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(emissionsCounter);
        eventsManager.addHandler(congestionCounter);
        eventsManager.addHandler(noiseCounter);

        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        // write to file
        emissionsCounter.writeCsvFile(config.controler().getOutputDirectory(), congestionCounter.getDate());
        congestionCounter.writeCsvFile(config.controler().getOutputDirectory(), congestionCounter.getDate());
        noiseCounter.writeCsvFile(config.controler().getOutputDirectory(), congestionCounter.getDate());

//        emissionModule.writeEmissionInformation();
//        log.info("Total delay: " + congestionHandler.getTotalDelay());
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
