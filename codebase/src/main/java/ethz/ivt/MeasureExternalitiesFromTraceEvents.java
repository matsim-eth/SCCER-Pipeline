package ethz.ivt;

import ethz.ivt.externalities.counters.CarExternalityCounter;
import ethz.ivt.externalities.counters.CongestionCounter;
import ethz.ivt.externalities.counters.ExternalityCostCalculator;
import ethz.ivt.externalities.counters.ExternalityCounter;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionField;
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
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


/**
 * Created by molloyj on 17.07.2017.
 */
public class MeasureExternalitiesFromTraceEvents {
    private final static Logger log = Logger.getLogger(MeasureExternalitiesFromTraceEvents.class);
    private final int bin_size_s;
    private final MatsimEventsReader reader;
    private final Scenario scenario;
    private final ExternalityCostCalculator ecc;

    private String CONGESTION_FILE; // = "aggregate/congestion/aggregate_delay.csv";
    private String NOISE_FILE; // = "aggregate/noise/marginal_damages_link_car_merged_xyt1t2t3etc.csv";

    private EventsManagerImpl eventsManager;
    private final ExternalityCounter externalityCounter;
    private String date;
    private String costValuesFile;

    public static void main(String[] args) {
//        String configPath = args[0];
//        String eventPath = args[1];
//        String congestionPath = args[2];
//        String costValuesPath = args[3];
//        String outputPath = args[4];

        String configPath = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pct/scenario/defaultIVTConfig_w_emissions.xml";
        String eventPath = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pct/scenario/800.events.xml.gz";
        String congestionPath = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pct/scenario/aggregate/congestion/aggregate_delay_per_link_per_time.csv";
        String costValuesPath = "/home/ctchervenkov/git/java/SCCER-Pipeline/codebase/src/test/resources/NISTRA_reference_values.txt";
        String outputPath = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pct/scenario/output/";

        // load config file
        Config config = ConfigUtils.loadConfig(configPath, new EmissionsConfigGroup());

        // preliminary scenario setup
        Scenario scenario = ScenarioUtils.loadScenario(config);
        MeasureExternalitiesFromTraceEvents.setUpVehicles(scenario, 0.4);
        MeasureExternalitiesFromTraceEvents.setUpRoadTypes(scenario.getNetwork());

        // load precalculated aggregate congestion data per link per time
        List<String> attributes = new LinkedList<>();
        attributes.add(CongestionField.COUNT.getText());
        attributes.add(CongestionField.DELAY_CAUSED.getText());
        attributes.add(CongestionField.DELAY_EXPERIENCED.getText());
        AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime = new AggregateDataPerTimeImpl<Link>(3600,
                scenario.getNetwork().getLinks().keySet(),
                attributes,
                null);
        aggregateCongestionDataPerLinkPerTime.loadDataFromCsv(congestionPath);

        MeasureExternalitiesFromTraceEvents runner = new MeasureExternalitiesFromTraceEvents(scenario, aggregateCongestionDataPerLinkPerTime, costValuesPath);
        runner.process(eventPath, "xxxx", null);
        runner.write(outputPath, "xxxx", "Switzerland");
    }

    public MeasureExternalitiesFromTraceEvents(
            Scenario scenario,
            AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime,
            String costValuesFile) {

        this.costValuesFile = costValuesFile;
        //NOISE_FILE = "";
        bin_size_s = 3600;
        this.scenario = scenario;

        date = "xxxx"; //ExternalityUtils.getDate(LocalDate.now());

        eventsManager = new EventsManagerImpl();
        reader = new MatsimEventsReader(eventsManager);
        log.info("add vehicles");

        eventsManager.addHandler(new JITvehicleCreator(scenario));

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

        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);

        externalityCounter = new ExternalityCounter(scenario, date);
        CarExternalityCounter carExternalityCounter = new CarExternalityCounter(scenario, externalityCounter);
        CongestionCounter congestionCounter = new CongestionCounter(scenario, aggregateCongestionDataPerLinkPerTime, externalityCounter);

        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(carExternalityCounter);
        eventsManager.addHandler(congestionCounter);

        ecc = new ExternalityCostCalculator(costValuesFile);

    }

    public void reset() {
        eventsManager.resetHandlers(0);
    }

    public void process(String eventsFile, String date, String personId) {
        externalityCounter.setDate(date);
        eventsManager.initProcessing();
        reader.readFile(eventsFile);

        ecc.addCosts(externalityCounter);

        eventsManager.finishProcessing();
        //TODO: make sure that the handlers get reset!!!!!!!!!!

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

    public static void setUpVehicles(Scenario scenario, double shareDiesel) {
        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        Id<VehicleType> carIdBenzin = Id.create("Benzin", VehicleType.class);
        Id<VehicleType> carIdDiesel = Id.create("Diesel", VehicleType.class);

        if (!scenario.getVehicles().getVehicleTypes().containsKey(carIdBenzin) || !scenario.getVehicles().getVehicleTypes().containsKey(carIdDiesel)) {
            addVehicleTypes(scenario);
        }

        VehicleType carBenzin = scenario.getVehicles().getVehicleTypes().get(carIdBenzin);
        VehicleType carDiesel = scenario.getVehicles().getVehicleTypes().get(carIdDiesel);

        // generate share of diesel cars
        Random randomGenerator = new Random();

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);

            //add petrol or diesel vehicles according to share
            double percent = randomGenerator.nextDouble();
            if (percent < shareDiesel) {
                Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, carDiesel);
                scenario.getVehicles().addVehicle(v);
            } else {
                Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, carBenzin);
                scenario.getVehicles().addVehicle(v);
            }

            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }


    }

}
