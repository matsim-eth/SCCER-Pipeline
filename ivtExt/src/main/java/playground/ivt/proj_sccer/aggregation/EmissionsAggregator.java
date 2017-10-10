package playground.ivt.proj_sccer.aggregation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.Map;

/**
 * Created by molloyj on 06.10.2017.
 * Note: These aggregators could be converted to a decorator pattern,
 */
public class EmissionsAggregator extends EventAggregator implements WarmEmissionEventHandler, ColdEmissionEventHandler {

    private final Vehicle2DriverEventHandler drivers;

    public EmissionsAggregator(Scenario scenario, double binSize_s, Vehicle2DriverEventHandler v2deh) {
        super(scenario, binSize_s);
        this.drivers = v2deh;
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        int bin = getTimeBin(event.getTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);

        //example of getting driver of vehicle:
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());


        Map<ColdPollutant, Double> pollutants = event.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent(pollutant, new double[num_bins]);
            this.linkId2timeBin2values.get(event.getLinkId()).get(pollutant)[bin] += p.getValue();
        }
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("ColdEmissionsCount", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("ColdEmissionsCount")[bin]++;


    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        int bin = getTimeBin(event.getTime());
        //this.linkId2timeBin2delaySum.putIfAbsent(event.getLinkId(), new double[num_bins]);
        Map<WarmPollutant, Double> pollutants = event.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent(pollutant, new double[num_bins]);
            this.linkId2timeBin2values.get(event.getLinkId()).get(pollutant)[bin] += p.getValue();
        }
        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("WarmEmissionsCount", new double[num_bins]);
        this.linkId2timeBin2values.get(event.getLinkId()).get("WarmEmissionsCount")[bin]++;
    }
}

