package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AggregateCongestionData {
    protected static final Logger log = Logger.getLogger(AggregateCongestionData.class);
    protected double binSize;
    protected int numBins;
    protected Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();
    private final String outputFileName = "aggregate_delay.csv";

    public AggregateCongestionData(Scenario scenario, double binSize) {
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

    public void loadDataFromCsv(String input) {
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(input), ';');
            // read line by line
            String[] header = null;
            String[] record = null;
            int line = 0;
            try {
                while ((record = reader.readNext()) != null) {
                    if(line==0){
                        header = record;
                    }
                    else {
                        // todo: get index using header?
                        Id<Link> lid = Id.createLinkId(record[0]);
                        int bin = Integer.parseInt(record[1]);
                        double count = Double.parseDouble(record[2]);
                        double value = Double.parseDouble(record[3]);

                        this.linkId2timeBin2values.get(lid).get("count")[bin] = count;
                        this.linkId2timeBin2values.get(lid).get("value")[bin] = value;
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

    public void writeDataToCsv(String outputPath) {

        File dir = new File(outputPath);
        dir.mkdirs();

        String fileName = outputPath + outputFileName;

        File file = new File(fileName);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            bw.write("LinkId;TimeBin;Count;Delay");
            bw.newLine();

            for (Map.Entry<Id<Link>, Map<String, double[]>> e : this.linkId2timeBin2values.entrySet()) {
                for (int bin = 0; bin<this.numBins; bin++) {
                    if (e.getValue().get("value")[bin] != 0.0) {
                        bw.write(e.getKey() + ";" + bin + ";" + e.getValue().get("count")[bin] + ";" + e.getValue().get("value")[bin]);
                        bw.newLine();
                    }
                }
            }

            bw.close();
            log.info("Output written to " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
