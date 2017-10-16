package playground.ivt.proj_sccer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.LinkSpeedCalculation;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.contrib.noise.handler.PersonActivityTracker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.ivt.proj_sccer.aggregation.EmissionsAggregator;
import playground.ivt.proj_sccer.aggregation.EmissionsTally;
import playground.ivt.proj_sccer.vsp.handlers.CongestionHandler;
import playground.ivt.proj_sccer.aggregation.CongestionAggregator;
import playground.ivt.proj_sccer.vsp.handlers.CongestionHandlerImplV3;

/**
 * Created by molloyj on 17.07.2017.
 */
public class MeasureExternalities {
    private final static Logger log = Logger.getLogger(MeasureExternalities.class);


 //   final private static String CONFIG_FILE = "output_config.xml"; // "defaultIVTConfig_w_emissions.xml";
 //   final private static String EVENTS_FILE = "output_events.xml.gz";
 //   final private static String RUN_FOLDER = "C:\\Users\\molloyj\\Documents\\ARE_SP_2016\\zurich_1pc\\emissions\\example\\";

    final private static String CONFIG_FILE = "defaultIVTConfig_w_emissions.xml"; // "defaultIVTConfig_w_emissions.xml";
    final private static String EVENTS_FILE = "800.events.xml.gz";
    final private static String RUN_FOLDER = "P:\\Projekte\\SCCER\\zurich_1pc\\scenario\\";
    
    final private static String OUTPUT_FOLDER = "P:\\Projekte\\SCCER\\zurich_1pc\\scenario\\output\\";


    private Config config;
    private NoiseContext noiseContext;
    private NoiseTimeTracker noiseTimeTracker;
    private EventsManagerImpl eventsManager;

    public static void main(String[] args) {
        new MeasureExternalities().run();
    }

    public void run() {

        int bin_size_s = 3600;

        config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE, new EmissionsConfigGroup(), new NoiseConfigGroup());
        config.controler().setOutputDirectory(RUN_FOLDER + "output\\");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        ((NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME)).setTimeBinSizeNoiseComputation(bin_size_s);

        eventsManager = new EventsManagerImpl();
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, bin_size_s);
        EmissionsAggregator emissionsAggregator = new EmissionsAggregator(scenario, bin_size_s, v2deh);

        setUpVehicles(scenario);
        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);

        eventsManager.addHandler(v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);
        //eventsManager.addHandler(emissionsAggregator);


        EmissionsTally emissionsTally = new EmissionsTally(scenario, v2deh);
        eventsManager.addHandler(emissionsTally);

        //emissions
        //add emissions module

   //     setUpNoise(scenario);

        //add listener to tally emission events by (warm|cold)/link/userGroup/time


        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        emissionsTally.outputSummary();
//        emissionsTally.writeCSVFile(OUTPUT_FOLDER);
        
//        congestionAggregator.getLinkIdAverageCausedDelays();
        congestionAggregator.writeCSVFile(OUTPUT_FOLDER);

        emissionModule.writeEmissionInformation();
        log.info("Total delay: " + congestionHandler.getTotalDelay());
        eventsManager.finishProcessing();
    }

    public void setUpNoise(Scenario scenario) { //taken from NoiseOfflineCalculation

        noiseContext = new NoiseContext(scenario);

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
