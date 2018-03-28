package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AggregateCongestionDataPerLinkPerTime implements AggregateDataPerTime<Link>{
    protected static final Logger log = Logger.getLogger(AggregateCongestionDataPerLinkPerTime.class);
    private Scenario scenario;
    private double binSize;
    private int numBins;
    private Map<Id<Link>, Map<String, double[]>> aggregateDataPerLinkPerTime = new HashMap<>();
    private final String[] attributes = {"count", "delay"};
    private final String outputFileName = "aggregate_delay_per_link_per_time.csv";

    public AggregateCongestionDataPerLinkPerTime(Scenario scenario, double binSize) {
        this.scenario = scenario;
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
        setUpTimeBins();
    }

    // getters
    @Override
    public int getNumBins() {
        return numBins;
    }

    @Override
    public double getBinSize() {
        return binSize;
    }

    @Override
    public Map<Id<Link>, Map<String, double[]>> getData() {
        return aggregateDataPerLinkPerTime;
    }

    @Override
    public double getValue(Id<Link> linkId, int timeBin, String attribute) {
        if (timeBin >= this.numBins) {
            log.warn("Time bin must be < " + this.numBins + ". Returning 0.");
            return 0.0;
        }
        if (aggregateDataPerLinkPerTime.containsKey(linkId)) {
            if (aggregateDataPerLinkPerTime.get(linkId).containsKey(attribute)) {
                return aggregateDataPerLinkPerTime.get(linkId).get(attribute)[timeBin];
            }
            log.warn("Attribute " + attribute + " is not valid. Returning 0.");
            return 0.0;
        }
        log.warn(Link.class.getSimpleName() + " id " + linkId + " is not valid. No value set.");
        return 0.0;
    }

    @Override
    public void setValue(Id<Link> linkId, int timeBin, String attribute, double value) {
        if (timeBin >= this.numBins) {
            log.warn("Time bin must be < " + this.numBins + ". No value set.");
            return;
        }
        if (aggregateDataPerLinkPerTime.containsKey(linkId)) {
            if (aggregateDataPerLinkPerTime.get(linkId).containsKey(attribute)) {
                aggregateDataPerLinkPerTime.get(linkId).get(attribute)[timeBin] = value;
                return;
            }
            log.warn("Attribute " + attribute + " is not valid. No value set.");
            return;
        }
        log.warn(Link.class.getSimpleName() + " id " + linkId + " is not valid. No value set.");
        return;
    }

    public void addValue(Id<Link> linkId, int timeBin, String attribute, double value) {
        double oldValue = getValue(linkId, timeBin, attribute);
        double newValue = oldValue + value;
        setValue(linkId, timeBin, attribute, newValue);
    }

    // setup
    @Override
    public void setUpTimeBins() {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            aggregateDataPerLinkPerTime.put(l, new HashMap<>());
            for (String attribute : this.attributes) {
                aggregateDataPerLinkPerTime.get(l).putIfAbsent(attribute, new double[numBins]);
            }
        });
    }

    //reader and writer
    @Override
    public void loadDataFromCsv(String input) {
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(input), ';');

            try {
                String[] record = null;
                int line = 0;

                // check if attributes are as expected
                String[] header = reader.readNext();
                if (header != null) {
                    if (header.length < (2 + attributes.length) ) {
                        log.error("CSV file contains too few columns");
                    }
                    else {
                        // read line by line
                        while ((record = reader.readNext()) != null) {
                            Id<Link> lid = Id.createLinkId(record[0]);
                            int bin = Integer.parseInt(record[1]);

                            // go through all attributes
                            for (int i = 0; i < attributes.length; i++) {
                                double value = Double.parseDouble(record[i+2]);
                                if (Double.isNaN(value))
                                {
                                    value = 0.;
                                }
                                this.aggregateDataPerLinkPerTime.get(lid).get(header[i+2])[bin] = value;
                            }
                        }
                    }
                }
                else {
                    log.error("CSV file contains no header info.");
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

    @Override
    public void writeDataToCsv(String outputPath) {

        File dir = new File(outputPath);
        dir.mkdirs();

        String fileName = outputPath + outputFileName;

        File file = new File(fileName);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            String header = Link.class.getSimpleName() + "id" + ";timebin";
            for (String attribute : this.attributes) {
                header = header + ";" + attribute;
            }
            bw.write(header);
            bw.newLine();

            for (Map.Entry<Id<Link>, Map<String, double[]>> e : this.aggregateDataPerLinkPerTime.entrySet()) {
                for (int bin = 0; bin<this.numBins; bin++) {

                    String entry = e.getKey() + ";" + bin;

                    for (String attribute : attributes) {
                        entry = entry + ";" + e.getValue().get(attribute)[bin];
                    }

                    bw.write(entry);
                    bw.newLine();

                }
            }

            bw.close();
            log.info("Output written to " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
