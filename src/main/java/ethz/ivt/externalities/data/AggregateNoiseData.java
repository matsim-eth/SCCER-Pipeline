package ethz.ivt.externalities.data;

import com.opencsv.CSVReader;
import ethz.ivt.externalities.ExternalityUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

import java.io.*;
import java.util.Map;
import java.util.regex.Pattern;

public class AggregateNoiseData extends AggregateExternalityData {
    private final String outputFileName = "aggregate_noise.csv";

    public AggregateNoiseData(Scenario scenario, double binSize) {
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

            bw.write("LinkId;TimeBin;Count;Noise");
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

    public void computeLinkId2timeBin2valuesFromEmissionFiles(String directory) {
        File dir = null;
        String[] files;

        try {
            // create new file object
            dir = new File(directory);

            // array of files and directory
            files = dir.list();

            // for each name in the path array
            for(String file:files) {
                loadNoiseDataFromNoiseEmissionCsv(directory, file);
                log.info("Loading : " + directory + file);
            }
        } catch (Exception e) {
            // if any error occurs
            e.printStackTrace();
        }
    }

    public void loadNoiseDataFromNoiseEmissionCsv(String directory, String file) {
        CSVReader reader;
        try {
            reader = new CSVReader(new FileReader(directory + file), ';');
            String[] file_parts = parseNoiseEmissionFileName(file);
            int bin = ExternalityUtils.getTimeBin(Double.parseDouble(file_parts[1]),this.binSize);

            // read line by line
            String[] record = null;
            int n = 0;
            try {
                while ((record = reader.readNext()) != null) {
                    if(n>0) {
                        Id<Link> lid = Id.createLinkId(record[0]);
                        double count = Double.parseDouble(record[1]) + Double.parseDouble(record[2]);
                        double value = Double.parseDouble(record[5]);

                        this.linkId2timeBin2values.get(lid).get("count")[bin] = count;
                        this.linkId2timeBin2values.get(lid).get("value")[bin] = value;
                    }
                    n++;
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

    private String[] parseNoiseEmissionFileName(String file) {
        String[] fp1 = file.split("_");
        String[] fp2 = fp1[1].split(Pattern.quote("."));
        String[] fp = new String[4];

        fp[0] = fp1[0];
        fp[1] = fp2[0];
        fp[2] = fp2[1];
        fp[3] = fp2[2];

        return fp;
    }
}
