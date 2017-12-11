package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.externalities.data.AggregateCongestionData;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.handler.LinkSpeedCalculation;
import org.matsim.contrib.noise.handler.NoiseTimeTracker;
import org.matsim.contrib.noise.handler.PersonActivityTracker;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
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

public class MeasureAggregateNoiseFromScenario {
	private final static Logger log = Logger.getLogger(MeasureAggregateNoiseFromScenario.class);

    private static String RUN_FOLDER; // = "/home/ctchervenkov/Documents/projects/road_pricing/zurich_1pc/scenario/";
	private static String CONFIG_FILE; // = "defaultIVTConfig_w_emissions.xml";
    private static String EVENTS_FILE; // = "800.events.xml.gz";
    
    private Config config;
    private EventsManagerImpl eventsManager;
    protected int bin_size_s = 3600;

    private NoiseTimeTracker noiseTimeTracker;

    public static void main(String[] args) {
        RUN_FOLDER = args[0];
        CONFIG_FILE = args[1];
        EVENTS_FILE = args[2];
        new MeasureAggregateNoiseFromScenario().run();
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

        setUpVehicles(scenario);

        // read through MATSim events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        noiseTimeTracker.computeFinalTimeIntervals();
        noiseTimeTracker.reset(0); // clear all internal data

        log.info("Noise calculation completed.");
        eventsManager.finishProcessing();

        // save noise emissions to single csv file
        final String[] labels = { "marginal_damages_link_car"};
        final String[] workingDirectories = {config.controler().getOutputDirectory() + "/noise/" + "/marginal_damages_link_car/"};

        MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
        merger.setNetworkFile(RUN_FOLDER + "network_zurich_w_types.xml");
        merger.setOutputDirectory(config.controler().getOutputDirectory() + "/noise/");
        merger.setStartTime(1.*3600.);
        merger.setEndTime(30.*3600.);
        merger.setTimeBinSize(3600.);
        merger.setOutputFormat(MergeNoiseCSVFile.OutputFormat.xyt1t2t3etc);
        merger.setWorkingDirectory(workingDirectories);
        merger.setLabel(labels);
        merger.run();
    }
    
    public void setUpNoise(Scenario scenario) { //taken from NoiseOfflineCalculation

        NoiseConfigGroup noiseParameters = (NoiseConfigGroup) scenario.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);

        noiseParameters.setTimeBinSizeNoiseComputation(this.bin_size_s);
        noiseParameters.setInternalizeNoiseDamages(false);
        noiseParameters.setComputeNoiseDamages(true);
        noiseParameters.setComputeAvgNoiseCostPerLinkAndTime(true);
        noiseParameters.setComputePopulationUnits(true);
        noiseParameters.setComputeCausingAgents(true);
        noiseParameters.setUseActualSpeedLevel(true);

        // Set to '1.' for a 100 percent sample size. Set to '10.' for a 10 percent sample size. Set to '100.' for a 1 percent sample size.
        noiseParameters.setScaleFactor(100.);

        // set parameter values to same as Kaddoura et al. 2017
        noiseParameters.setAnnualCostRate(63.3); // 63.3 EUR i.e. 85 DEM * 0.51129 * 1.02 ^ (2014-1995)
//        noiseContext.getNoiseParams().setAnnualCostRate(78.6); // 78.6 CHF i.e. 85 DEM * 0.59827 * 1.02 ^ (2017-1995) ??

//        double numberReceiverPoints = 2000000;
//        double gap = computeMinimalGap(scenario, numberReceiverPoints);

        noiseParameters.setReceiverPointGap(500.); // gap
//        noiseParameters.setRelevantRadius(500.); // 500 m
        noiseParameters.setRelevantRadius(Math.sqrt(2) / 2 * noiseParameters.getReceiverPointGap()); // sqrt(2)/2 * gap

        String[] desiredActivities = {"home","work","remote_work"};

        String[] consideredActivities = getValidActivitiesOfType(scenario, desiredActivities);
        noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivities);
        noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivities);

        NoiseContext noiseContext = new NoiseContext(scenario);

//        NoiseTimeTracker noiseTimeTracker = new NoiseTimeTracker();
        noiseTimeTracker = new NoiseTimeTracker();
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

//    private double computeMinimalGap(Scenario scenario, double numberReceiverPoints) {
//        double xCoordMinLinkNode = Double.MAX_VALUE;
//        double yCoordMinLinkNode = Double.MAX_VALUE;
//        double xCoordMaxLinkNode = Double.MIN_VALUE;
//        double yCoordMaxLinkNode = Double.MIN_VALUE;
//
//        for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
//            if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX()) < xCoordMinLinkNode) {
//                xCoordMinLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
//            }
//            if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY()) < yCoordMinLinkNode) {
//                yCoordMinLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
//            }
//            if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX()) > xCoordMaxLinkNode) {
//                xCoordMaxLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
//            }
//            if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY()) > yCoordMaxLinkNode) {
//                yCoordMaxLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
//            }
//        }
//
//        return Math.sqrt( (xCoordMaxLinkNode - xCoordMinLinkNode) * (yCoordMaxLinkNode - yCoordMinLinkNode) / numberReceiverPoints);
//    }

}
