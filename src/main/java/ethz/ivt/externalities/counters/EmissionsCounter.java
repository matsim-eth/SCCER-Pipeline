package ethz.ivt.externalities.counters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by molloyj on 10.10.2017.
 */
public class EmissionsCounter implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private ExternalityCounter externalityCounterDelegate;
    private Scenario scenario;

    public EmissionsCounter(Scenario scenario, ExternalityCounter externalityCounterDelegate) {
        this.scenario = scenario;
    	this.externalityCounterDelegate = externalityCounterDelegate;
        initializeFields();
    }

    protected void initializeFields() {
        final Set<String> pollutants = new HashSet<>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));

        for(String wp : pollutants) {
            externalityCounterDelegate.addKey(wp);
        }
    }
    
    @Override
    public void handleEvent(ColdEmissionEvent e) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(e.getVehicleId());

        // add emissions
        Map<String, Double> pollutants = e.getColdEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();
            externalityCounterDelegate.incrementTempValueBy(personId,pollutant, p.getValue());
        }
    }


    @Override
    public void handleEvent(WarmEmissionEvent e) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(e.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(e.getVehicleId().toString());
        }

        // add emissions
        Map<String, Double> pollutants = e.getWarmEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();
            externalityCounterDelegate.incrementTempValueBy(personId,pollutant, p.getValue());
        }
    }


}
