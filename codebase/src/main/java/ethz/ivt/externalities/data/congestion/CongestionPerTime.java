package ethz.ivt.externalities.data.congestion;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.opencsv.CSVReader;
import ethz.ivt.externalities.data.AggregateDataPerTime;
import ethz.ivt.externalities.data.congestion.io.IdSerializer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.*;

public class CongestionPerTime implements AggregateDataPerTime<Link>, KryoSerializable {
    protected static final Logger log = Logger.getLogger(CongestionPerTime.class);

    double binSize;
    int numBins;

    @MapSerializer.BindMap(
            keySerializer = IdSerializer.class,
            valueSerializer = MapSerializer.class,
            keyClass = Id.class,
            valueClass = HashMap.class,
            keysCanBeNull = false
    )
    Map<Id<Link>, Map<String, int[]>> aggregateDataPerLinkPerTime = new HashMap<>();

    ArrayList<String> attributes;

    public CongestionPerTime() {

    }

    public CongestionPerTime(double binSize, ArrayList<String> attributes) {
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
        this.attributes = attributes;

    }

    // getters
    public int getNumBins() {
        return numBins;
    }

    public double getBinSize() {
        return binSize;
    }

    public Map<Id<Link>, Map<String, int[]>> getData() {
        return aggregateDataPerLinkPerTime;
    }

    public int getValue(Id<Link> id, double time, String attribute) {
        int timeBin = getTimeBin(time);
        return getValue(id, timeBin, attribute);
    }

    public int getValue(Id<Link> id, int timeBin, String attribute) {
        if (timeBin >= this.numBins) {
            log.warn("Time bin must be < " + this.numBins + ". Returning 0.");
            return 0;
        }
        if (aggregateDataPerLinkPerTime.containsKey(id)) {
            if (aggregateDataPerLinkPerTime.get(id).containsKey(attribute)) {
                return aggregateDataPerLinkPerTime.get(id).get(attribute)[timeBin];
            }
            log.warn("Attribute " + attribute + " is not valid. Returning 0.");
            return 0;
        }
        log.warn("Id " + id + " is not valid. No value set.");
        return 0;
    }

    public void setValue(Id<Link> id, double time, String attribute, int value) {
        int timeBin = getTimeBin(time);
        setValue(id, timeBin, attribute, value);
    }

    public void setValue(Id<Link> id, int timeBin, String attribute, int value) {
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
            else {
                log.warn("Attribute " + attribute + " is not valid. No value set.");
                return;
            }
        } else {
            log.warn("Id " + id + " is not valid. No value set.");
        }
        return;
    }

    public void addValue(Id<Link> id, double time, String attribute, int value) {
        int timeBin = getTimeBin(time);
        addValue(id, timeBin, attribute, value);
    }

    public void addValue(Id<Link> id, int timeBin, String attribute, int value) {
        int oldValue = getValue(id, timeBin, attribute);
        int newValue = oldValue + value;
        setValue(id, timeBin, attribute, newValue);
    }


    // setup
    private void setUpTimeBins(Id<Link> id) {
        aggregateDataPerLinkPerTime.computeIfAbsent(id, a -> {
            HashMap<String, int[]> map = new HashMap<>();
            for (String attribute : this.attributes) {
                map.putIfAbsent(attribute, new int[numBins]);
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
                            Id<Link> lid = Id.createLinkId(record[0]);
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
                                this.addValue(lid, time, header[i+3], (int) value);
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

    //    File dir = new File(outputPath).getParentFile();
    //    dir.mkdirs();

        String fileName = outputPath;

        File file = new File(fileName);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            String header = Link.class.getSimpleName() + "id;binSize;timebin";
            for (String attribute : this.attributes) {
                header = header + ";" + attribute;
            }
            bw.write(header);
            bw.newLine();

            for (Map.Entry<Id<Link>, Map<String, int[]>> e : this.aggregateDataPerLinkPerTime.entrySet()) {
                for (int bin = 0; bin<this.numBins; bin++) {
                    if (e.getValue().get("count")[bin] > 0) {
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
            log.info("Output written to " + fileName);

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

    public static CongestionPerTime congestion(double binSize) {
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("count");
        attributes.add("delay_caused");
        attributes.add("delay_experienced");
        attributes.add("congestion_caused");
        attributes.add("congestion_experienced");

        return new CongestionPerTime(binSize, attributes);
    }

    @Override
    public void write(Kryo kryo, Output output) {
        output.writeDouble(binSize);
        output.writeInt(numBins);
        output.writeInt(attributes.size());
        output.writeInt(aggregateDataPerLinkPerTime.size());
        for (String at: attributes) {
            output.writeString(at);
        }

        //Map<Id<Link>, Map<String, double[]>> aggregateDataPerLinkPerTime = new HashMap<>();
        for (Id<Link> id : aggregateDataPerLinkPerTime.keySet()) {
            output.writeString(id.toString());
            long numRows = Arrays.stream(aggregateDataPerLinkPerTime.get(id).get("count")).filter(x -> x>0).count();
            output.writeLong(numRows, true);
            for (int bin = 0; bin < this.numBins; bin++) {
                if (aggregateDataPerLinkPerTime.get(id).get("count")[bin] > 0) {
                    output.writeInt(bin, true);
                    for (String at : attributes) {
                        int val = aggregateDataPerLinkPerTime.get(id).get(at)[bin];
                        output.writeInt(val, true);

                    }
                }
            }
        }
    }

    @Override
    public void read(Kryo kryo, Input input) {
        kryo.getGenerics().popGenericType();

        binSize = input.readDouble();
        numBins = input.readInt();
        int numAttr = input.readInt();
        int numLinks = input.readInt();
        attributes = new ArrayList<>();
        for (int i=0; i<numAttr; i++) {
            attributes.add(input.readString());
        }
        for (int i = 0; i < numLinks; i++) {
            Id<Link> linkId = Id.createLinkId(input.readString());
            long numRows = input.readLong(true);

            for (int bin = 0; bin < numRows; bin++) {
                int bin_num = input.readInt(true);
                for (String at : attributes) {
                    int val = input.readInt(true);
                    setValue(linkId, bin_num, at, val);
                }

            }
        }

    }
}
