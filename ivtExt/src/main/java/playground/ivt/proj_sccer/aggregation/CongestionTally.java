package playground.ivt.proj_sccer.aggregation;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class CongestionTally implements LinkEnterEventHandler {
	private static final Logger log = Logger.getLogger(CongestionTally.class);
	
    protected Map<Id<Link>, double[]> linkId2timeBin2values = new HashMap<>();
    Map<Id<Person>, Double> personId2causedDelay = new HashMap<>();

    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    
	protected final int num_bins;
	protected int binSize_s;

    public CongestionTally(Scenario scenario, Vehicle2DriverEventHandler drivers, int binSize_s) {
        this.num_bins = (int) (30 * 3600 / binSize_s);
        this.binSize_s = binSize_s;
        this.scenario = scenario;
        this.drivers = drivers;

        setUpBinsForLinks(scenario);
        log.info("Number of congestion bins: " + num_bins);
        
        // initialize maps
        scenario.getPopulation().getPersons().keySet().forEach(p -> {
        	personId2causedDelay.put(p, 0.0);         
        });
    }

    protected void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            linkId2timeBin2values.put(l, new double[num_bins]);
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
		 * Calculate the bin for the given time. Increase it by one if the result
		 * of the modulo operation is > 0. If it is 0, it is the last time value
		 * which is part of the previous bin.
		 */  
        int bin = (int) (timeAfterSimStart / binSize_s);
        if (timeAfterSimStart % binSize_s == 0.0) bin--;

        return bin;
    }
    
    public void loadCsvFile(String input) {
    	CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(input), ',');
			// read line by line
			String[] record = null;
			int count = 0;
			try {
				while ((record = reader.readNext()) != null) {
					if(count>0) {
						Id<Link> lid = Id.createLinkId(record[0]);
						int bin = Integer.parseInt(record[1]);
						double delay = Double.parseDouble(record[2]);
						this.linkId2timeBin2values.get(lid)[bin] = delay;
					}
					count ++;
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
	public void handleEvent(LinkEnterEvent event) {
		int bin = getTimeBin(event.getTime());
		Id<Link> lid = event.getLinkId();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }
		double delay = this.linkId2timeBin2values.get(lid)[bin];
		double previous = this.personId2causedDelay.get(event.getVehicleId());
		
		this.personId2causedDelay.put(personId, previous + delay);
	}
	
    public void outputSummary() {
    	for (Map.Entry<Id<Person>, Double> e : personId2causedDelay.entrySet()  ) {
    		System.out.println("Person " + e.getKey().toString() + " caused " + personId2causedDelay.get(e.getKey()).toString() + " seconds of delay.");
    	}
    }
    
    public void writeCsvFile(String output) {
		String fileName = output + "caused_delay.csv";
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(fileName));
	        // write header and records
			String[] header = "PersonId,CausedDelay".split(",");
			writer.writeNext(header);
			
	    	for (Map.Entry<Id<Person>, Double> e : personId2causedDelay.entrySet()  ) {
	    		String record = e.getKey().toString() + "," + e.getValue().toString();
	    		String[] records = record.split(",");
            	writer.writeNext(records);
	    	}
    		writer.close();
    		log.info("CSV created successfully!");
		} catch (IOException e1) {
			log.error("Error writing CSV file!");
			e1.printStackTrace();
		}
    }

}
