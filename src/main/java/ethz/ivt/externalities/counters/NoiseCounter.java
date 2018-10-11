package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateNoiseData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.nio.file.Path;

public class NoiseCounter implements LinkEnterEventHandler {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
    private final ExternalityCounter externalityCounterDelegate;
    private AggregateNoiseData aggregateNoiseData;

	public NoiseCounter(Scenario scenario, Vehicle2DriverEventHandler drivers,
                        AggregateNoiseData aggregateNoiseData,
                        ExternalityCounter externalityCounterDeletage) {
        this.aggregateNoiseData = aggregateNoiseData;
        this.externalityCounterDelegate = externalityCounterDeletage;
        log.info("Number of congestion bins: " + aggregateNoiseData.getNumBins());

        this.externalityCounterDelegate.addKey("MarginalNoiseCost");
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateNoiseData.getBinSize());
        Id<Link> linkId = event.getLinkId();
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }
        double noise = this.aggregateNoiseData.getValue(linkId, timeBin);
        externalityCounterDelegate.incrementTempValueBy(personId, "MarginalNoiseCost", noise);

    }

}
