package ethz.ivt.aggregation.data;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;

public abstract class AggregateExternalityData {
    protected static final Logger log = Logger.getLogger(AggregateExternalityData.class);
    protected double binSize;
    protected int numBins;
    protected Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();

    // constructor
    public AggregateExternalityData(Scenario scenario, double binSize) {
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
        setUpBinsForLinks(scenario);
    }

    // getters
    public double getBinSize() {
        return binSize;
    }

    public int getNumBins() {
        return numBins;
    }

    public Map<Id<Link>, Map<String, double[]>> getLinkId2timeBin2values() {
        return linkId2timeBin2values;
    }

    // setup
    private void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {

            linkId2timeBin2values.put(l, new HashMap<>());
            linkId2timeBin2values.get(l).putIfAbsent("count", new double[numBins]);
            linkId2timeBin2values.get(l).putIfAbsent("value", new double[numBins]);
        });
    }

    // get values
    public double getCount(Id<Link> lid, int timeBin) {
        if (timeBin > numBins) {
            return 0.0;
        }
        return this.linkId2timeBin2values.get(lid).get("count")[timeBin];
    }

    public double getValue(Id<Link> lid, int timeBin) {
        if (timeBin > numBins) {
            return 0.0;
        }
        return this.linkId2timeBin2values.get(lid).get("value")[timeBin];
    }

    public double getMeanValue(Id<Link> lid, int timeBin) {
        double count = getCount(lid, timeBin);
        if (count == 0.0) {
            return 0.0;
        }
        double value = getValue(lid, timeBin);
        return value / count;
    }

    // import/export csv methods
    public abstract void loadDataFromCsv(String outputPath);
    public abstract void writeDataToCsv(String outputPath);

}
