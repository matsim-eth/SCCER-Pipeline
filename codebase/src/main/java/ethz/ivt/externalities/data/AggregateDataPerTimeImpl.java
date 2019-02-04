package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AggregateDataPerTimeImpl<T> implements AggregateDataPerTime<T>{
    protected static final Logger log = Logger.getLogger(AggregateDataPerTimeImpl.class);
    final Class<T> clazz;
    private double binSize;
    private int numBins;
    private Map<Id<T>, Map<String, double[]>> aggregateDataPerLinkPerTime = new HashMap<>();
    private List<String> attributes;
    private String outputFileName;

    public AggregateDataPerTimeImpl(double binSize, List<String> attributes, String outputFileName,
                                    Class<T> clazz) {
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
        this.attributes = attributes;
        this.outputFileName = outputFileName;
        this.clazz = clazz;

    }

    // getters
    public int getNumBins() {
        return numBins;
    }

    public double getBinSize() {
        return binSize;
    }

    public Map<Id<T>, Map<String, double[]>> getData() {
        return aggregateDataPerLinkPerTime;
    }

    public double getValue(Id<T> id, int timeBin, String attribute) {
        if (timeBin >= this.numBins) {
            log.warn("Time bin must be < " + this.numBins + ". Returning 0.");
            return 0.0;
        }
        if (aggregateDataPerLinkPerTime.containsKey(id)) {
            if (aggregateDataPerLinkPerTime.get(id).containsKey(attribute)) {
                return aggregateDataPerLinkPerTime.get(id).get(attribute)[timeBin];
            }
            log.warn("Attribute " + attribute + " is not valid. Returning 0.");
            return 0.0;
        }
        log.warn("Id " + id + " is not valid. No value set.");
        return 0.0;
    }

    public void setValue(Id<T> id, int timeBin, String attribute, double value) {
        if (timeBin >= this.numBins) {
            log.warn("Time bin must be < " + this.numBins + ". No value set.");
            return;
        }
        setUpTimeBins(id);
        if (aggregateDataPerLinkPerTime.containsKey(id)) {
            if (aggregateDataPerLinkPerTime.get(id).containsKey(attribute)) {
                aggregateDataPerLinkPerTime.get(id).get(attribute)[timeBin] = value;
                return;
            }
            log.warn("Attribute " + attribute + " is not valid. No value set.");
            return;
        }
        log.warn("Id " + id + " is not valid. No value set.");
        return;
    }

    public void addValue(Id<T> id, int timeBin, String attribute, double value) {
        double oldValue = getValue(id, timeBin, attribute);
        double newValue = oldValue + value;
        setValue(id, timeBin, attribute, newValue);
    }


    // setup
    private void setUpTimeBins(Id<T> id) {
        aggregateDataPerLinkPerTime.computeIfAbsent(id, a -> {
            HashMap<String, double[]> map = new HashMap<>();
            for (String attribute : this.attributes) {
                map.putIfAbsent(attribute, new double[numBins]);
            }
            return map;
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
                    if (header.length < (2 + attributes.size()) ) {
                        log.error("CSV file contains too few columns");
                    }
                    else {
                        // read line by line
                        while ((record = reader.readNext()) != null) {
                            Id<T> lid = Id.create(record[0], clazz);
                            int bin = Integer.parseInt(record[1]);

                            // go through all attributes
                            for (int i = 0; i < attributes.size(); i++) {
                                double value = Double.parseDouble(record[i+2]);
                                if (Double.isNaN(value))
                                {
                                    value = 0.;
                                }
                                setUpTimeBins(lid);
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

            for (Map.Entry<Id<T>, Map<String, double[]>> e : this.aggregateDataPerLinkPerTime.entrySet()) {
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
