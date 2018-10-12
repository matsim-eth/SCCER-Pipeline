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

    private final int globalWarmingPotential_CH4 = 23;
    private final int globalWarmingPotential_N2O = 296;
    private final double costPerTonCO2eq = 121.5;

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
        keys.add("CO2-eq");
        keys.add("Climate Cost");
    }
    
    @Override
    public void handleEvent(ColdEmissionEvent e) {
        Id<Person> personId = getDriverOfVehicle(e.getVehicleId());

        // add emissions and CO2-equivalent
        Map<ColdPollutant, Double> pollutants = e.getColdEmissions();
        double CO2_equivalent = 0.0;
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());

            if (pollutant.equals("CO2(total)")) {
                CO2_equivalent += p.getValue();
            }
            else if (pollutant.equals("CH4")) {
                CO2_equivalent += p.getValue() * globalWarmingPotential_CH4;
            }
            else if (pollutant.equals("N2O")) {
                CO2_equivalent += p.getValue() * globalWarmingPotential_N2O;
            }
        }

        double cost = CO2_equivalent / 1e6 * costPerTonCO2eq;

        tempValues.get(personId).put("CO2-eq", tempValues.get(personId).get("CO2-eq") + CO2_equivalent);
        tempValues.get(personId).put("Climate Cost", tempValues.get(personId).get("Climate Cost") + cost);

    }


    @Override
    public void handleEvent(WarmEmissionEvent e) {
        Id<Person> personId = getDriverOfVehicle(e.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(e.getVehicleId().toString());
        }

        // add emissions
        Map<WarmPollutant, Double> pollutants = e.getWarmEmissions();
        double CO2_equivalent = 0.0;
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            tempValues.get(personId).put(pollutant, tempValues.get(personId).get(pollutant) + p.getValue());

            switch(pollutant) {
                case "CO2(total)":
                    CO2_equivalent += p.getValue();

                case "CH4":
                    CO2_equivalent += p.getValue() * globalWarmingPotential_CH4;

                case "N2O":
                    CO2_equivalent += p.getValue() * globalWarmingPotential_N2O;
            }
        }

        double cost = CO2_equivalent / 1e6 * costPerTonCO2eq;

        tempValues.get(personId).put("CO2-eq", tempValues.get(personId).get("CO2-eq") + CO2_equivalent);
        tempValues.get(personId).put("Climate Cost", tempValues.get(personId).get("Climate Cost") + cost);
    }

    public void writeCsvFile(Path outputPath, String filename) {
        Path outputFileName = outputPath.resolve(filename + "_emissions.csv");
        super.writeCsvFile(outputFileName);
    }

}
