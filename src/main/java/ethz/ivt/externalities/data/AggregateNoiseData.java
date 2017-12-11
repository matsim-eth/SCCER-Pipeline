package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AggregateNoiseData {
    protected static final Logger log = Logger.getLogger(AggregateNoiseData.class);
    protected double binSize;
    protected int numBins;
    protected Map<Id<Link>, double[]> linkId2timeBin2value = new HashMap<>();

    public AggregateNoiseData(Scenario scenario, double binSize) {
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

    public Map<Id<Link>, double[]> getLinkId2timeBin2value() {
        return linkId2timeBin2value;
    }

    // setup
    private void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            linkId2timeBin2value.putIfAbsent(l, new double[numBins]);
        });
    }

    public double getValue(Id<Link> linkId, int timeBin) {
        return this.linkId2timeBin2value.get(linkId)[timeBin];
    }

    public void loadDataFromCsv(String input) {
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(input), ';');
            // read line by line
            String[] record = null;
            int line = 0;
            try {
                while ((record = reader.readNext()) != null) {
                    if (line > 0) {
                        Id<Link> lid = Id.createLinkId(record[0]);
                        for (int column = 1; column<record.length; column++) {
                            this.linkId2timeBin2value.get(lid)[column-1] = Double.parseDouble(record[column]);
                        }
                    }
                    line ++;
                }
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                reader.close();
            } catch (IOException e) {
                log.error("Error while closing CSV file reader!");
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            log.error("CSV file not found!");
            e.printStackTrace();
        }
    }
}
