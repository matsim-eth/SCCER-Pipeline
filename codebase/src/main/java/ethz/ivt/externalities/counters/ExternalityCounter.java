package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.MeasureExternalities;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.stream.Collectors;

public class ExternalityCounter implements PersonArrivalEventHandler, PersonDepartureEventHandler,
		ExtendedPersonDepartureEventEventHandler, TeleportationArrivalEventHandler, EventHandler {
	private static final Logger log = Logger.getLogger(ExternalityCounter.class);

    protected final Scenario scenario;
	private LocalDateTime date;
	private Map<Id<Person>,List<LegValues>> personId2Leg = new HashMap<>(); //summed emissions values per person per leg
    private Map<Id<Person>, LegValues> tempValues = new HashMap<>(); //summed values within leg
    private Set<String> keys = new LinkedHashSet<>(); //list of all leg data fields
	private EventsManager eventsManager;

	public ExternalityCounter(Scenario scenario, EventsManager eventsManager) {
    	this.scenario = scenario;
    	this.eventsManager = eventsManager;
    	initializeFields();
    }

    //basic fields
    protected void initializeFields() {
		keys = new LinkedHashSet<>();
		keys.add("StartTime");
        keys.add("EndTime");
        keys.add("MappedDistance");

    }

    public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
    	//strip vehicle type
        return Id.createPersonId(vehicleId);
    }

	public Map<Id<Person>, List<LegValues>> getPersonId2Leg() {
		return personId2Leg;
	}

	public void handleEvent(ExtendedPersonDepartureEvent e ) {
		Id<Person> pid = e.getPersonId();
		LocalDateTime eventDateTime = this.date.plus(Duration.ofSeconds(Math.round(e.getTime())));
		this.tempValues.putIfAbsent(pid, new LegValues(eventDateTime, e.getPersonDepartureEvent().getLegMode()));
		tempValues.get(pid).setTriplegId(e.getTripleg_id());
		tempValues.get(pid).setDistance(e.getDistance());
		eventsManager.processEvent(e.getPersonDepartureEvent());

	}

	// Check if leg is traveled by car and get start time
	@Override
	public void handleEvent(PersonDepartureEvent e) {
		Id<Person> pid = e.getPersonId();
		//check if person in map, assume that first event is always departure event
		LocalDateTime eventDateTime = this.date.plus(Duration.ofSeconds(Math.round(e.getTime())));
		this.tempValues.putIfAbsent(pid, new LegValues(eventDateTime, e.getLegMode()));
		tempValues.get(pid).put("StartTime", e.getTime());
        tempValues.get(pid).setMode(e.getLegMode());

	}

	@Override
	public void handleEvent(TeleportationArrivalEvent e) {
		Id<Person> pid = e.getPersonId();
		LocalDateTime eventDateTime = this.date.plus(Duration.ofSeconds(Math.round(e.getTime())));

		this.tempValues.get(pid).put("EndTime", e.getTime());
		this.tempValues.get(pid).setTriplegId("0");
		this.tempValues.get(pid).setDistance(e.getDistance());
		this.personId2Leg.putIfAbsent(pid, new ArrayList<>());
		this.personId2Leg.get(pid).add(this.tempValues.get(pid)); //add new leg

		//reset
		String legMode = this.tempValues.get(pid).getMode();
		this.tempValues.put(pid, new LegValues(eventDateTime, legMode));
	}

	// Get leg end time and append new leg to list
    @Override
    public void handleEvent(PersonArrivalEvent e) {
        Id<Person> pid = e.getPersonId();
		LocalDateTime eventDateTime = this.date.plus(Duration.ofSeconds(Math.round(e.getTime())));

		this.tempValues.putIfAbsent(pid, new LegValues(eventDateTime, e.getLegMode()));
		tempValues.get(pid).put("EndTime", e.getTime());
		personId2Leg.putIfAbsent(pid, new ArrayList<>());
		personId2Leg.get(pid).add(tempValues.get(pid)); //add new leg

        //reset
        tempValues.put(pid, new LegValues(eventDateTime, e.getLegMode()));

    }

    public void writeCsvFile(Path outputFileName) {

		File file = outputFileName.toFile();
		file.getParentFile().mkdirs();


		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	        // write header and records
			boolean headerWritten = false;
			List<String> keys_list = new ArrayList<>(keys);

	    	for (Map.Entry<Id<Person>,List<LegValues>> person : personId2Leg.entrySet()  ) {
	    		int legCount = 0;
	    		for (LegValues leg : person.getValue()) {
	    			legCount++;

	    			String record = person.getKey() + ";" + this.date + ";" + leg.getTriplegId() + ";";
					record += leg.getMode() + ";";
					record += leg.getDistance() + ";";
	    			record += keys_list.stream().map(key -> String.format("%.4f", leg.get(key)))
							.collect(Collectors.joining(";"));

	    	        if (!headerWritten) {
                        String header = "PersonId;Date;Leg;Mode;Distance;";
                        bw.write(header + String.join(";", keys_list));
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
		personId2Leg = new HashMap<>();
		tempValues = new HashMap<>();
		initializeFields();
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public void addKey(String key) {
		keys.add(key);
	}

	public double getTempValue(Id<Person> personId, String key) {
		if (!this.tempValues.containsKey(personId)) return 0.0;

		this.tempValues.get(personId).putIfAbsent(key, 0.0);
		return this.tempValues.get(personId).get(key);
	}

	private void putTempValue(Id<Person> personId, String key, double value) {
		this.tempValues.get(personId).put(key, value);
	}

	public void incrementTempValueBy(Id<Person> personId, String key, Double value) {
		if (value.isNaN() || value.isInfinite()) {
			log.warn(String.format("Skipping attempt to add NaN or Infinity to key %s for person %s", key, personId));
		} else {
			double oldValue = getTempValue(personId, key);
			putTempValue(personId, key, oldValue + value);
		}

	}

	public void addKeys(Set<String> newKeys) {
		keys.addAll(newKeys);
	}

	public ExternalityCounter copy() {
		ExternalityCounter ec = new ExternalityCounter(scenario, eventsManager);
		ec.setDate(date);
		ec.personId2Leg = getPersonId2Leg();
		ec.keys = new HashSet<>(keys);
		return ec;
	}
}
