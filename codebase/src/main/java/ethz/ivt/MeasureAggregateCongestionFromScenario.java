package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

public class MeasureAggregateCongestionFromScenario {
	private final static Logger log = Logger.getLogger(MeasureAggregateCongestionFromScenario.class);

    private static String RUN_FOLDER;
	private static String CONFIG_FILE;
    private static String EVENTS_FILE;
    
    private Config config;
    private EventsManagerImpl eventsManager;
    protected int bin_size_s = 3600;

    public static void main(String[] args) {
        RUN_FOLDER = args[0];
        CONFIG_FILE = args[1];
        EVENTS_FILE = args[2];

        new MeasureAggregateCongestionFromScenario().run();
    }
    
    public void run() {

    	// set up config
    	config = ConfigUtils.loadConfig(RUN_FOLDER + CONFIG_FILE, new EmissionsConfigGroup(), new NoiseConfigGroup());
        config.controler().setOutputDirectory(RUN_FOLDER + "aggregate/");
        Scenario scenario = ScenarioUtils.loadScenario(config);

    	// set up event manager and handlers
    	eventsManager = new EventsManagerImpl();

        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        // read through MATSim events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(RUN_FOLDER + EVENTS_FILE);

        // save congestion data to single csv file
        congestionAggregator.aggregateCongestionDataPerLinkPerTime.writeDataToCsv(config.controler().getOutputDirectory() + "congestion/");
        congestionAggregator.aggregateCongestionDataPerPersonPerTime.writeDataToCsv(config.controler().getOutputDirectory() + "congestion/");
        log.info("Congestion calculation completed.");
        log.info("Total delay : " + congestionHandler.getTotalDelay());
        log.info("Total internalized delay : " + congestionHandler.getTotalInternalizedDelay());

        eventsManager.finishProcessing();
    }

}
