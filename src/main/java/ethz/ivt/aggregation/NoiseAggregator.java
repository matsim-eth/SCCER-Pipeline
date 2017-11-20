package ethz.ivt.aggregation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

public class NoiseAggregator implements NoiseEventCausedHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
    private static final Logger log = Logger.getLogger(NoiseAggregator.class);
    
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    
    protected final double binSize_s; //30 hours, how do we split them
    protected int num_bins; //30 hours, how do we split them
    
    protected Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();
    protected Map<Id<Link>, Map<Integer, List<Id<Person>>>> linkId2timeBin2personId = new HashMap<>();

    public NoiseAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers, int binSize_s) {
        this.num_bins = (int) (30 * 3600 / binSize_s);
        this.binSize_s = binSize_s;
        this.scenario = scenario;
        this.drivers = drivers;

        setUpBinsForLinks(scenario);
        log.info("Number of congestion bins: " + num_bins);
    }
    
    protected void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
        	
            linkId2timeBin2values.put(l, new HashMap<>());
            linkId2timeBin2values.get(l).putIfAbsent("value", new double[num_bins]);
            linkId2timeBin2values.get(l).putIfAbsent("count", new double[num_bins]);
            linkId2timeBin2values.get(l).putIfAbsent("average", new double[num_bins]);
            
            linkId2timeBin2personId.put(l, new HashMap<>());
            for(int bin = 0; bin<this.num_bins; bin++) {
            	linkId2timeBin2personId.get(l).put(bin, new ArrayList<>());
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
	public void handleEvent(NoiseEventCaused event) {
        int bin = getTimeBin(event.getTime());
        this.linkId2timeBin2values.get(event.getLinkId()).get("value")[bin] += event.getAmount();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int bin = getTimeBin(event.getTime());
		Id<Person> pid = drivers.getDriverOfVehicle(event.getVehicleId());
        if(!this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).contains(pid) ) {
        	this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).add(pid);
        	this.linkId2timeBin2values.get(event.getLinkId()).get("count")[bin] += 1.0;
        }
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int bin = getTimeBin(event.getTime());
		Id<Person> pid = drivers.getDriverOfVehicle(event.getVehicleId());
        if(!this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).contains(pid) ) {
        	this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).add(pid);
        	this.linkId2timeBin2values.get(event.getLinkId()).get("count")[bin] += 1.0;
        }
	}

    public void computeLinkId2timeBin2averageValues() {
        for (Map.Entry<Id<Link>, Map<String, double[]>> e : linkId2timeBin2values.entrySet()) {
            double[] a = e.getValue().get("value").clone();
            double[] counts = e.getValue().get("count").clone();
            for (int i=0; i<counts.length; i++) {
            	if(counts[i] > 0.0) {
            		a[i] /= counts[i];
            	}
            	else {
            		a[i] = 0.0;
            	}
            }
            linkId2timeBin2values.get(e.getKey()).put("average", a);
        }
    }
    
    public void writeCsvFile(String outputPath) {
    	
		File dir = new File(outputPath);
		dir.mkdirs();
		
		String fileName = outputPath + "average_caused_noise.csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("LinkId;TimeBin;AverageCausedNoise");
			bw.newLine();
			
	        for (Map.Entry<Id<Link>, Map<String, double[]>> e : linkId2timeBin2values.entrySet()) {
	            for (int i=0; i<e.getValue().get("average").length; i++) {
	            	if (e.getValue().get("average")[i] != 0.0) {
		            	bw.write(e.getKey() + ";" + i + ";" + e.getValue().get("average")[i]);
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