package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.types.WarmPollutant;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AggregateEmissionsDataPerPersonPerTime implements AggregateDataPerTime<Person>{
    protected static final Logger log = Logger.getLogger(AggregateCongestionDataPerLinkPerTime.class);
    private Scenario scenario;
    private double binSize = 3600.0;
    private int numBins = 30;
    private Map<Id<Person>, Map<String, double[]>> aggregateDataPerPersonPerTime = new HashMap<>();
    private final String[] attributes = {WarmPollutant.CO.getText(),
            WarmPollutant.CO2_TOTAL.getText(),
            WarmPollutant.FC.getText(),
            WarmPollutant.HC.getText(),
            WarmPollutant.NMHC.getText(),
            WarmPollutant.NO2.getText(),
            WarmPollutant.NOX.getText(),
            WarmPollutant.PM.getText(),
            WarmPollutant.SO2.getText(),};
    private final String outputFileName = "aggregate_emissions_per_person_per_time.csv";

    public AggregateEmissionsDataPerPersonPerTime(Scenario scenario, double binSize) {
        this.scenario = scenario;
        this.numBins = (int) (30 * 3600 / binSize);
        this.binSize = binSize;
        setUpTimeBins();
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

    public void addValue(Id<Person> personId, int timeBin, String attribute, double value) {
        double oldValue = getValue(personId, timeBin, attribute);
        double newValue = oldValue + value;
        setValue(personId, timeBin, attribute, newValue);
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
                                this.aggregateDataPerPersonPerTime.get(lid).get(header[i+2])[bin] = value;
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

            for (Map.Entry<Id<Person>, Map<String, double[]>> e : this.aggregateDataPerPersonPerTime.entrySet()) {
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
