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
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CongestionCounter extends ExternalityCounter implements LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
	private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
	private Map<Id<Person>, Double> personLinkEntryTime = new HashMap<>();

	private final double costPerVehicleHourCar = 42.4;

	public CongestionCounter(Scenario scenario, String date, AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime) {
    	super(scenario, date);
    	this.aggregateCongestionDataPerLinkPerTime = aggregateCongestionDataPerLinkPerTime;
        log.info("Number of congestion bins: " + aggregateCongestionDataPerLinkPerTime.getNumBins());
    }
    
    @Override
    protected void initializeFields() {
    	super.initializeFields();
		keys.add(CongestionField.DELAY_CAUSED.getText());
		keys.add(CongestionField.DELAY_EXPERIENCED.getText());
		keys.add("clock_time"); //time lost on gps trace
		keys.add("matsim_time"); //time lost on gps trace
		keys.add("matsim_delay"); //time lost on gps trace
		keys.add("delay_cost_caused");
    }

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personLinkEntryTime.put(event.getPersonId(), null);
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> personId = getDriverOfVehicle(event.getVehicleId());
		Double linkEntryTime =  this.personLinkEntryTime.get(personId);
		if (linkEntryTime != null) {
			double linkTravelTime = event.getTime() - linkEntryTime;
			Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
			double matsim_traveltime = l.getLength() / l.getFreespeed();
			//double time_lost = Math.max(0, linkTravelTime - matsim_traveltime);
			double time_lost = linkTravelTime;

			double previousClockTime = this.tempValues.get(personId).get("clock_time");
			this.tempValues.get(personId).put("clock_time", previousClockTime + time_lost);

			double previous_matsimTime = this.tempValues.get(personId).get("matsim_time");
			this.tempValues.get(personId).put("matsim_time", previous_matsimTime + matsim_traveltime);

			double previous_delay = this.tempValues.get(personId).get("matsim_delay");
			double delay_on_link = Math.max(0, matsim_traveltime - time_lost); // the delay can't be negative
			this.tempValues.get(personId).put("matsim_delay", previous_delay + delay_on_link);

			this.personLinkEntryTime.put(personId, null);

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int bin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionDataPerLinkPerTime.getBinSize());
		Id<Link> lid = event.getLinkId();
        Id<Person> personId = getDriverOfVehicle(event.getVehicleId());
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

			double previous = this.tempValues.get(personId).get(field.getText());
			this.tempValues.get(personId).put(field.getText(), previous + value);
		}

		// Compute caused delay costs
		double previous = this.tempValues.get(personId).get("delay_cost_caused");
		double cost = this.tempValues.get(personId).get(CongestionField.DELAY_CAUSED) * costPerVehicleHourCar;
		this.tempValues.get(personId).put("delay_cost_caused", previous + cost);
		
		//Now store the event for the person
		this.personLinkEntryTime.put(personId, event.getTime());

		super.handleEvent(event); //add distance
	}


	public void writeCsvFile(Path outputPath, String filename) {
		Path outputFileName = outputPath.resolve(filename + "_congestion.csv");
		super.writeCsvFile(outputFileName);
	}

}
