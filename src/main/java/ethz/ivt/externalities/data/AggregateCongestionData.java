package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.util.Map;

public class AggregateCongestionData extends AggregateExternalityData{
    private final String outputFileName = "aggregate_delay.csv";

    public AggregateCongestionData(Scenario scenario, double binSize) {
        super(scenario, binSize);
    }

    @Override
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

    @Override
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
