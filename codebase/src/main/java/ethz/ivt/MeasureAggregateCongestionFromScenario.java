package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionWriter;
import ethz.ivt.vsp.congestion.handlers.*;
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
    private int congestionHandlerVersion;
    private EventsManagerImpl eventsManager;
    private final MatsimEventsReader reader;
    private CongestionHandler congestionHandler;
    private CongestionAggregator congestionAggregator;

    public static void main(String[] args) throws IOException {
        String configPath = args[0];
        double binSize = Double.parseDouble(args[1]); // aggregation time bin size in seconds
        String eventFile = args[2];
        int congestionHandlerVersion = Integer.parseInt(args[3]);
        String outputDirectory = args[4];

        // load config file
        Config config = ConfigUtils.loadConfig(configPath, new EmissionsConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // process MATSim events and write congestion data to file
        MeasureAggregateCongestionFromScenario runner = new MeasureAggregateCongestionFromScenario(scenario, binSize, congestionHandlerVersion);
        runner.process(eventFile);
        runner.write(outputDirectory);
    }

    public MeasureAggregateCongestionFromScenario(Scenario scenario, double binSize, int congestionHandlerVersion) {
        this.scenario = scenario;
        this.binSize = binSize;
        this.congestionHandlerVersion = congestionHandlerVersion;

        // set up event manager
        this.eventsManager = new EventsManagerImpl();
        this.reader = new MatsimEventsReader(this.eventsManager);

        // add vehicle handler
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        this.eventsManager.addHandler(v2deh);

        // add congestion handler and aggregator
        switch (this.congestionHandlerVersion) {
            case 3:
                this.congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
                break;
//            case 4:
//                this.congestionHandler = new CongestionHandlerImplV4(eventsManager, scenario);
//                break;
            case 7:
                this.congestionHandler = new CongestionHandlerImplV7(eventsManager, scenario);
                break;
            case 8:
                this.congestionHandler = new CongestionHandlerImplV8(eventsManager, scenario);
                break;
            case 9:
                this.congestionHandler = new CongestionHandlerImplV9(eventsManager, scenario);
                break;
            case 10:
                this.congestionHandler = new CongestionHandlerImplV10(eventsManager, scenario);
                break;
            default:
                this.congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        }
        log.info("Selected " + this.congestionHandler.getClass().getName());
        this.congestionAggregator = new CongestionAggregator(scenario, v2deh, this.binSize);
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
