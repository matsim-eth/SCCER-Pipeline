package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.externalities.data.AggregateCongestionData;
import ethz.ivt.externalities.data.AggregateNoiseData;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

public class MeasureAggregateExternalities {
	private final static Logger log = Logger.getLogger(MeasureAggregateExternalities.class);

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
        new MeasureAggregateExternalities().run();
    }
    
    public void run() {

    	// set up config
    	config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE, new EmissionsConfigGroup(), new NoiseConfigGroup());
        config.controler().setOutputDirectory(RUN_FOLDER + "aggregate/");
        Scenario scenario = ScenarioUtils.loadScenario(config);

    	// set up event manager and handlers
    	eventsManager = new EventsManagerImpl();
        setUpNoise(scenario);

        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        AggregateCongestionData aggregateCongestionData = new AggregateCongestionData(scenario, bin_size_s);
        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh, aggregateCongestionData);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        setUpVehicles(scenario);

        // read through MATSim events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        // save noise emissions to single csv file
        AggregateNoiseData aggregateNoiseData = new AggregateNoiseData(scenario, bin_size_s);
        aggregateNoiseData.computeLinkId2timeBin2valuesFromEmissionFiles(config.controler().getOutputDirectory() + "noise/emissions/");
        aggregateNoiseData.writeDataToCsv(config.controler().getOutputDirectory() + "noise/");
        log.info("Noise calculation completed.");

        // save congestion data to single csv file
        aggregateCongestionData.writeDataToCsv(config.controler().getOutputDirectory() + "congestion/");
        log.info("Congestion calculation completed.");

        eventsManager.finishProcessing();
    }
    
    public void setUpNoise(Scenario scenario) { //taken from NoiseOfflineCalculation

        NoiseConfigGroup noiseParameters = (NoiseConfigGroup) scenario.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);

        noiseParameters.setTimeBinSizeNoiseComputation(this.bin_size_s);
        noiseParameters.setInternalizeNoiseDamages(true);
        noiseParameters.setComputeNoiseDamages(true);
        noiseParameters.setComputeAvgNoiseCostPerLinkAndTime(true);
        noiseParameters.setComputePopulationUnits(false);
        noiseParameters.setComputeCausingAgents(true);

        // set parameter values to same as Kaddoura et al. 2017
        noiseParameters.setAnnualCostRate(63.3); // 63.3 EUR i.e. 85 DEM * 0.51129 * 1.02 ^ (2014-1995)
//        noiseContext.getNoiseParams().setAnnualCostRate(78.6); // 78.6 CHF i.e. 85 DEM * 0.59827 * 1.02 ^ (2017-1995) ??
        noiseParameters.setReceiverPointGap(500); // 50 m
        noiseParameters.setRelevantRadius(50); // 500 m

        String[] desiredActivities = {"home","work","remote_work"};

        String[] consideredActivities = getValidActivitiesOfType(scenario, desiredActivities);
        noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivities);
        noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivities);

        NoiseContext noiseContext = new NoiseContext(scenario);

        NoiseTimeTracker noiseTimeTracker = new NoiseTimeTracker();
        noiseTimeTracker.setNoiseContext(noiseContext);
        noiseTimeTracker.setEvents(eventsManager);
        noiseTimeTracker.setOutputFilePath(config.controler().getOutputDirectory() + "noise/");

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

    private String[] getValidActivitiesOfType(Scenario scenario, String[] desiredActTypes) {
        Set<String> setValidActivities = new HashSet<>();

        for (Person person: scenario.getPopulation().getPersons().values()) {

            for (PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;

                    if (!activity.getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {

                        for (String desiredActType : desiredActTypes) {
                            if(activity.getType().contains(desiredActType)) {
                                setValidActivities.add(activity.getType());
                            }
                        }
                    }
                }
            }
        }
        String[] validActivities = new String[setValidActivities.size()];
        return setValidActivities.toArray(validActivities);
    }

}
