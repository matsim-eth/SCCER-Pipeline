package playground.ivt.proj_sccer.aggregation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by molloyj on 10.10.2017.
 */
public class EmissionsTally implements WarmEmissionEventHandler, ColdEmissionEventHandler, PersonArrivalEventHandler {
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    Map<Id<Person>,List<Map<String, Double>>> personId2Leg2Pollutant = new HashMap<>();
    Map<Id<Person>, Map<String, Double>> tempValues = new HashMap<>();

    public EmissionsTally(Scenario scenario, Vehicle2DriverEventHandler drivers) {
        this.drivers = drivers;
        this.scenario = scenario;

        scenario.getPopulation().getPersons().keySet().forEach(p -> {
            personId2Leg2Pollutant.put(p, new ArrayList<>());
            tempValues.put(p, new HashMap<>()); //instead of a double, we need to put a map from string to double
        });
    }

    @Override
    public void handleEvent(PersonArrivalEvent e) {
        Id<Person> pid = e.getPersonId();
        personId2Leg2Pollutant.get(pid).add(tempValues.get(pid)); //this stays the same.
        tempValues.put(pid, new HashMap<>()); //then we need to build a new map here
    }

    @Override
    public void handleEvent(ColdEmissionEvent e) {
        Id<Person> personId = drivers.getDriverOfVehicle(e.getVehicleId());
        if (personId == null) {
            personId = Id.createPersonId(e.getVehicleId().toString());
        }
//        double emissionCount = e.getColdEmissions().get(ColdPollutant.NOX);
//        double oldValue = tempValues.get(personId);
//        tempValues.put(personId, oldValue + emissionCount); //these three lines instead need to a be a loop through all cold pollutants
        
        Map<ColdPollutant, Double> pollutants = e.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            if(tempValues.get(personId).containsKey(pollutant)) {
            	tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());
            }
            else {
            	tempValues.get(personId).put(pollutant, p.getValue());
            }         
        }
    }

    @Override
    public void handleEvent(WarmEmissionEvent e) {
        Id<Person> personId = drivers.getDriverOfVehicle(e.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(e.getVehicleId().toString());
        }
//        double emissionCount = e.getWarmEmissions().get(WarmPollutant.NOX);
//        double oldValue = tempValues.get(personId);
//        tempValues.put(personId, oldValue + emissionCount); //and the same here
        
        Map<WarmPollutant, Double> pollutants = e.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            if(tempValues.get(personId).containsKey(pollutant)) {
            	tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());
            }
            else {
            	tempValues.get(personId).put(pollutant, p.getValue());
            }         
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
}
