package ethz.ivt;

import ethz.ivt.externalities.MeasureExternalities;
import ethz.ivt.externalities.counters.ExternalityCostCalculator;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;


/**
 * Created by molloyj on 17.07.2017.
 */
public class MeasureExternalitiesFromTraceEvents {
    private final static Logger log = Logger.getLogger(MeasureExternalitiesFromTraceEvents.class);

    public static void main(String[] args) throws IOException {
        String configPath = args[0];
        String eventPath = args[1];
        String congestionPath = args[2];
        String costValuesPath = args[3];
        String vehicleCompositionPath = args[4];
        String outputPath = args[5];

        // load config file and scenario
        Config config = ConfigUtils.loadConfig(configPath, new EmissionsConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // set up vehicle composition from file
        VehicleGenerator vehicleGenerator = new VehicleGenerator(scenario);
        vehicleGenerator.read(vehicleCompositionPath, 2015);
        vehicleGenerator.setUpVehicles();

//        CSVVehicleWriter writer = new CSVVehicleWriter(scenario.getVehicles().getVehicles().values());
//        writer.write(outputPath + "vehicles.csv");

        // load data
        AggregateDataPerTimeImpl<Link> congestionData = CSVCongestionReader.forLink().read(congestionPath, 900.);
        ExternalityCostCalculator ecc = new ExternalityCostCalculator(costValuesPath);

        // create runner
        MeasureExternalities measureExternalities = new MeasureExternalities(scenario, congestionData, ecc);

        // process events and write to file
        measureExternalities.process(eventPath, LocalDateTime.now());
        measureExternalities.write(Paths.get(outputPath, LocalDateTime.now().toString(), "Switzerland"));
    }


}
