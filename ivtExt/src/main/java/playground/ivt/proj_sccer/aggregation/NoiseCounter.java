package playground.ivt.proj_sccer.aggregation;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

public class NoiseCounter extends ExternalityCounter {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
    protected Map<Id<Link>, double[]> linkId2timeBin2values = new HashMap<>();

	protected final int num_bins;
	protected int binSize_s;

	public NoiseCounter(Scenario scenario, Vehicle2DriverEventHandler drivers, int binSize_s) {
    	super(scenario, drivers);

        this.num_bins = (int) (30 * 3600 / binSize_s);
        this.binSize_s = binSize_s;

        setUpBinsForLinks(scenario);
        log.info("Number of congestion bins: " + num_bins);
    }

	private void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            linkId2timeBin2values.put(l, new double[num_bins]);
        });
	}
}
