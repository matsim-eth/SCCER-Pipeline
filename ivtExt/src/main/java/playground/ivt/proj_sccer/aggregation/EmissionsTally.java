package playground.ivt.proj_sccer.aggregation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by molloyj on 10.10.2017.
 */
public class EmissionsTally implements WarmEmissionEventHandler, ColdEmissionEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler {
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    Map<Id<Person>,List<Map<String, Double>>> personId2Leg2Pollutant = new HashMap<>(); //summed emissions values per person per leg
    Map<Id<Person>, Map<String, Double>> tempValues = new HashMap<>(); //tallied values within leg
    List<String> keys = new ArrayList<>(); //list of all leg data fields

    public EmissionsTally(Scenario scenario, Vehicle2DriverEventHandler drivers) {
        this.drivers = drivers;
        this.scenario = scenario;
        
        // initialize data fields
        keys.add("StartTime");
        keys.add("EndTime");
        keys.add("Distance");
        for(WarmPollutant wp : WarmPollutant.values()) {
        	if(!keys.contains(wp.getText())) {
        		keys.add(wp.getText());
        	}
        }
        for(ColdPollutant cp : ColdPollutant.values()) {
        	if(!keys.contains(cp.getText())) {
        		keys.add(cp.getText());
        	}
        }

        // initialize maps
        scenario.getPopulation().getPersons().keySet().forEach(p -> {
            personId2Leg2Pollutant.put(p, new ArrayList<>());
            tempValues.put(p, new HashMap<>());
            for (String key : keys) {
            	tempValues.get(p).put(key, 0.0);
            }
        });
        
    }
    
    // Check if leg is traveled by car and get start time
    @Override
    public void handleEvent(PersonDepartureEvent e) {
        Id<Person> pid = e.getPersonId();
        if (e.getLegMode().equals("car")) {
        	tempValues.get(pid).put("StartTime", e.getTime());
        }
    }

    // Get leg end time and append new leg to list
    @Override
    public void handleEvent(PersonArrivalEvent e) {
        Id<Person> pid = e.getPersonId();
        if (e.getLegMode().equals("car")) {
        	tempValues.get(pid).put("EndTime", e.getTime());
        	personId2Leg2Pollutant.get(pid).add(tempValues.get(pid)); //add new leg
        }
        //reset
        for (String key : keys) {
        	tempValues.get(pid).put(key, 0.0);
        }
    }

    @Override
    public void handleEvent(ColdEmissionEvent e) {
        Id<Person> personId = drivers.getDriverOfVehicle(e.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(e.getVehicleId().toString());
        }
        
        // add traveled distance
        double linkLength = scenario.getNetwork().getLinks().get(e.getLinkId()).getLength();
        tempValues.get(personId).put("Distance", tempValues.get(personId).get("Distance") + linkLength);

        // add emissions
        Map<ColdPollutant, Double> pollutants = e.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());
        }
    }

    @Override
    public void handleEvent(WarmEmissionEvent e) {
        Id<Person> personId = drivers.getDriverOfVehicle(e.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(e.getVehicleId().toString());
        }
        
        // add traveled distance
        double linkLength = scenario.getNetwork().getLinks().get(e.getLinkId()).getLength();
        tempValues.get(personId).put("Distance", tempValues.get(personId).get("Distance") + linkLength);
        
        // add emissions
        Map<WarmPollutant, Double> pollutants = e.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());       
        }
    }

    public void outputSummary() {
    	for (Map.Entry<Id<Person>,List<Map<String, Double>>> pi2l2p : personId2Leg2Pollutant.entrySet()  ) {
    		System.out.println("Person ID: " + pi2l2p.getKey().toString());
    		int legCount = 0;
    		for (Map<String, Double> leg : pi2l2p.getValue()) {
    			legCount++;
    			System.out.print("Leg #" + legCount + ": ");
    	        for (Map.Entry<String, Double> p : leg.entrySet()) {
    	        	System.out.print(p.getKey() + ": " + p.getValue() + ", ");
    	        }
    			System.out.println();
    		}
    	}
    	System.out.println();
    }
    
    public void writeCSVFile(String output) {
    	int count = 0;
    	for (Map.Entry<Id<Person>,List<Map<String, Double>>> person : personId2Leg2Pollutant.entrySet()  ) {
    		if (count > 20) { //TODO: limit on how many output files. remove this after testing
    			break;
    		}
    		try {
    			String fileName = output + person.getKey().toString() + ".csv";
    			CSVWriter writer = new CSVWriter(new FileWriter(fileName));
    			
    	        // write header and records
    			boolean headerWritten = false;
	    		for (Map<String, Double> leg : person.getValue()) {
	    			String fields = "";
	    			String record = "";
        	        for (String key : keys) {
        	        	fields = fields + key + ",";
        	        	record = record + leg.get(key).toString() + ",";
        	        }
        	        String[] header = fields.split(",");
	    	        String[] records = record.split(",");
        	        if (!headerWritten) {
        	        	writer.writeNext(header);
        	        	headerWritten = true;
        	        } 
	    	        writer.writeNext(records);
	    		}
	    		writer.close();
	    		count++; //TODO: remove this after testing
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
}
