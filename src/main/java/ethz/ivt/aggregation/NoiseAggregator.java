package ethz.ivt.aggregation;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.events.NoiseEventCaused;
import org.matsim.contrib.noise.handler.NoiseEventCausedHandler;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

public class NoiseAggregator {
    private static final Logger log = Logger.getLogger(NoiseAggregator.class);
    
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    
    protected final double binSize_s; //30 hours, how do we split them
    protected int num_bins; //30 hours, how do we split them
    
    protected Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();

    public NoiseAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers, int binSize_s) {
        this.num_bins = (int) (30 * 3600 / binSize_s);
        this.binSize_s = binSize_s;
        this.scenario = scenario;
        this.drivers = drivers;

        setUpBinsForLinks(scenario);
        log.info("Number of noise bins: " + num_bins);
    }
    
    protected void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
        	
            linkId2timeBin2values.put(l, new HashMap<>());
            linkId2timeBin2values.get(l).putIfAbsent("value", new double[num_bins]);
            linkId2timeBin2values.get(l).putIfAbsent("count", new double[num_bins]);

        });
    }

    public void computeLinkId2timeBin2values(String directory) {
		File dir = null;
		String[] files;

		try {
			// create new file object
			dir = new File(directory);

			// array of files and directory
			files = dir.list();

			// for each name in the path array
			for(String file:files) {
				loadNoiseDataFromCsvFile(directory, file);
				log.info("Loading : " + directory + file);
			}
		} catch (Exception e) {
			// if any error occurs
			e.printStackTrace();
		}
    }

	public void loadNoiseDataFromCsvFile(String directory, String file) {
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(directory + file), ';');
			String[] file_parts = parseFileName(file);
			int bin = ExternalityUtils.getTimeBin(Double.parseDouble(file_parts[1]),binSize_s);

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

	private String[] parseFileName(String file) {
		String[] fp1 = file.split("_");
		String[] fp2 = fp1[1].split(Pattern.quote("."));
		String[] fp = new String[4];

		fp[0] = fp1[0];
		fp[1] = fp2[0];
		fp[2] = fp2[1];
		fp[3] = fp2[2];

		return fp;
	}
    
    public void writeAggregateNoiseCsvFile(String outputPath) {
    	
		File dir = new File(outputPath);
		dir.mkdirs();
		
		String fileName = outputPath + "aggregate_noise.csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("LinkId;TimeBin;Count;Noise");
			bw.newLine();
			
	        for (Map.Entry<Id<Link>, Map<String, double[]>> e : linkId2timeBin2values.entrySet()) {
	            for (int bin=0; bin<e.getValue().get("value").length; bin++) {
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
