package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateEmissionsDataPerLinkPerTime;
import ethz.ivt.externalities.data.AggregateEmissionsDataPerPersonPerTime;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
public class EmissionsAggregator implements WarmEmissionEventHandler, ColdEmissionEventHandler {
    private static final Logger log = Logger.getLogger(EmissionsAggregator.class);
    private final Vehicle2DriverEventHandler drivers;
    private AggregateEmissionsDataPerLinkPerTime aggregateEmissionsDataPerLinkPerTime;
    private AggregateEmissionsDataPerPersonPerTime aggregateEmissionsDataPerPersonPerTime;

    public EmissionsAggregator(Scenario scenario, Vehicle2DriverEventHandler v2deh,
                               AggregateEmissionsDataPerLinkPerTime aggregateEmissionsDataPerLinkPerTime,
                               AggregateEmissionsDataPerPersonPerTime aggregateEmissionsDataPerPersonPerTime) {
        this.drivers = v2deh;
        this.aggregateEmissionsDataPerLinkPerTime = aggregateEmissionsDataPerLinkPerTime;
        this.aggregateEmissionsDataPerPersonPerTime = aggregateEmissionsDataPerPersonPerTime;
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateEmissionsDataPerLinkPerTime.getBinSize());

        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();

        Map<ColdPollutant, Double> pollutants = event.getColdEmissions();
        for (Map.Entry<ColdPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            aggregateEmissionsDataPerLinkPerTime.addValue(linkId, timeBin, pollutant, p.getValue());
            aggregateEmissionsDataPerPersonPerTime.addValue(personId, timeBin, pollutant, p.getValue());
        }
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateEmissionsDataPerLinkPerTime.getBinSize());

        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();

        Map<WarmPollutant, Double> pollutants = event.getWarmEmissions();
        for (Map.Entry<WarmPollutant, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey().getText();
            aggregateEmissionsDataPerLinkPerTime.addValue(linkId, timeBin, pollutant, p.getValue());
            aggregateEmissionsDataPerPersonPerTime.addValue(personId, timeBin, pollutant, p.getValue());
        }
//        this.linkId2timeBin2values.get(event.getLinkId()).putIfAbsent("WarmEmissionsCount", new double[num_bins]);
//        this.linkId2timeBin2values.get(event.getLinkId()).get("WarmEmissionsCount")[bin]++;
    }
}

