package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionWriter;
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
	private Scenario scenario;
    private double binSize;
    private EventsManagerImpl eventsManager;
    private final MatsimEventsReader reader;
    private CongestionHandler congestionHandler;
    private CongestionAggregator congestionAggregator;
    private double congestionThresholdRatio;

    public static void main(String[] args) throws IOException {
        String configPath = args[0];
        String eventFile = args[1];
        String outputDirectory = args[2];
        double binSize = Double.parseDouble(args[3]); // aggregation time bin size in seconds
        double congestionThresholdRatio = Double.parseDouble(args[4]); // ratio of freeflow speed under which it is considered congestion
//        double congestionThresholdRatio = 0.65

        // load config file
        Config config = ConfigUtils.loadConfig(configPath);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // process MATSim events and write congestion data to file
        MeasureAggregateCongestionFromScenario runner = new MeasureAggregateCongestionFromScenario(scenario, binSize, congestionThresholdRatio);
        runner.process(eventFile);
        runner.write(outputDirectory);
    }

    public MeasureAggregateCongestionFromScenario(Scenario scenario, double binSize, double congestionThresholdRatio) {
        this.scenario = scenario;
        this.binSize = binSize;
        this.congestionThresholdRatio = congestionThresholdRatio;

        // set up event manager
        this.eventsManager = new EventsManagerImpl();
        this.reader = new MatsimEventsReader(this.eventsManager);

        // add vehicle handler
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        this.eventsManager.addHandler(v2deh);

        // add congestion handler and aggregator
        this.congestionHandler = new CongestionHandlerImplV3(this.eventsManager, scenario, v2deh);
        this.congestionAggregator = new CongestionAggregator(scenario, v2deh, this.binSize, this.congestionThresholdRatio);
        this.eventsManager.addHandler(congestionHandler);
        this.eventsManager.addHandler(congestionAggregator);
    }

    public void process(String eventFile) {
        // read through MATSim events
        this.eventsManager.initProcessing();
        this.reader.readFile(eventFile);

        log.info("Congestion calculation completed.");
        log.info("Total delay : " + this.congestionHandler.getTotalDelay());
        log.info("Total internalized delay : " + this.congestionHandler.getTotalInternalizedDelay());

        this.eventsManager.finishProcessing();
    }

    public void write(String outputDirectory) throws IOException {
        // save congestion data to csv files
        CSVCongestionWriter.forLink().write(this.congestionAggregator.getAggregateCongestionDataPerLinkPerTime(),
                outputDirectory + "/aggregate_delay_per_link_per_time.csv");
        CSVCongestionWriter.forPerson().write(this.congestionAggregator.getAggregateCongestionDataPerPersonPerTime(),
                outputDirectory + "/aggregate_delay_per_person_per_time.csv");
    }

}
