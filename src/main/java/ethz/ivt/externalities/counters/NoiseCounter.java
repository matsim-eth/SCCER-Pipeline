package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateNoiseData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

public class NoiseCounter extends ExternalityCounter {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
    private AggregateNoiseData aggregateNoiseData;

	public NoiseCounter(Scenario scenario, Vehicle2DriverEventHandler drivers, String date, AggregateNoiseData aggregateNoiseData) {
    	super(scenario, drivers, date);
        this.aggregateNoiseData = aggregateNoiseData;
        log.info("Number of congestion bins: " + aggregateNoiseData.getNumBins());
    }

    @Override
    protected void initializeFields() {
        super.initializeFields();
        keys.add("NoiseEmissions");
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        int bin = ExternalityUtils.getTimeBin(event.getTime(), aggregateNoiseData.getBinSize());
        Id<Link> lid = event.getLinkId();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }
        double noise = this.aggregateNoiseData.getMeanValue(lid, bin);;
        double previous = this.tempValues.get(personId).get("NoiseEmissions");
        this.tempValues.get(personId).put("NoiseEmissions", previous + noise);

        super.handleEvent(event); //add distance
    }

    public void writeCsvFile(String outputPath, String date) {
        String outputFileName = date + "_noise.csv";
        super.writeCsvFile(outputPath, outputFileName);
    }
}
