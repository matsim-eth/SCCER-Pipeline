package ethz.ivt;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionPerLinkField;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import org.matsim.api.core.v01.network.Network;

import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.roadTypeMapping.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import ethz.ivt.externalities.counters.EmissionsCounter;
import ethz.ivt.externalities.counters.CongestionCounter;

import java.util.LinkedList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;


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

    public MeasureExternalitiesFromTraceEvents(Scenario scenario, AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime) {
        //NOISE_FILE = "";
        bin_size_s = 3600;
        this.scenario = scenario;

        String date = "2018-07-27"; //ExternalityUtils.getDate(LocalDate.now());

        eventsManager = new EventsManagerImpl();
        reader = new MatsimEventsReader(eventsManager);
        log.info("add vehicles");

        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(new JITvehicleCreator(scenario));


        congestionCounter = new CongestionCounter(scenario, v2deh, date, aggregateCongestionDataPerLinkPerTime);

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
        eventsManager.initProcessing();
        reader.readFile(eventsFile);

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

    public static void setUpRoadTypes(Network network) {
        for (Link l : network.getLinks().values()) {
            NetworkUtils.setType(l, (String) l.getAttributes().getAttribute("osm:way:highway"));
        }
    }

    public static void addVehicleTypes(Scenario scenario) {
        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

    }

}
