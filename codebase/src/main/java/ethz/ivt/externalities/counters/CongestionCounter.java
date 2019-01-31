package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionField;
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
    private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
    private Map<Id<Person>, Double> personLinkEntryTime = new HashMap<>();
    private ExternalityCounter externalityCounterDelegate;
    private Scenario scenario;
    private double freeSpeedFraction = 0.65;

    public CongestionCounter(Scenario scenario,
                             AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime,
                             ExternalityCounter externalityCounterDelegate) {

        this.scenario = scenario;
        this.aggregateCongestionDataPerLinkPerTime = aggregateCongestionDataPerLinkPerTime;
        this.externalityCounterDelegate = externalityCounterDelegate;
        initializeFields();
        log.info("Number of congestion bins: " + aggregateCongestionDataPerLinkPerTime.getNumBins());
    }

    protected void initializeFields() {
        externalityCounterDelegate.addKey(CongestionField.DELAY_CAUSED.getText());
        externalityCounterDelegate.addKey(CongestionField.DELAY_EXPERIENCED.getText());
        externalityCounterDelegate.addKey("actual_travel_time"); //actual travel time
        externalityCounterDelegate.addKey("freespeed_travel_time"); //freespeed travel time
        externalityCounterDelegate.addKey("actual_gps_delay"); //actual delay on gps trace

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
        int bin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionDataPerLinkPerTime.getBinSize());
        Id<Link> lid = event.getLinkId();

        Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
        double freespeedTravelTime = l.getLength() / l.getFreespeed();

        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }

        double count = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, bin, CongestionField.COUNT.getText());

        CongestionField [] congestion_fields = new CongestionField[]{CongestionField.DELAY_CAUSED, CongestionField.DELAY_EXPERIENCED};
        double value = 0;
        for (CongestionField field : congestion_fields){
            if (count > 0) {
                value = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, bin, field.getText()) / count;
            }

            //delay must be larger than certain threshold to be considered as congestion
            if ( value < (freespeedTravelTime * (1 / freeSpeedFraction - 1))) {
                value = 0;
            }

            externalityCounterDelegate.incrementTempValueBy(personId, field.getText(), value);
        }

        //Now store the event for the person
        this.personLinkEntryTime.put(personId, event.getTime());

    }
}
