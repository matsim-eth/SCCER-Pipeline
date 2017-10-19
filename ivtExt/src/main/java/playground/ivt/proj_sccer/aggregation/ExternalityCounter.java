package playground.ivt.proj_sccer.aggregation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import com.opencsv.CSVWriter;

public abstract class ExternalityCounter implements PersonArrivalEventHandler, PersonDepartureEventHandler, LinkEnterEventHandler {
	protected final Scenario scenario;
    protected final Vehicle2DriverEventHandler drivers;
    Map<Id<Person>,List<Map<String, Double>>> personId2Leg2Values = new HashMap<>(); //summed emissions values per person per leg
    Map<Id<Person>, Map<String, Double>> tempValues = new HashMap<>(); //summed values within leg
    List<String> keys = new ArrayList<>(); //list of all leg data fields
    
    public ExternalityCounter(Scenario scenario, Vehicle2DriverEventHandler drivers) {
    	this.scenario = scenario;
    	this.drivers = drivers;
    	initializeFields();
    	initializeMaps();
    }
    
    //basic fields
    protected void initializeFields() {
        keys.add("StartTime");
        keys.add("EndTime");
        keys.add("Distance");
    }
    
    protected void initializeMaps() {
        scenario.getPopulation().getPersons().keySet().forEach(p -> {
            personId2Leg2Values.put(p, new ArrayList<>());
            tempValues.put(p, new HashMap<>());
            for (String key : keys) {
            	tempValues.get(p).put(key, 0.0);
            }           
        });
    }
    
	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }
        double linkLength = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
        tempValues.get(personId).put("Distance", tempValues.get(personId).get("Distance") + linkLength);
	}
    
    // Check if leg is traveled by car and get start time
    @Override
    public void handleEvent(PersonDepartureEvent e) {
        Id<Person> pid = e.getPersonId();
        if (e.getLegMode().equals(TransportMode.car.toString())) {
        	tempValues.get(pid).put("StartTime", e.getTime());
        }
    }

    // Get leg end time and append new leg to list
    @Override
    public void handleEvent(PersonArrivalEvent e) {
        Id<Person> pid = e.getPersonId();
        if (e.getLegMode().equals(TransportMode.car.toString())) {
        	tempValues.get(pid).put("EndTime", e.getTime());
        	personId2Leg2Values.get(pid).add(tempValues.get(pid)); //add new leg
        }
        //reset
        tempValues.put(pid, new HashMap<>());
        for (String key : keys) {
        	tempValues.get(pid).putIfAbsent(key, 0.0);
        }
    }
    
    public void writeCsvFile(String output) {
    	try {
			String fileName = output;
			CSVWriter writer = new CSVWriter(new FileWriter(fileName));
			
	        // write header and records
			boolean headerWritten = false;
	    	for (Map.Entry<Id<Person>,List<Map<String, Double>>> person : personId2Leg2Values.entrySet()  ) {
	    		for (Map<String, Double> leg : person.getValue()) {
	    			String fields = "PersonId,";
	    			String record = person.getKey().toString() + ",";
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
	    	}
    		writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
