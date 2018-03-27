package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AggregateCongestionDataPerPersonPerTime implements AggregateDataPerTime<Person>{
    protected static final Logger log = Logger.getLogger(AggregateCongestionDataPerLinkPerTime.class);
    private Scenario scenario;
    private double binSize = 3600.0;
    private int numBins = 30;
    private Map<Id<Person>, Map<String, double[]>> aggregateDataPerPersonPerTime = new HashMap<>();
    private final String[] attributes = {"delay_experienced", "delay_caused"};
    private final String outputFileName = "aggregate_delay_per_person_per_time.csv";

    public AggregateCongestionDataPerPersonPerTime(Scenario scenario, double binSize) {
        this.scenario = scenario;
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
    }

    @Override
    public int getNumBins() {
        return numBins;
    }

    @Override
    public double getBinSize() {
        return binSize;
    }

    @Override
    public Map<Id<Person>, Map<String, double[]>> getData() {
        return aggregateDataPerPersonPerTime;
    }

    @Override
    public double getValue(Id<Person> personId, int timeBin, String attribute) {
        if (timeBin >= this.numBins) {
            log.warn("Time bin must be < " + this.numBins + ". Returning 0.");
            return 0.0;
        }
        if (aggregateDataPerPersonPerTime.containsKey(personId)) {
            if (aggregateDataPerPersonPerTime.get(personId).containsKey(attribute)) {
                return aggregateDataPerPersonPerTime.get(personId).get(attribute)[timeBin];
            }
            log.warn("Requested attribute " + attribute + " is invalid. Returning 0.");
            return 0.0;
        }
        log.warn("Requested person id " + personId + " is invalid. Returning 0.");
        return 0.0;
    }

    @Override
    public void setValue(Id<Person> personId, int timeBin, String attribute, double value) {
        if (timeBin >= this.numBins) {
            log.warn("Time bin must be < " + this.numBins + ". No value set.");
            return;
        }
        if (aggregateDataPerPersonPerTime.containsKey(personId)) {
            if (aggregateDataPerPersonPerTime.get(personId).containsKey(attribute)) {
                aggregateDataPerPersonPerTime.get(personId).get(attribute)[timeBin] = value;
                return;
            }
            log.warn("Requested attribute " + attribute + " is invalid. No value set.");
            return;
        }
        log.warn("Requested person id " + personId + " is invalid. No value set.");
        return;

    }

    @Override
    public void setUpTimeBins() {
        scenario.getPopulation().getPersons().keySet().forEach(p -> {
            aggregateDataPerPersonPerTime.put(p, new HashMap<>());
            for (String attribute : this.attributes) {
                aggregateDataPerPersonPerTime.get(p).putIfAbsent(attribute, new double[numBins]);
            }
        });
    }

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
                        log.error("Csv file contains too few columns");
                    }
                    else {
                        Map<String, Integer> name2index = new HashMap<>();
                        for (int index = 0; index < header.length; index++) {
                            name2index.putIfAbsent(header[index], index);
                        }

                        // read line by line
                        while ((record = reader.readNext()) != null) {
                            Id<Link> lid = Id.createLinkId(record[name2index.get("LinkId")]);
                            int bin = Integer.parseInt(record[name2index.get("TimeBin")]);
                            double count = Double.parseDouble(record[name2index.get("Count")]);
                            double value = Double.parseDouble(record[name2index.get("Delay")]);
                            if (Double.isNaN(count))
                            {
                                count = 0.;
                            }
                            if (Double.isNaN(value))
                            {
                                value = 0.;
                            }

                            this.aggregateDataPerPersonPerTime.get(lid).get("count")[bin] = count;
                            this.aggregateDataPerPersonPerTime.get(lid).get("delay")[bin] = value;
                        }
                    }
                }
                else {
                    log.error("Congestion csv contains no header info.");
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
    public void writeDataToCsv(String output) {

    }
}
