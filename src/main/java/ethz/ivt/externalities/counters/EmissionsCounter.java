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

import java.nio.file.Path;
import java.util.Map;

/**
 * Created by molloyj on 10.10.2017.
 */
public class EmissionsCounter extends ExternalityCounter implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    public EmissionsCounter(Scenario scenario, String date) {
    	super(scenario, date);
    }
    
    @Override
    protected void initializeFields() {
    	super.initializeFields();
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
    }
    
    @Override
    public void handleEvent(ColdEmissionEvent e) {
        Id<Person> personId = getDriverOfVehicle(e.getVehicleId());

        // add emissions
        Map<ColdPollutant, Double> pollutants = e.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());
        }
    }


    @Override
    public void handleEvent(WarmEmissionEvent e) {
        Id<Person> personId = getDriverOfVehicle(e.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(e.getVehicleId().toString());
        }

        // add emissions
        Map<WarmPollutant, Double> pollutants = e.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());       
        }
    }

    public void writeCsvFile(Path outputPath, String filename) {
        Path outputFileName = outputPath.resolve(filename + "_emissions.csv");
        super.writeCsvFile(outputFileName);
    }

}
