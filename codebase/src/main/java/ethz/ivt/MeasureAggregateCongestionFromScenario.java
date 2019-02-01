package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.externalities.data.congestion.writer.CSVCongestionPerLinkPerTimeWriter;
import ethz.ivt.externalities.data.congestion.writer.CSVCongestionPerPersonPerTimeWriter;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

public class MeasureAggregateCongestionFromScenario {
	private final static Logger log = Logger.getLogger(MeasureAggregateCongestionFromScenario.class);

	private String configFile;
    private String eventFile;
    private String outputDirectory;
    
    private Config config;
    private EventsManagerImpl eventsManager;
    protected int bin_size_s = 3600;

    public static void main(String[] args) throws IOException {
        new MeasureAggregateCongestionFromScenario(args[0], args[1], args[2]).run();
    }

    public MeasureAggregateCongestionFromScenario(String configFile, String eventFile, String outputDirectory) {
        this.configFile = configFile;
        this.eventFile = eventFile;
        this.outputDirectory = outputDirectory;
    }

    public void run() throws IOException {
    	// set up config
    	config = ConfigUtils.loadConfig(this.configFile, new EmissionsConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

    	// set up event manager and handlers
    	eventsManager = new EventsManagerImpl();

        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh, bin_size_s);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        // read through MATSim events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(this.eventFile);

        // save congestion data to single csv file
        new CSVCongestionPerLinkPerTimeWriter(congestionAggregator.getAggregateCongestionDataPerLinkPerTime())
                .write(outputDirectory + "/aggregate_delay_per_link_per_time.csv");
        new CSVCongestionPerPersonPerTimeWriter(congestionAggregator.getAggregateCongestionDataPerPersonPerTime())
                .write(outputDirectory + "/aggregate_delay_per_person_per_time.csv");

        log.info("Congestion calculation completed.");
        log.info("Total delay : " + congestionHandler.getTotalDelay());
        log.info("Total internalized delay : " + congestionHandler.getTotalInternalizedDelay());

        eventsManager.finishProcessing();
    }

}
