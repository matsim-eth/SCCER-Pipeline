package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateCongestionData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

public class CongestionCounter extends ExternalityCounter {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
	private AggregateCongestionData aggregateCongestionData;

    public CongestionCounter(Scenario scenario, Vehicle2DriverEventHandler drivers, String date, AggregateCongestionData aggregateCongestionData) {
    	super(scenario, drivers, date);
    	this.aggregateCongestionData = aggregateCongestionData;
        log.info("Number of congestion bins: " + aggregateCongestionData.getNumBins());
    }
    
    @Override
    protected void initializeFields() {
    	super.initializeFields();
        keys.add("Delay");
    }

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int bin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionData.getBinSize());
		Id<Link> lid = event.getLinkId();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }
		double delay = this.aggregateCongestionData.getMeanValue(lid, bin);;
		double previous = this.tempValues.get(personId).get("Delay");
		this.tempValues.get(personId).put("Delay", previous + delay);
		
		super.handleEvent(event); //add distance
	}

	public void writeCsvFile(String outputPath, String date) {
		String outputFileName = date + "_congestion.csv";
		super.writeCsvFile(outputPath, outputFileName);
	}

}
