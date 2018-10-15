package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionField;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CongestionCounter implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
	private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
	private Map<Id<Person>, Double> personLinkEntryTime = new HashMap<>();
	private ExternalityCounter externalityCounterDelegate;
	private Scenario scenario;

	public CongestionCounter(Scenario scenario,
							 AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime,
							ExternalityCounter externalityCounterDelegate) {

		this.scenario = scenario;
    	this.aggregateCongestionDataPerLinkPerTime = aggregateCongestionDataPerLinkPerTime;
    	this.externalityCounterDelegate = externalityCounterDelegate;
        log.info("Number of congestion bins: " + aggregateCongestionDataPerLinkPerTime.getNumBins());
    }
    
    protected void initializeFields() {
		externalityCounterDelegate.addKey(CongestionField.DELAY_CAUSED.getText());
		externalityCounterDelegate.addKey(CongestionField.DELAY_EXPERIENCED.getText());
		externalityCounterDelegate.addKey("clock_time"); //time lost on gps trace
		externalityCounterDelegate.addKey("matsim_time"); //time lost on gps trace
		externalityCounterDelegate.addKey("matsim_delay"); //time lost on gps trace

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
			double linkTravelTime = event.getTime() - linkEntryTime;
			Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
			double matsim_traveltime = l.getLength() / l.getFreespeed();
			//double time_lost = Math.max(0, linkTravelTime - matsim_traveltime);
			double time_lost = linkTravelTime;

			double previousClockTime = externalityCounterDelegate.getTempValue(personId, "clock_time");
			externalityCounterDelegate.putTempValue(personId, "clock_time", previousClockTime + time_lost);

			double previous_matsimTime = externalityCounterDelegate.getTempValue(personId,"matsim_time");
			externalityCounterDelegate.putTempValue(personId,"matsim_time", previous_matsimTime + matsim_traveltime);

			double previous_delay = externalityCounterDelegate.getTempValue(personId,"matsim_delay");
			double delay_on_link = Math.max(0, matsim_traveltime - time_lost); // the delay can't be negative
			externalityCounterDelegate.putTempValue(personId,"matsim_delay", previous_delay + delay_on_link);

			this.personLinkEntryTime.put(personId, null);

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int bin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionDataPerLinkPerTime.getBinSize());
		Id<Link> lid = event.getLinkId();
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

			double previous = externalityCounterDelegate.getTempValue(personId, field.getText());
			externalityCounterDelegate.putTempValue(personId, field.getText(), previous + value);
		}

		//Now store the event for the person
		this.personLinkEntryTime.put(personId, event.getTime());

	}

	private static double congestionScalingFactor(Link link, int queueLength, double gpsTravelTime, double scenarioTravelTime) {
		double freeFlowTravelTime = link.getLength() / link.getFreespeed();
		return (gpsTravelTime - freeFlowTravelTime) / (scenarioTravelTime - freeFlowTravelTime);
	}


}
