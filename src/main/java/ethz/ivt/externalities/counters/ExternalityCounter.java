package ethz.ivt.externalities.counters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.apache.log4j.Logger;
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
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;

public abstract class ExternalityCounter implements PersonArrivalEventHandler, PersonDepartureEventHandler, LinkEnterEventHandler, EventHandler {
	private static final Logger log = Logger.getLogger(ExternalityCounter.class);
    private static final double ECAR = 1;
    private static final double CAR = 0;
    protected final Scenario scenario;
	private String date;
    Map<Id<Person>,List<Map<String, Double>>> personId2Leg2Values = new HashMap<>(); //summed emissions values per person per leg
    Map<Id<Person>, Map<String, Double>> tempValues = new HashMap<>(); //summed values within leg
    List<String> keys = new ArrayList<>(); //list of all leg data fields
    
    public ExternalityCounter(Scenario scenario, String date) {
    	this.scenario = scenario;
    	this.date = date;
    	initializeFields();
    }
    
    //basic fields
    protected void initializeFields() {
        keys.add("StartTime");
        keys.add("EndTime");
        keys.add("Distance");
        keys.add("Mode");
    }
    
    protected void initializeHashMaps(Id<Person> p) {
        personId2Leg2Values.put(p, new ArrayList<>());
        tempValues.put(p, new HashMap<>());
        for (String key : keys) {
         	tempValues.get(p).put(key, 0.0);
        }
    }

    protected Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
        return Id.createPersonId(vehicleId.toString().substring(0,4)); //TODO: better handling of IDs -> this wil only work for 4 digit ids
    }


	// Check if leg is traveled by car and get start time
	@Override
	public void handleEvent(PersonDepartureEvent e) {
		Id<Person> pid = e.getPersonId();
		//check if person in map, assume that first event is always departure event
		if (!personId2Leg2Values.containsKey(pid)) initializeHashMaps(pid);
		if (e.getLegMode().equals(TransportMode.car)) {
			tempValues.get(pid).put("StartTime", e.getTime());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = getDriverOfVehicle(event.getVehicleId());
		double carType = event.getVehicleId().toString().contains("Ecar") ? ECAR : CAR;

        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }
        double linkLength = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
        tempValues.get(personId).put("Distance", tempValues.get(personId).get("Distance") + linkLength);
        tempValues.get(personId).put("Mode", carType);
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
    
    public void writeCsvFile(Path outputFileName) {
    	
		File file = outputFileName.toFile();
		file.getParentFile().mkdirs();
		

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
	        // write header and records
			boolean headerWritten = false;
	    	for (Map.Entry<Id<Person>,List<Map<String, Double>>> person : personId2Leg2Values.entrySet()  ) {
	    		int legCount = 0;
	    		for (Map<String, Double> leg : person.getValue()) {
	    			legCount++;
	    			String record = person.getKey() + ";" + this.date + ";" + legCount + ";";
	    	        for (String key : keys) {
	    	            String val = String.format("%.4f", leg.get(key));
	    	            if ("Mode".equals(key)) {
	    	                val = leg.get(key) > 0.5 ? "Ecar" : "Car";
                        }
	    	        	record = record + val + ";";
	    	        }
	    	        if (!headerWritten) {
                        String header = "PersonId;Date;Leg;";
                        bw.write(header + String.join(";", keys));
	    				bw.newLine();
	    	        	headerWritten = true;
	    	        } 
	    	        bw.write(record);
	    	        bw.newLine();
	    		}
	    	}	
			bw.close();
			log.info("Output written to " + outputFileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }

	@Override
	public void reset(int iteration) {
		personId2Leg2Values.clear();
		tempValues.clear();
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
}
