package ethz.ivt.externalities.counters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;

import java.util.Map;

/**
 * Created by molloyj on 10.10.2017.
 */
public class EmissionsCounter implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private ExternalityCounter externalityCounterDelegate;
    private Scenario scenario;

    public EmissionsCounter(Scenario scenario, ExternalityCounter externalityCounterDelegate) {
        this.scenario = scenario;
    	this.externalityCounterDelegate = externalityCounterDelegate;
    }

    protected void initializeFields() {
        for(WarmPollutant wp : WarmPollutant.values()) {
            externalityCounterDelegate.addKey(wp.getText());
        }
        for(ColdPollutant cp : ColdPollutant.values()) {
            externalityCounterDelegate.addKey(cp.getText());

        }
    }
    
    @Override
    public void handleEvent(ColdEmissionEvent e) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(e.getVehicleId());

        // add emissions
        Map<ColdPollutant, Double> pollutants = e.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
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
        Map<WarmPollutant, Double> pollutants = e.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            externalityCounterDelegate.incrementTempValueBy(personId,pollutant, p.getValue());
        }
    }


}
