package ethz.ivt;

import ethz.ivt.externalities.counters.*;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import org.matsim.api.core.v01.network.Network;

import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.roadTypeMapping.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.roadTypeMapping.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

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
    private final ExternalityCounter externalityCounter;
    private final CongestionCounter congestionCounter;
    private final EmissionsCounter emissionsCounter;
    private final EmissionModule emissionModule;

    public MeasureExternalitiesFromTraceEvents(Scenario scenario, AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime) {
        //NOISE_FILE = "";
        bin_size_s = 3600;
        this.scenario = scenario;

        String date = "2018-07-27"; //ExternalityUtils.getDate(LocalDate.now());

        eventsManager = new EventsManagerImpl();
        reader = new MatsimEventsReader(eventsManager);
        log.info("add vehicles");

        eventsManager.addHandler(new JITvehicleCreator(scenario));

        GpsLinkLeaveEventHandler gpsLinkLeaveEventHandler = new GpsLinkLeaveEventHandlerImpl(eventsManager);
        eventsManager.addHandler(gpsLinkLeaveEventHandler);


        //AggregateNoiseData aggregateNoiseData = new AggregateNoiseData(scenario, bin_size_s);
        //aggregateNoiseData.loadDataFromCsv(RUN_FOLDER + NOISE_FILE);
        //NoiseCounter noiseCounter = new NoiseCounter(scenario, v2deh, date, aggregateNoiseData);
        //eventsManager.addHandler(noiseCounter);
        log.info("load emissions module");
        // setup externality counters
        EmissionsConfigGroup ecg = (EmissionsConfigGroup) scenario.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME);
    //    ecg.setUsingDetailedEmissionCalculation(false);

        //add Hbefa mappings to the network
        HbefaRoadTypeMapping hbefaRoadTypeMapping = OsmHbefaMapping.build();
        hbefaRoadTypeMapping.addHbefaMappings(scenario.getNetwork());

        emissionModule = new EmissionModule(scenario, eventsManager);

        externalityCounter = new ExternalityCounter(scenario, date);
        emissionsCounter = new EmissionsCounter(scenario, externalityCounter);
        congestionCounter = new CongestionCounter(scenario, externalityCounter);

        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(emissionsCounter);
        eventsManager.addHandler(congestionCounter);
    }

    public void reset() {
        eventsManager.resetHandlers(0);
    }

    public void process(String eventsFile, String date, String personId) {
        externalityCounter.setDate(date);
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
        externalityCounter.writeCsvFile(outputFolder, person);
    }

    public static void setUpRoadTypes(Network network) {
        for (Link l : network.getLinks().values()) {
            NetworkUtils.setType(l, (String) l.getAttributes().getAttribute("osm:way:highway"));
        }
    }

    public static void addVehicleTypes(Scenario scenario) {
        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("Benzin", VehicleType.class));
        car.setMaximumVelocity(100.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC-P-Euro-4END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

        VehicleType car_diesel = VehicleUtils.getFactory().createVehicleType(Id.create("Diesel", VehicleType.class));
        car_diesel.setMaximumVelocity(100.0 / 3.6);
        car_diesel.setPcuEquivalents(1.0);
        car_diesel.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;1,4-<2L;PC D Euro-4END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car_diesel);

        //hybrids are only coming in hbefa vresion 4.

    }

}
