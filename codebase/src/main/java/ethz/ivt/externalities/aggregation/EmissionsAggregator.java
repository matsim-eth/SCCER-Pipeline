package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by molloyj on 06.10.2017.
 * Note: These aggregators could be converted to a decorator pattern,
 */
public class EmissionsAggregator implements WarmEmissionEventHandler, ColdEmissionEventHandler {
    private static final Logger log = Logger.getLogger(EmissionsAggregator.class);
    private final Vehicle2DriverEventHandler drivers;
    public AggregateDataPerTimeImpl<Link> aggregateEmissionsDataPerLinkPerTime;
    public AggregateDataPerTimeImpl<Person> aggregateEmissionsDataPerPersonPerTime;

    public EmissionsAggregator(Scenario scenario, Vehicle2DriverEventHandler v2deh) {
        this.drivers = v2deh;
        ArrayList<String> attributes = new ArrayList<>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));

        this.aggregateEmissionsDataPerLinkPerTime = new AggregateDataPerTimeImpl<Link>(3600, attributes, Link.class);
        this.aggregateEmissionsDataPerPersonPerTime = new AggregateDataPerTimeImpl<Person>(3600, attributes, Person.class);
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateEmissionsDataPerLinkPerTime.getBinSize());

        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();

        Map<String, Double> pollutants = event.getColdEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();
            aggregateEmissionsDataPerLinkPerTime.addValueToTimeBin(linkId, timeBin, pollutant, p.getValue());
            aggregateEmissionsDataPerPersonPerTime.addValueToTimeBin(personId, timeBin, pollutant, p.getValue());
        }
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateEmissionsDataPerLinkPerTime.getBinSize());

        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();

        Map<String, Double> pollutants = event.getWarmEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();
            aggregateEmissionsDataPerLinkPerTime.addValueToTimeBin(linkId, timeBin, pollutant, p.getValue());
            aggregateEmissionsDataPerPersonPerTime.addValueToTimeBin(personId, timeBin, pollutant, p.getValue());
        }
//        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("WarmEmissionsCount", new double[num_bins]);
//        this.linkId2timeBin2values.get(event.getLinkId()).get("WarmEmissionsCount")[bin]++;
    }
}

