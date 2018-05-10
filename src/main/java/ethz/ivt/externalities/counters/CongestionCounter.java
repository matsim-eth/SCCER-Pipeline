package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionField;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.nio.file.Path;

public class CongestionCounter extends ExternalityCounter {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
	private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;

    public CongestionCounter(Scenario scenario, String date, AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime) {
    	super(scenario, date);
    	this.aggregateCongestionDataPerLinkPerTime = aggregateCongestionDataPerLinkPerTime;
        log.info("Number of congestion bins: " + aggregateCongestionDataPerLinkPerTime.getNumBins());
    }
    
    @Override
    protected void initializeFields() {
    	super.initializeFields();
        keys.add(CongestionField.DELAY_CAUSED.getText());
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
        double delay = this.aggregateCongestionDataPerLinkPerTime.getValue(lid, bin, CongestionField.DELAY_CAUSED.getText());

        double avg_link_delay = delay / count;
        if (Double.isNaN(avg_link_delay)) avg_link_delay = 0.0 ;

		double previous = this.tempValues.get(personId).get(CongestionField.DELAY_CAUSED.getText());
		this.tempValues.get(personId).put(CongestionField.DELAY_CAUSED.getText(), previous + avg_link_delay);
		
		super.handleEvent(event); //add distance
	}


	public void writeCsvFile(Path outputPath, String filename) {
		Path outputFileName = outputPath.resolve(filename + "_congestion.csv");
		super.writeCsvFile(outputFileName);
	}

}
