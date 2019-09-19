package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionWriter;
import ethz.ivt.vsp.congestion.handlers.*;
import org.apache.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunAggregateCongestionInSimulation {

    public static void main(String[] args) throws ConfigurationException{
        Logger log = Logger.getLogger(RunAggregateCongestionInSimulation.class);

        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "binSize", "congestionHandlerVersion", "outputDirectory") //
                .build();

        // load config file
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), SwitzerlandConfigurator.getConfigGroups());

        // scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        SwitzerlandConfigurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        SwitzerlandConfigurator.adjustScenario(scenario);

        // create controller
        Controler controller = new Controler(scenario);
        SwitzerlandConfigurator.configureController(controller);
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new SwissModeChoiceModule(cmd));

        // get other command line args
        double binSize = Double.parseDouble(cmd.getOptionStrict("binSize")); // aggregation time bin size in seconds
        int congestionHandlerVersion = Integer.parseInt(cmd.getOptionStrict("congestionHandlerVersion"));
        String outputDirectory = cmd.getOptionStrict("outputDirectory");

        // create event handlers
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();

        CongestionHandler congestionHandler;
        switch (congestionHandlerVersion) {
            case 7:
                congestionHandler = new CongestionHandlerImplV7(controller.getEvents(), scenario);
                break;
            case 8:
                congestionHandler = new CongestionHandlerImplV8(controller.getEvents(), scenario);
                break;
            case 9:
                congestionHandler = new CongestionHandlerImplV9(controller.getEvents(), scenario);
                break;
            case 10:
                congestionHandler = new CongestionHandlerImplV10(controller.getEvents(), scenario);
                break;
            default:
                congestionHandler = new CongestionHandlerImplV3(controller.getEvents(), scenario);
        }
        log.info("Selected " + congestionHandler.getClass().getName());

        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh, binSize);

        // add handlers to controller
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(v2deh);
                addEventHandlerBinding().toInstance(congestionHandler);
                addEventHandlerBinding().toInstance(congestionAggregator);
            }
        });

        // add writer to controller
        controller.addControlerListener(new IterationEndsListener() {
            @Override
            public void notifyIterationEnds(IterationEndsEvent event) {

                int iteration = event.getIteration();

                CSVCongestionWriter.forLink().write(congestionAggregator.getAggregateCongestionDataPerLinkPerTime(),
                        outputDirectory + "/it" + iteration + ".aggregate_delay_per_link_per_time.csv");
            }
        });

        controller.run();
    }
}


