package playground.jmolloy.externalitiesAnalysis.aggregation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;

import java.util.Map;

/**
 * Created by molloyj on 06.10.2017.
 * Note: These aggregators could be converted to a decorator pattern,
 */
public class EmissionsAggregator extends EventAggregator implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    public EmissionsAggregator(Scenario scenario, double congestionTollFactor, double binSize_s) {
        super(scenario, binSize_s);
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        int bin = getTimeBin(event.getTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);
        Map<ColdPollutant, Double> pollutants = event.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent(pollutant, new double[num_bins]);
            this.linkId2timeBin2values.get(event.getLinkId()).get(pollutant)[bin] += p.getValue();
        }
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("ColdEmissionsCount", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("ColdEmissionsCount")[bin]++;


    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        int bin = getTimeBin(event.getTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);
        Map<WarmPollutant, Double> pollutants = event.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent(pollutant, new double[num_bins]);
            this.linkId2timeBin2values.get(event.getLinkId()).get(pollutant)[bin] += p.getValue();
        }
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("WarmEmissionsCount", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("WarmEmissionsCount")[bin]++;
    }
}

