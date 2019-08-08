package ethz.ivt;

import ethz.ivt.externalities.MeasureExternalities;
import ethz.ivt.externalities.counters.ExternalityCostCalculator;
import ethz.ivt.externalities.counters.ExternalityCounter;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader;
import ethz.ivt.externalities.roadTypeMapping.HbefaRoadTypeMapping;
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Random;


/**
 * Created by molloyj on 17.07.2017.
 */
public class MeasureExternalitiesFromTraceEvents {
    private final static Logger log = Logger.getLogger(MeasureExternalitiesFromTraceEvents.class);

    public static void main(String[] args) throws IOException {
        String configPath = args[0];
        String eventPath = args[1];
        String congestionPath = args[2];
        double binSize = Double.valueOf(args[3]);
        String costValuesPath = args[4];
        String vehicleCompositionPath = args[5];
        String outputPath = args[6];

        // load config file and scenario
        Config config = ConfigUtils.loadConfig(configPath, new EmissionsConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // adding hbefa mappings
        log.info("Adding hbefa mappings");
        HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
        roadTypeMapping.addHbefaMappings(scenario.getNetwork());

        // set up vehicle composition from file
        VehicleGenerator vehicleGenerator = new VehicleGenerator(scenario);
        vehicleGenerator.read(vehicleCompositionPath, 2015);
        vehicleGenerator.setUpVehicles();

        CSVVehicleWriter writer = new CSVVehicleWriter(scenario.getVehicles().getVehicles().values());
        writer.write(Paths.get(outputPath, "externalities.csv"));

        // load data
        AggregateDataPerTimeImpl<Link> congestionData = CSVCongestionReader.forLink().read(congestionPath, binSize);
        ExternalityCostCalculator ecc = new ExternalityCostCalculator(costValuesPath);

        // create runner
        MeasureExternalities measureExternalities = new MeasureExternalities(scenario, congestionData, ecc);

        // process events and write to file
        ExternalityCounter externalityCounter = measureExternalities.process(eventPath, LocalDateTime.now());
        externalityCounter.writeCsvFile(Paths.get(outputPath, LocalDateTime.now().toString(), "externalities.csv"));
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
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC P Euro-4END_EMISSIONS");
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
