package ethz.ivt;

import ethz.ivt.externalities.aggregation.CongestionAggregatorV2;
import ethz.ivt.externalities.data.AggregateDataPerLinkPerTimeImplV2;
import ethz.ivt.vsp.congestion.handlers.*;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.log4j.Logger;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;

public class RunAggregateCongestionInSimulation {

    public static void main(String[] args) throws ConfigurationException{
        Logger log = Logger.getLogger(RunAggregateCongestionInSimulation.class);

        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "binSize", "congestionHandlerVersion", "outputDirectory") //
                .allowPrefixes("mode-parameter", "cost-parameter") //
                .build();

        // config
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
                SwitzerlandConfigurator.getConfigGroups());
        cmd.applyConfiguration(config);

        // scenario
        Scenario scenario = ScenarioUtils.createScenario(config);

        SwitzerlandConfigurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        SwitzerlandConfigurator.adjustScenario(scenario);

        // controller
        Controler controller = new Controler(scenario);
        SwitzerlandConfigurator.configureController(controller);
        controller.addOverridingModule(new EqasimAnalysisModule());
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

        CongestionAggregatorV2 congestionAggregator = new CongestionAggregatorV2(scenario, v2deh, binSize);

        // add handlers to controller
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(v2deh);
                addEventHandlerBinding().toInstance(congestionHandler);
                addEventHandlerBinding().toInstance(congestionAggregator);
            }
        });


        // some numbers
        int numIterations = controller.getConfig().controler().getLastIteration();
        int numBins = congestionAggregator.getAggregateCongestionDataPerLinkPerTime().getNumBins();

        // startup listener
        controller.addControlerListener(new StartupListener() {
            @Override
            public void notifyStartup(StartupEvent event) {
                for (Link link : controller.getScenario().getNetwork().getLinks().values()) {
                    double[][] array = new double[numBins][numIterations + 1];
                    link.getAttributes().putAttribute("mean_caused_congestion", array);
                }
            }
        });

        // add mean congestion caused as network attribute
        controller.addControlerListener(new IterationEndsListener() {
            @Override
            public void notifyIterationEnds(IterationEndsEvent event) {

                int iteration = event.getIteration();

                AggregateDataPerLinkPerTimeImplV2 rawCongestionData = congestionAggregator.getAggregateCongestionDataPerLinkPerTime();

                for (Id<Link> linkId : rawCongestionData.getData().keySet()) {

                    double[][] data = (double[][]) controller.getScenario().getNetwork().getLinks().get(linkId).getAttributes().getAttribute("mean_caused_congestion");

                    for (int bin = 0; bin < rawCongestionData.getNumBins(); bin++) {

                        double countEntering = rawCongestionData.getValueInTimeBin(linkId, bin, "count_entering");
                        double congestionCaused = rawCongestionData.getValueInTimeBin(linkId, bin, "congestion_caused");

                        if (countEntering == 0.0) {
                            data[bin][iteration] = 0.0;
                        }
                        else {
                            data[bin][iteration] = congestionCaused / countEntering;
                        }
                    }
                }
            }
        });

        // write out values to csv at the end of simulation
        controller.addControlerListener(new ShutdownListener() {
            @Override
            public void notifyShutdown(ShutdownEvent event) {

                File file = new File(outputDirectory + "/aggregate_delay_per_link_per_time.csv");
                File parentFile = file.getParentFile();
                parentFile.mkdirs();

                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));

                    String header = String.join(";",
                            new String[] { "link_id", "binSize", "timebin",
                                    "mean", "geometric_mean",
                                    "percentile_50", "percentile_75", "percentile_90",
                                    "min", "max", "variance" });

                    bw.write(header);
                    bw.newLine();

                    for (Link link : controller.getScenario().getNetwork().getLinks().values()) {
                        double[][] data = (double[][]) link.getAttributes().getAttribute("mean_caused_congestion");

                        for (int bin = 0; bin < numBins; bin++) {

                            double[] dataThisBin = data[bin];

                            String entry = String.join(";", new String[] {
                                    link.getId().toString(),
                                    String.valueOf(binSize),
                                    String.valueOf(bin),
                                    String.valueOf(StatUtils.mean(dataThisBin)),
                                    String.valueOf(StatUtils.geometricMean(dataThisBin)),
                                    String.valueOf(StatUtils.percentile(dataThisBin, 50)),
                                    String.valueOf(StatUtils.percentile(dataThisBin, 75)),
                                    String.valueOf(StatUtils.percentile(dataThisBin, 90)),
                                    String.valueOf(StatUtils.min(dataThisBin)),
                                    String.valueOf(StatUtils.max(dataThisBin)),
                                    String.valueOf(StatUtils.variance(dataThisBin)),
                            });

                            bw.write(entry);
                            bw.newLine();

                        }

                    }

                    bw.close();
                    log.info("Output written to " + outputDirectory);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        controller.run();
    }

}


