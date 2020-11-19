package ethz.ivt;

import ethz.ivt.externalities.aggregation.EmissionsAggregator;
import ethz.ivt.externalities.roadTypeMapping.HbefaRoadTypeMapping;
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MeasureAggregateEmissionsFromScenario {
    private final static Logger log = Logger.getLogger(MeasureExternalitiesFromTraceEvents.class);

    private final Scenario scenario;

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("config-path", "vehicle-path", "events-path", "binsize", "output-path")
                .build();

        // set up scenario
        String configPath = cmd.getOptionStrict("config-path");
        String vehicleCompositionPath = cmd.getOptionStrict("vehicle-path");

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

        // process events
        String eventsPath = cmd.getOptionStrict("events-path");
        int binSize = Integer.parseInt(cmd.getOptionStrict("binsize"));
        String outputPath = cmd.getOptionStrict("output-path");
        new MeasureAggregateEmissionsFromScenario(scenario).run(eventsPath, binSize, outputPath);
    }

    public MeasureAggregateEmissionsFromScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public void run(String eventsPath, int binSize, String outputPath) {

        // create event manager
        EventsManagerImpl eventsManager = new EventsManagerImpl();

        // add driver handler
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        // setup aggregators
        EmissionModule emissionModule = new EmissionModule(this.scenario, eventsManager);
        ArrayList<String> attributes = new ArrayList<>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));
        EmissionsAggregator emissionsAggregator = new EmissionsAggregator(binSize, attributes);
        emissionModule.getEmissionEventsManager().addHandler(emissionsAggregator);

        // read MATSim events
        MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
        reader.readFile(eventsPath);

        // save emissions data to csv files
        emissionsAggregator.getAggregateEmissionsDataPerLinkPerTime().writeDataToCsv(outputPath);

    }

}
