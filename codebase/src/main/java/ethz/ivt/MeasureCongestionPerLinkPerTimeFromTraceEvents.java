package ethz.ivt;

import ethz.ivt.externalities.counters.CongestionPerLinkCounter;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;


/**
 * Created by tchervec on 19.11.2020.
 */
public class MeasureCongestionPerLinkPerTimeFromTraceEvents {
    private final static Logger log = Logger.getLogger(MeasureCongestionPerLinkPerTimeFromTraceEvents.class);

    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("config-path", "events-path", "congestion-path", "binsize", "output-path")
                .build();

        String configPath = cmd.getOptionStrict("config-path");
        String eventPath = cmd.getOptionStrict("events-path");
        String congestionPath = cmd.getOptionStrict("congestion-path");
        int binSize = Integer.parseInt(cmd.getOptionStrict("binsize"));
        String outputPath = cmd.getOptionStrict("output-path");

        // load config file and scenario
        Config config = ConfigUtils.loadConfig(configPath, new EmissionsConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // load data
        AggregateDataPerTimeImpl<Link> congestionData = CSVCongestionReader.forLink().read(congestionPath, binSize);

        // create events manager
        EventsManagerImpl eventsManager = new EventsManagerImpl();

        // add handler
        CongestionPerLinkCounter congestionCounter = new CongestionPerLinkCounter(scenario, congestionData, binSize);
        eventsManager.addHandler(congestionCounter);

        // process events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(eventPath);

        // write output
        congestionCounter.write(outputPath);
    }
}
