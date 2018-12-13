package ethz.ivt.externalities.counters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ExternalityCounter implements PersonArrivalEventHandler, PersonDepartureEventHandler, EventHandler {
	private static final Logger log = Logger.getLogger(ExternalityCounter.class);

    protected final Scenario scenario;
	private String date;
	private Map<Id<Person>,List<LegValues>> personId2Leg = new HashMap<>(); //summed emissions values per person per leg
    private Map<Id<Person>, LegValues> tempValues = new HashMap<>(); //summed values within leg
    private Set<String> keys = new LinkedHashSet<>(); //list of all leg data fields

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

    }

    public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
        return Id.createPersonId(vehicleId.toString());
    }

	public Map<Id<Person>, List<LegValues>> getPersonId2Leg() {
		return personId2Leg;
	}

	// Check if leg is traveled by car and get start time
	@Override
	public void handleEvent(PersonDepartureEvent e) {
		Id<Person> pid = e.getPersonId();
		//check if person in map, assume that first event is always departure event

		this.tempValues.putIfAbsent(pid, new LegValues());
		tempValues.get(pid).put("StartTime", e.getTime());
        tempValues.get(pid).setMode(e.getLegMode());

	}

    // Get leg end time and append new leg to list
    @Override
    public void handleEvent(PersonArrivalEvent e) {
        Id<Person> pid = e.getPersonId();
		this.tempValues.putIfAbsent(pid, new LegValues());
		tempValues.get(pid).put("EndTime", e.getTime());
		personId2Leg.putIfAbsent(pid, new ArrayList<>());
		personId2Leg.get(pid).add(tempValues.get(pid)); //add new leg

        //reset
        tempValues.put(pid, new LegValues());

    }

    public void writeCsvFile(Path outputPath, String filename) {
		Path outputFileName = outputPath.resolve(filename + "_externalities.csv");
    	
		File file = outputFileName.toFile();
		file.getParentFile().mkdirs();
		

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	        // write header and records
			boolean headerWritten = false;
	    	for (Map.Entry<Id<Person>,List<LegValues>> person : personId2Leg.entrySet()  ) {
	    		int legCount = 0;
	    		for (LegValues leg : person.getValue()) {
	    			legCount++;

	    			String record = person.getKey() + ";" + this.date + ";" + legCount + ";";
					record += leg.getMode() + ";";
	    			record += keys.stream ().map(key -> String.format("%.4f", leg.get(key)))
							.collect(Collectors.joining(";"));

	    	        if (!headerWritten) {
                        String header = "PersonId;Date;Leg;Mode;";
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
		personId2Leg.clear();
		tempValues.clear();
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void addKey(String key) {
		keys.add(key);
	}

	public double getTempValue(Id<Person> personId, String key) {
    	this.tempValues.putIfAbsent(personId, new LegValues());
    	keys.add(key);
    	this.tempValues.get(personId).putIfAbsent(key, 0.0);
		return this.tempValues.get(personId).get(key);
	}

	public void putTempValue(Id<Person> personId, String key, double value) {
		this.tempValues.putIfAbsent(personId, new LegValues());
		this.tempValues.get(personId).put(key, value);
	}

	public void incrementTempValueBy(Id<Person> personId, String key, Double value) {
    	double oldValue = getTempValue(personId, key);
		putTempValue(personId, key, oldValue + value);

	}

	public void addKeys(Set<String> newKeys) {
		keys.addAll(newKeys);
	}
}
