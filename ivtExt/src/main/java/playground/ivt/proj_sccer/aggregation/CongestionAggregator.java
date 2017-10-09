package playground.ivt.proj_sccer.aggregation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import playground.ivt.proj_sccer.vsp.CongestionEvent;
import playground.ivt.proj_sccer.vsp.handlers.CongestionEventHandler;

import java.util.*;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator extends EventAggregator implements CongestionEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);

    private List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

    private double vtts_car;
    private double congestionTollFactor;

    public CongestionAggregator(Scenario scenario, double congestionTollFactor, double binSize_s) {
        super(scenario, binSize_s);
        this.vtts_car = (scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
        this.congestionTollFactor = congestionTollFactor;
        log.info("VTTS_car: " + vtts_car);
        log.info("Congestion toll factor: " + congestionTollFactor);

    }

    public CongestionAggregator(Scenario scenario, int binSize_s) {
        this(scenario, 1.0, binSize_s);
    }

    @Override
    public void reset(int iteration) {
        this.congestionEvents.clear();
        super.reset(iteration);
    }

    @Override
    public void handleEvent(CongestionEvent event) {

        //this.congestionEvents.add(event);
        int bin = getTimeBin(event.getEmergenceTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("delay", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("delay")[bin] += event.getDelay();

    }


}
