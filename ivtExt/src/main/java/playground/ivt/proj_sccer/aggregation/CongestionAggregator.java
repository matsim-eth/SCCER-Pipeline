package playground.ivt.proj_sccer.aggregation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import playground.ivt.proj_sccer.vsp.CongestionEvent;
import playground.ivt.proj_sccer.vsp.handlers.CongestionEventHandler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    
    private final Scenario scenario;
    
    protected final double binSize_s; //30 hours, how do we split them
    protected int num_bins; //30 hours, how do we split them
    
    protected Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();
    protected Map<Id<Link>, Map<Integer, List<Id<Person>>>> linkId2timeBin2personIdCausingDelay = new HashMap<>();

    public CongestionAggregator(Scenario scenario, int binSize_s) {
        this.num_bins = (int) (30 * 3600 / binSize_s);
        this.binSize_s = binSize_s;
        this.scenario = scenario;

        setUpBinsForLinks(scenario);
        log.info("Number of congestion bins: " + num_bins);
    }
    
    protected void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
        	
            linkId2timeBin2values.put(l, new HashMap<>());
            linkId2timeBin2values.get(l).putIfAbsent("delay", new double[num_bins]);
            linkId2timeBin2values.get(l).putIfAbsent("count", new double[num_bins]);
            linkId2timeBin2values.get(l).putIfAbsent("avg_delay", new double[num_bins]);
            
            linkId2timeBin2personIdCausingDelay.put(l, new HashMap<>());
            for(int bin = 0; bin<this.num_bins; bin++) {
            	linkId2timeBin2personIdCausingDelay.get(l).put(bin, new ArrayList<>());
            }
        });
    }

    /*package*/ int getTimeBin(double time) {

        double timeAfterSimStart = time;

		/*
		 * Agents who end their first activity before the simulation has started
		 * will depart in the first time step.
		 */
        if (timeAfterSimStart <= 0.0) return 0;

		/*
		 * Calculate the bin for the given time. If the result
		 * of the modulo operation is 0, it is the last time value
		 * which is part of the previous bin.
		 */  
        int bin = (int) (timeAfterSimStart / binSize_s);
        if (timeAfterSimStart % binSize_s == 0.0) bin--;

        return bin;
    }

    @Override
    public void handleEvent(CongestionEvent event) {

    	// add delay
        int bin = getTimeBin(event.getEmergenceTime());
        this.linkId2timeBin2values.get(event.getLinkId()).get("delay")[bin] += event.getDelay();
        
        // add if delay caused by new agent
        if(!this.linkId2timeBin2personIdCausingDelay.get(event.getLinkId()).get(bin).contains(event.getCausingAgentId())) {
        	this.linkId2timeBin2personIdCausingDelay.get(event.getLinkId()).get(bin).add(event.getCausingAgentId());
        	this.linkId2timeBin2values.get(event.getLinkId()).get("count")[bin] += 1.0;
        }
        
    }

    public void computeLinkAverageCausedDelays() {
        for (Map.Entry<Id<Link>, Map<String, double[]>> e : linkId2timeBin2values.entrySet()) {
            double[] a = e.getValue().get("delay").clone();
            double[] counts = e.getValue().get("count").clone();
            for (int i=0; i<counts.length; i++) {
            	if(counts[i] > 0.0) {
            		a[i] /= counts[i];
            	}
            	else {
            		a[i] = 0.0;
            	}
            }
            linkId2timeBin2values.get(e.getKey()).put("avg_delay", a);
        }
    }
    
    public void writeCsvFile(String output) {
		String fileName = output + "average_caused_delay.csv";
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(fileName));
	        // write header and records
			String[] header = "LinkId,TimeBin,AverageCausedDelay".split(",");
			writer.writeNext(header);
			
	        for (Map.Entry<Id<Link>, Map<String, double[]>> e : linkId2timeBin2values.entrySet()) {
	            for (int i=0; i<e.getValue().get("avg_delay").length; i++) {
	            	String record = e.getKey().toString() + "," + i + "," + e.getValue().get("avg_delay")[i];
	            	String[] records = record.split(",");
	            	writer.writeNext(records);
	            }
	        }
    		writer.close();
    		log.info("CSV created successfully!");
		} catch (IOException e1) {
			log.error("Error writing CSV file!");
			e1.printStackTrace();
		}
    }
    
}
