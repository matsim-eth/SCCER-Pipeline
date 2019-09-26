package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class CongestionCounter implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {
    private static final Logger log = Logger.getLogger(CongestionCounter.class);
    private Scenario scenario;
    private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
    public static final String CONGESTION_KEY = "congestion_caused";
    private ExternalityCounter externalityCounterDelegate;

    public CongestionCounter(Scenario scenario,
                             AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime,
                             ExternalityCounter externalityCounterDelegate) {

        this.scenario = scenario;
        this.aggregateCongestionDataPerLinkPerTime = aggregateCongestionDataPerLinkPerTime;
        this.externalityCounterDelegate = externalityCounterDelegate;

        initializeFields();
    }

    protected void initializeFields() {
        externalityCounterDelegate.addKey(CONGESTION_KEY); //average congestion caused
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        double time = event.getTime();
        Id<Link> linkId = event.getLinkId();
        Id<Person> personId = event.getPersonId();;

        double congestionCaused = this.aggregateCongestionDataPerLinkPerTime.getValueAtTime(linkId, time, "congestion");
        externalityCounterDelegate.incrementTempValueBy(personId, CONGESTION_KEY, congestionCaused);
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        double time = event.getTime();
        Id<Link> lid = event.getLinkId();

        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }

        double congestionCaused = this.aggregateCongestionDataPerLinkPerTime.getValueAtTime(lid, time, "congestion");
        externalityCounterDelegate.incrementTempValueBy(personId, CONGESTION_KEY, congestionCaused);
    }
}
