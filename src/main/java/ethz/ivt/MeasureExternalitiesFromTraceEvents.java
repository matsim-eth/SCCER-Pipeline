package ethz.ivt;

import com.google.inject.Guice;
import com.google.inject.Injector;
import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.counters.NoiseCounter;
import ethz.ivt.externalities.data.AggregateCongestionData;
import ethz.ivt.externalities.data.AggregateNoiseData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.roadTypeMapping.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.roadTypeMapping.OsmHbefaMapping;
import org.matsim.contrib.emissions.roadTypeMapping.RoadTypeMappingProvider;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import ethz.ivt.externalities.counters.EmissionsCounter;
import ethz.ivt.externalities.counters.CongestionCounter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;

/**
 * Created by molloyj on 17.07.2017.
 */
public class MeasureExternalitiesFromTraceEvents {
    private final static Logger log = Logger.getLogger(MeasureExternalitiesFromTraceEvents.class);
    private final int bin_size_s;
    private final MatsimEventsReader reader;
    private final Scenario scenario;

    private String CONGESTION_FILE; // = "aggregate/congestion/aggregate_delay.csv";
    private String NOISE_FILE; // = "aggregate/noise/marginal_damages_link_car_merged_xyt1t2t3etc.csv";

    private Config config;
    private NoiseContext noiseContext;
    private NoiseTimeTracker noiseTimeTracker;
    private EventsManagerImpl eventsManager;
    private final CongestionCounter congestionCounter;
    private final EmissionsCounter emissionsCounter;

    public MeasureExternalitiesFromTraceEvents(Config config, String congestionFile) {
        CONGESTION_FILE = congestionFile;
        //NOISE_FILE = "";
        bin_size_s = 3600;

        String date = "2018-07-27"; //ExternalityUtils.getDate(LocalDate.now());

        this.config = config;
        scenario = ScenarioUtils.loadScenario(config);

        log.info("load road types");
        setUpRoadTypes(scenario.getNetwork());
        setUpVehicleTypes();

        eventsManager = new EventsManagerImpl();
        reader = new MatsimEventsReader(eventsManager);
        log.info("add vehicles");

        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(new JITvehicleCreator(scenario));

        log.info("load aggregate congestion data");

        // load precomputed aggregate data
        AggregateCongestionData aggregateCongestionData = new AggregateCongestionData(scenario, bin_size_s);
        aggregateCongestionData.loadDataFromCsv(CONGESTION_FILE);
        congestionCounter = new CongestionCounter(scenario, v2deh, date, aggregateCongestionData);
        log.trace("add congestion handler");

        eventsManager.addHandler(congestionCounter);

        //AggregateNoiseData aggregateNoiseData = new AggregateNoiseData(scenario, bin_size_s);
        //aggregateNoiseData.loadDataFromCsv(RUN_FOLDER + NOISE_FILE);
        //NoiseCounter noiseCounter = new NoiseCounter(scenario, v2deh, date, aggregateNoiseData);
        //eventsManager.addHandler(noiseCounter);
        log.info("load emissions module");
        // setup externality counters
        EmissionsConfigGroup ecg = (EmissionsConfigGroup) scenario.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME);
        ecg.setUsingDetailedEmissionCalculation(false);

        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager, OsmHbefaMapping.build());

        emissionsCounter = new EmissionsCounter(scenario, v2deh, date);
        eventsManager.addHandler(emissionsCounter);

        // add event handlers
        eventsManager.addHandler(v2deh);


    }

    public void process(String eventsFile) {
        log.info("start processing event file");
        eventsManager.initProcessing();
        reader.readFile(eventsFile);
        log.info("finish processing event file");

        // write to file
   //     emissionsCounter.writeCsvFile(config.controler().getOutputDirectory(), emissionsCounter.getDate());
   //     congestionCounter.writeCsvFile(config.controler().getOutputDirectory(), congestionCounter.getDate());
   //     noiseCounter.writeCsvFile(config.controler().getOutputDirectory(), noiseCounter.getDate());

        eventsManager.finishProcessing();
        //TODO: make sure that the handlers get reset!!!!!!!!!!

        //return results here

    }

    public void write(String folder, String date, String person) {
        Path outputFolder = Paths.get(folder, date);
        congestionCounter.writeCsvFile(outputFolder, person);
        emissionsCounter.writeCsvFile(outputFolder, person);
    }

    private void setUpRoadTypes(Network network) {
        for (Link l : network.getLinks().values()) {
            NetworkUtils.setType(l, (String) l.getAttributes().getAttribute("osm:way:highway"));
        }
    }

    private void setUpVehicleTypes() {
        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

    }

}
