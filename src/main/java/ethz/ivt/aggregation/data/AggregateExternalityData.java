package ethz.ivt.aggregation.data;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;

public abstract class AggregateExternalityData {
    protected static final Logger log = Logger.getLogger(AggregateExternalityData.class);
    protected double bin_size;
    protected int num_bins;
    protected Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();

    // constructor
    public AggregateExternalityData(Scenario scenario, double bin_size, int num_bins) {
        this.bin_size = bin_size;
        this.num_bins = num_bins;
        setUpBinsForLinks(scenario);
    }

    // getters
    public double getBin_size() {
        return bin_size;
    }

    public int getNum_bins() {
        return num_bins;
    }

    public Map<Id<Link>, Map<String, double[]>> getLinkId2timeBin2values() {
        return linkId2timeBin2values;
    }

    // setup
    private void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {

            linkId2timeBin2values.put(l, new HashMap<>());
            linkId2timeBin2values.get(l).putIfAbsent("count", new double[num_bins]);
            linkId2timeBin2values.get(l).putIfAbsent("value", new double[num_bins]);
        });
    }

    // get values
    public double getCount(Id<Link> lid, int timeBin) {
        if (timeBin > num_bins) {
            return 0.0;
        }
        return this.linkId2timeBin2values.get(lid).get("count")[timeBin];
    }

    public double getValue(Id<Link> lid, int timeBin) {
        if (timeBin > num_bins) {
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
