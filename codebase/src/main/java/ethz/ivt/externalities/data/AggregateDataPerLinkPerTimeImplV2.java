package ethz.ivt.externalities.data;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AggregateDataPerLinkPerTimeImplV2 {
    protected static final Logger log = Logger.getLogger(AggregateDataPerLinkPerTimeImplV2.class);
    private double binSize;
    private int numBins;
    private int numLinks;
    private ArrayList<String> attributes = new ArrayList<>();
    Map<Id<Link>, Map<String, double[]>> aggregateDataPerLinkPerTime = new HashMap<>();

    public AggregateDataPerLinkPerTimeImplV2(Scenario scenario, double binSize) {
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
        this.attributes.add("count_entering");
        this.attributes.add("count_exiting");
        this.attributes.add("delay_caused");
        this.attributes.add("delay_experienced");
        this.attributes.add("congestion_caused");
        this.attributes.add("congestion_experienced");
    }

    public void reset() {
        this.aggregateDataPerLinkPerTime.clear();
    }

    // getters
    public int getNumBins() {
        return numBins;
    }

    public double getBinSize() {
        return binSize;
    }

    public Map<Id<Link>, Map<String, double[]>> getData() {
        return aggregateDataPerLinkPerTime;
    }

    public double getValueAtTime(Id<Link> id, double time, String attribute) {
        int timeBin = getTimeBin(time);
        return getValueInTimeBin(id, timeBin, attribute);
    }

    public double getValueInTimeBin(Id<Link> id, int timeBin, String attribute) {
        if (timeBin >= this.numBins) {
//            log.warn("Time bin must be < " + this.numBins + ". Returning 0.");
            return 0.0;
        }
        if (aggregateDataPerLinkPerTime.containsKey(id)) {
            if (aggregateDataPerLinkPerTime.get(id).containsKey(attribute)) {
                return aggregateDataPerLinkPerTime.get(id).get(attribute)[timeBin];
            } else {
//                log.warn("Attribute " + attribute + " is not valid. Returning 0.");
            }
            return 0.0;
        } else {
//            log.debug("No value for " + id + ", returning 0");
            return 0.0;

        }
    }

    public void setValueAtTime(Id<Link> id, double time, String attribute, double value) {
        int timeBin = getTimeBin(time);
        setValueForTimeBin(id, timeBin, attribute, value);
    }

    public void setValueForTimeBin(Id<Link> id, int timeBin, String attribute, double value) {
        if (timeBin >= this.numBins) {
//            log.warn("Time bin must be < " + this.numBins + ". No value set.");
            return;
        }
        setUpTimeBins(id);
        if (aggregateDataPerLinkPerTime.containsKey(id)) {
            if (aggregateDataPerLinkPerTime.get(id).containsKey(attribute)) {
                aggregateDataPerLinkPerTime.get(id).get(attribute)[timeBin] = value;
                return;
            } else {
//                log.warn("Attribute " + attribute + " is not valid. No value set.");
            }return;
        } else {
//            log.error("Id " + id + " is not valid. A value should have been set");
        }
    }

    public void addValueAtTime(Id<Link> id, double time, String attribute, double value) {
        int timeBin = getTimeBin(time);
        addValueToTimeBin(id, timeBin, attribute, value);
    }

    public void addValueToTimeBin(Id<Link> id, int timeBin, String attribute, double value) {
        double oldValue = getValueInTimeBin(id, timeBin, attribute);
        double newValue = oldValue + value;
        setValueForTimeBin(id, timeBin, attribute, newValue);
    }


    // setup
    private void setUpTimeBins(Id<Link> id) {
        aggregateDataPerLinkPerTime.computeIfAbsent(id, a -> {
            HashMap<String, double[]> map = new HashMap<>();
            for (String attribute : this.attributes) {
                map.putIfAbsent(attribute, new double[numBins]);
            }
            return map;
        });
    }

    private int getTimeBin(double time) {
        //Agents who end their first activity before the simulation has started will depart in the first time step.
        if (time <= 0.0) return 0;
        //Calculate the bin for the given time.
        int bin = (int) (time / this.binSize);
        //Anything larger than 30 hours gets placed in the final bin.
        return Math.min(bin, this.numBins-1);
    }

    public ArrayList<String> getAttributes() {
        return attributes;
    }

}
