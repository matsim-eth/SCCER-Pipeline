package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregateDataPerTimeMock extends AggregateDataPerTimeImpl<Link>{
    protected static final Logger log = Logger.getLogger(AggregateDataPerTimeMock.class);

    public AggregateDataPerTimeMock() {
        super(30*60, new ArrayList<String>(), Link.class );

    }

    // getters
    public int getNumBins() {
        return numBins;
    }

    public double getBinSize() {
        return 30*60;
    }

    public Map<Id<Link>, Map<String, double[]>> getData() {
        return null;
    }

    public double getValue(Id<Link> id, double time, String attribute) {
        int timeBin = getTimeBin(time);
        return getValue(id, timeBin, attribute);
    }

    public double getValue(Id<Link> id, int timeBin, String attribute) {
        return 0.0;
    }

    public void setValue(Id<Link> id, double time, String attribute, double value) {

    }

    public void setValue(Id<Link> id, int timeBin, String attribute, double value) {
    }

    public void addValue(Id<Link> id, double time, String attribute, double value) {

    }

    public void addValue(Id<Link> id, int timeBin, String attribute, double value) {

    }

    // setup
    private void setUpTimeBins(Id<Link> id) {

    }

    //reader and writer
    @Override
    public void loadDataFromCsv(String input) {

    }

    @Override
    public void writeDataToCsv(String outputPath) {

    }

    private int getTimeBin(double time) {
        //Agents who end their first activity before the simulation has started will depart in the first time step.
        if (time <= 0.0) return 0;
        //Calculate the bin for the given time.
        int bin = (int) (time / this.binSize);
        //Anything larger than 30 hours gets placed in the final bin.
        return Math.min(bin, this.numBins-1);
    }

    public static AggregateDataPerTimeMock congestion(double binSize, Class clazz) {
        List<String> attributes = new ArrayList<>();
        attributes.add("count");
        attributes.add("delay_caused");
        attributes.add("delay_experienced");
        attributes.add("congestion_caused");
        attributes.add("congestion_experienced");

        return new AggregateDataPerTimeMock();
    }
}
