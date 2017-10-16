package playground.ivt.proj_sccer.aggregation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import com.opencsv.CSVWriter;

import playground.ivt.proj_sccer.vsp.CongestionEvent;
import playground.ivt.proj_sccer.vsp.handlers.CongestionEventHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator extends EventAggregator implements CongestionEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);

    private List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

    private double vtts_car;
    private double congestionTollFactor;

    public CongestionAggregator(Scenario scenario, double congestionTollFactor, double binSize_s) {
        super(scenario, binSize_s);
        this.vtts_car = (scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
        this.congestionTollFactor = congestionTollFactor;
        log.info("VTTS_car: " + vtts_car);
        log.info("Congestion toll factor: " + congestionTollFactor);

    }

    public CongestionAggregator(Scenario scenario, int binSize_s) {
        this(scenario, 1.0, binSize_s);
    }

    @Override
    public void reset(int iteration) {
        this.congestionEvents.clear();
        super.reset(iteration);
    }

    @Override
    public void handleEvent(CongestionEvent event) {

    	// add delay
        int bin = getTimeBin(event.getEmergenceTime());
        this.linkId2timeBin2values.get(event.getLinkId()).get("delay")[bin] += event.getDelay();
        
        // add if delay caused by new agent
        if(!this.linkId2timeBin2personIdCausingDelay.get(event.getLinkId()).get(bin).contains(event.getCausingAgentId())) {
        	this.linkId2timeBin2personIdCausingDelay.get(event.getLinkId()).get(bin).add(event.getCausingAgentId());
        	this.linkId2timeBin2numberCausingDelay.get(event.getLinkId())[bin] += 1.0;
        }
        
    }
    
    public void writeCSVFile(String output) {
        try {
			String fileName = output + "average_caused_delay.csv";
			CSVWriter writer = new CSVWriter(new FileWriter(fileName));
			
	        // write header and records
			String[] header = "LinkId,TimeBin,AverageCausedDelay".split(",");
			writer.writeNext(header);
			
	        for (Map.Entry<Id<Link>, Map<String, double[]>> e : linkId2timeBin2values.entrySet()) {
	            double[] a = e.getValue().get("delay").clone();
	            double[] counts = linkId2timeBin2numberCausingDelay.get(e.getKey());
	            for (int i=0; i<counts.length; i++) {
	            	if (counts[i] != 0.0) {
	            		a[i] /= counts[i];
	            	}
	            	else {
	            		a[i] = 0.0;
	            	}
	            	String record = e.getKey().toString() + "," + i + "," + a[i];
	            	String[] records = record.split(",");
	            	writer.writeNext(records);
	            }
	        }
    		writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


}
