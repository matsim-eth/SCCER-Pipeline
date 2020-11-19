package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by molloyj on 06.10.2017.
 * Note: These aggregators could be converted to a decorator pattern,
 */
public class EmissionsAggregator implements WarmEmissionEventHandler, ColdEmissionEventHandler {
    private static final Logger log = Logger.getLogger(EmissionsAggregator.class);
    private AggregateDataPerTimeImpl<Link> aggregateEmissionsDataPerLinkPerTime;

    public EmissionsAggregator(int binSize, ArrayList<String> attributes) {
        this.aggregateEmissionsDataPerLinkPerTime = new AggregateDataPerTimeImpl<Link>(binSize, attributes, Link.class);
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateEmissionsDataPerLinkPerTime.getBinSize());
        Id<Link> linkId = event.getLinkId();

        Map<String, Double> pollutants = event.getColdEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();
            aggregateEmissionsDataPerLinkPerTime.addValueToTimeBin(linkId, timeBin, pollutant, p.getValue());
        }
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateEmissionsDataPerLinkPerTime.getBinSize());
        Id<Link> linkId = event.getLinkId();

        Map<String, Double> pollutants = event.getWarmEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();
            aggregateEmissionsDataPerLinkPerTime.addValueToTimeBin(linkId, timeBin, pollutant, p.getValue());
        }
    }

    public AggregateDataPerTimeImpl<Link> getAggregateEmissionsDataPerLinkPerTime() {
        return aggregateEmissionsDataPerLinkPerTime;
    }
}

