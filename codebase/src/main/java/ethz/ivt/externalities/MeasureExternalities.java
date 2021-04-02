package ethz.ivt.externalities;

import ethz.ivt.externalities.counters.*;
import ethz.ivt.externalities.data.AggregateDataPerTime;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.JITVehicleCreator;
import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import ethz.ivt.externalities.data.congestion.PtChargingZones;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Created by molloyj on 17.07.2017.
 */
public class MeasureExternalities implements MeasureExternalitiesInterface {
    private final static Logger log = Logger.getLogger(MeasureExternalities.class);
    private final MatsimEventsReader reader;
    private final Scenario scenario;
    private final ExternalityCostCalculator ecc;


    private EventsManagerImpl eventsManager;
    private ExternalityCounter externalityCounter;

    public MeasureExternalities(
            Scenario scenario,
            AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime,
            ExternalityCostCalculator ecc,
            PtChargingZones ptChargingZones) {

        this.scenario = scenario;
        this.ecc = ecc;

        eventsManager = new EventsManagerImpl();
        eventsManager.addHandler(new JITVehicleCreator(scenario));

        reader = new MatsimEventsReader(eventsManager);

        log.info("load emissions module");
        // setup externality counters
        EmissionsConfigGroup ecg = (EmissionsConfigGroup) scenario.getConfig().getModules().get(EmissionsConfigGroup.GROUP_NAME);
    //    ecg.setUsingDetailedEmissionCalculation(false);

        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);
        externalityCounter = new ExternalityCounter(scenario, eventsManager);

        CarExternalityCounter carExternalityCounter = new CarExternalityCounter(scenario, externalityCounter);
        eventsManager.addHandler(carExternalityCounter);

        AutobahnSplitCounter autobahnSplitCounter = new AutobahnSplitCounter(scenario, externalityCounter);
        eventsManager.addHandler(autobahnSplitCounter);

        CongestionCounter congestionCounter = new CongestionCounter(scenario, aggregateCongestionDataPerLinkPerTime, externalityCounter);
        eventsManager.addHandler(congestionCounter);

        PTCongestionCounter ptCongestionCounter = new PTCongestionCounter(scenario, externalityCounter, ptChargingZones);
        eventsManager.addHandler(ptCongestionCounter);

        eventsManager.addHandler(externalityCounter);

    }

    @Override
    public void reset() {
        eventsManager.resetHandlers(0);
    }

    @Override
    public ExternalityCounter process(List<Event> events, LocalDateTime date) {
        this.reset();

        externalityCounter.setDate(date);
        eventsManager.initProcessing();
        events.forEach(eventsManager::processEvent);

        ecc.addCosts(externalityCounter);

        eventsManager.finishProcessing();
        ExternalityCounter ecCopy = externalityCounter.copy();
        eventsManager.resetHandlers(0);
        return ecCopy;

    }

    @Override
    public ExternalityCounter process(String events, LocalDateTime date) {
        externalityCounter.setDate(date);
        eventsManager.initProcessing();
        reader.readFile(events);

        ecc.addCosts(externalityCounter);

        eventsManager.finishProcessing();
        ExternalityCounter ecCopy = externalityCounter.copy();
        eventsManager.resetHandlers(0);
        return ecCopy;

    }

    @Override
    public void write(Path outputFolder) {
        externalityCounter.writeCsvFile(outputFolder);
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
