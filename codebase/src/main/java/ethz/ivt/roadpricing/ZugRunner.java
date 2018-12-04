package ethz.ivt.roadpricing;

import ethz.ivt.baseline.BlackListedTimeAllocationMutatorConfigGroup;
import ethz.ivt.baseline.BlackListedTimeAllocationMutatorStrategyModule;
import ethz.ivt.baseline.IVTBaselineScoringFunctionFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.roadpricing.*;


/**
 * Basic main for the ivt baseline scenarios.
 * <p>
 * Based on playground/ivt/teaching/RunZurichScenario.java by thibautd
 *
 * @author jmolloy, boescpa
 */

public class ZugRunner {


    public static void main(String[] args) {

        (new ZugRunner()).run(args);

    }


    /**
     * Basic main for the ivt baseline scenarios extended for road pricing.
     * <p>
     * Based on playground/ivt/teaching/RunZurichScenario.java by thibautd
     *
     * @author boescpa
     */

    public void run(String[] args) {
        // This allows to get a log file containing the log messages happening
        // before controler init.
        OutputDirectoryLogging.catchLogEntries();

        String baseDirectory = "C:\\Projects\\SCCER_project\\scenarios";

        String configFile = args.length > 0 ? args[0] : baseDirectory + "/defaultIVTConfig_25PrctZug.xml";


        // It is suggested to use the config created by playground/boescpa/baseline/ConfigCreator.java.
        final Config config = ConfigUtils.loadConfig(configFile,
                new BlackListedTimeAllocationMutatorConfigGroup(),
                new RoadPricingConfigGroup());


        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        final Scenario scenario = ScenarioUtils.loadScenario(config);


        String roadPricingConfigfile = baseDirectory + "/pricingSchemes/testTollScheme.xml";

        TollFactor tollfactor = PeakTollFactor.basicPeak(4);

        RoadPricingScheme scheme = new RoadPricingSchemeUsingTollFactor(roadPricingConfigfile, tollfactor);
        scheme.getTolledLinkIds().addAll(scenario.getNetwork().getLinks().keySet());
        RoadPricingModule roadPricingModule = new RoadPricingModule(scheme);

        final Controler controler = new Controler(scenario);

        //user vehicle selection:
        //household car allocation to maximise utility gain - socnet maximise household utility

        //1. select car randomly from household
            //car ownership from jäggi - why look at household car ownership, then include age, sex variables, and not #household members
            // its ok - household reference person should just be the oldest household member - or working male?
            // car generation - @link{P:\Projekte\OECD\matsim\Flottenmodel R\R_Scripts}
        //2. incorporate household car limit into selection?
        //3. maximise household utility gain from car allocation


        // We use a time allocation mutator that allows to exclude certain activities.
        controler.addOverridingModule(roadPricingModule);
        controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());
        // We use a specific scoring function, that uses individual preferences for activity durations.
        controler.setScoringFunctionFactory(
                new IVTBaselineScoringFunctionFactory(controler.getScenario(),
                        new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)));


        controler.run();
    }




}
