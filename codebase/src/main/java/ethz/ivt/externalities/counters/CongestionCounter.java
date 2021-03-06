package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;

public class CongestionCounter implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {
    private static final Logger log = Logger.getLogger(CongestionCounter.class);
    private Scenario scenario;

    private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
    private Map<Id<Person>, Double> personLinkEntryTime = new HashMap<>();

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
        externalityCounterDelegate.addKey("freespeed_travel_time"); //freespeed travel time
        externalityCounterDelegate.addKey("actual_travel_time"); //actual travel time
        externalityCounterDelegate.addKey("actual_gps_delay"); //actual delay on gps trace

        externalityCounterDelegate.addKey("delay_caused"); //average delay caused
        externalityCounterDelegate.addKey("delay_experienced"); //average delay experienced
        externalityCounterDelegate.addKey("congestion_caused"); //average congestion caused
        externalityCounterDelegate.addKey("congestion_experienced"); //average congestion experienced
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.personLinkEntryTime.put(event.getPersonId(), null);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());

        Double linkEntryTime =  this.personLinkEntryTime.get(personId);
        if (linkEntryTime != null) {
            double actualLinkTravelTime = event.getTime() - linkEntryTime;
            Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
            double freespeedTravelTime = l.getLength() / l.getFreespeed();

            double previousClockTime = externalityCounterDelegate.getTempValue(personId, "actual_travel_time");
            externalityCounterDelegate.putTempValue(personId, "actual_travel_time", previousClockTime + actualLinkTravelTime);

            double previous_matsimTime = externalityCounterDelegate.getTempValue(personId,"freespeed_travel_time");
            externalityCounterDelegate.putTempValue(personId,"freespeed_travel_time", previous_matsimTime + freespeedTravelTime);

            double previous_delay = externalityCounterDelegate.getTempValue(personId,"actual_gps_delay");

            // actualLinkTravelTime should be larger than the freespeedTravelTime
            double delay_on_link = Math.max(0, actualLinkTravelTime - freespeedTravelTime); // the delay can't be negative
            externalityCounterDelegate.putTempValue(personId,"actual_gps_delay", previous_delay + delay_on_link);
            this.personLinkEntryTime.put(personId, null);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        double time = event.getTime();
        Id<Link> lid = event.getLinkId();

        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }

        double count = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, time, "count");

        if (count > 0) {
            double delayCaused = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, time, "delay_caused") / count;
            double delayExperienced = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, time, "delay_experienced") / count;
            double congestionCaused = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, time, "congestion_caused") / count;
            double congestionExperienced = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, time, "congestion_experienced") / count;

            externalityCounterDelegate.incrementTempValueBy(personId, "delay_caused", delayCaused);
            externalityCounterDelegate.incrementTempValueBy(personId, "delay_experienced", delayExperienced);
            externalityCounterDelegate.incrementTempValueBy(personId, "congestion_caused", congestionCaused);
            externalityCounterDelegate.incrementTempValueBy(personId, "congestion_experienced", congestionExperienced);
        }

        //Now store the event for the person
        this.personLinkEntryTime.put(personId, event.getTime());

    }
}
