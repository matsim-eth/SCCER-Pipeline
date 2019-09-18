package ethz.ivt.externalities.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.opencsv.CSVReader;
import ethz.ivt.externalities.data.congestion.io.IdSerializer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AggregateDataPerTimeImpl<T> implements AggregateDataPerTime<T> {
    protected static final Logger log = Logger.getLogger(AggregateDataPerTimeImpl.class);
    final Class<T> clazz;
    double binSize;
    int numBins;
    Map<Id<T>, Map<String, double[]>> aggregateDataPerLinkPerTime = new HashMap<>();
    ArrayList<String> attributes;

    public AggregateDataPerTimeImpl(double binSize, ArrayList<String> attributes,
                                    Class<T> clazz) {
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
        this.attributes = attributes;
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

    public double getValueAtTime(Id<T> id, double time, String attribute) {
        int timeBin = getTimeBin(time);
        return getValueInTimeBin(id, timeBin, attribute);
    }

    public double getValueInTimeBin(Id<T> id, int timeBin, String attribute) {
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

    public void setValueAtTime(Id<T> id, double time, String attribute, double value) {
        int timeBin = getTimeBin(time);
        setValueForTimeBin(id, timeBin, attribute, value);
    }

    public void setValueForTimeBin(Id<T> id, int timeBin, String attribute, double value) {
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

    public void addValueAtTime(Id<T> id, double time, String attribute, double value) {
        int timeBin = getTimeBin(time);
        addValueToTimeBin(id, timeBin, attribute, value);
    }

    public void addValueToTimeBin(Id<T> id, int timeBin, String attribute, double value) {
        double oldValue = getValueInTimeBin(id, timeBin, attribute);
        double newValue = oldValue + value;
        setValueForTimeBin(id, timeBin, attribute, newValue);
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
                            double originalBinSize = Double.parseDouble(record[1]);
                            int timeBin = Integer.parseInt(record[2]);
                            double time = timeBin * originalBinSize;

                            setUpTimeBins(lid);
                            // go through all attributes
                            for (int i = 0; i < attributes.size(); i++) {
                                double value = Double.parseDouble(record[i+3]);
                                if (Double.isNaN(value))
                                {
                                    value = 0.;
                                }
                                this.addValueAtTime(lid, time, header[i+3], value);
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

        File file = new File(outputPath);
        file.getParentFile().mkdirs();


        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            String header = Link.class.getSimpleName() + "id;binSize;timebin";
            for (String attribute : this.attributes) {
                header = header + ";" + attribute;
            }
            bw.write(header);
            bw.newLine();

            for (Map.Entry<Id<T>, Map<String, double[]>> e : this.aggregateDataPerLinkPerTime.entrySet()) {
                for (int bin = 0; bin<this.numBins; bin++) {
                    if (aggregateDataPerLinkPerTime.get(e.getKey()).get("count_entering")[bin] > 0 |
                            aggregateDataPerLinkPerTime.get(e.getKey()).get("count_exiting")[bin] > 0) {

                        String entry = e.getKey() + ";" + binSize + ";" + bin;

                        for (String attribute : attributes) {
                            entry = entry + ";" + e.getValue().get(attribute)[bin];
                        }

                        bw.write(entry);
                        bw.newLine();
                    }

                }
            }

            bw.close();
            log.info("Output written to " + outputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int getTimeBin(double time) {
        //Agents who end their first activity before the simulation has started will depart in the first time step.
        if (time <= 0.0) return 0;
        //Calculate the bin for the given time.
        int bin = (int) (time / this.binSize);
        //Anything larger than 30 hours gets placed in the final bin.
        return Math.min(bin, this.numBins-1);
    }

    public static AggregateDataPerTimeImpl<Link> congestionLink(double binSize) {
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("count_entering");
        attributes.add("count_exiting");
        attributes.add("delay_caused");
        attributes.add("delay_experienced");
        attributes.add("congestion_caused");
        attributes.add("congestion_experienced");

        return new AggregateDataPerTimeImpl<Link>(binSize, attributes, Link.class);
    }

    public static AggregateDataPerTimeImpl<Person> congestionPerson(double binSize) {
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("count");
        attributes.add("delay_caused");
        attributes.add("delay_experienced");
        attributes.add("congestion_caused");
        attributes.add("congestion_experienced");

        return new AggregateDataPerTimeImpl<Person>(binSize, attributes, Person.class);
    }

    public static AggregateDataPerTimeImpl<?> congestion(double binSize, Class clazz) {
        if (clazz.getName().equals("Link")) {
            return congestionLink(binSize);
        }
        if (clazz.getName().equals("Person")) {
            return congestionPerson(binSize);
        }
        return null;
    }

    public ArrayList<String> getAttributes() {
        return attributes;
    }

    public static AggregateDataPerTimeImpl<Link> readKryoCongestion(Path path) throws FileNotFoundException {
        Kryo kryo = new Kryo();
        kryo.register(Id.class, new IdSerializer(Link.class));
        kryo.register(Link.class);
        kryo.register(AggregateDataPerTimeImpl.class, new AggregateDataSerializer());

        long startTime = System.currentTimeMillis();

        Input input = new Input(new FileInputStream(path.toFile()));
        AggregateDataPerTimeImpl<Link> cd2 = kryo.readObject(input, AggregateDataPerTimeImpl.class);
        long endTime = System.currentTimeMillis();
        double readTime = (endTime - startTime)/1000;
        log.info(String.format("read from kryo format in %.2f seconds", readTime));
        return cd2;

    }
}
