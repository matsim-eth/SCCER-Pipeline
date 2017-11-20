package ethz.ivt.aggregation;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import com.opencsv.CSVReader;

public class CongestionCounter extends ExternalityCounter {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
    protected Map<Id<Link>, double[]> linkId2timeBin2values = new HashMap<>();

	protected final int num_bins;
	protected int binSize_s;

    public CongestionCounter(Scenario scenario, Vehicle2DriverEventHandler drivers, int binSize_s) {
    	super(scenario, drivers);

        this.num_bins = (int) (30 * 3600 / binSize_s);
        this.binSize_s = binSize_s;

        setUpBinsForLinks(scenario);
        log.info("Number of congestion bins: " + num_bins);
    }
    
    @Override
    protected void initializeFields() {
    	super.initializeFields();
        keys.add("Delay");
    }

    protected void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            linkId2timeBin2values.put(l, new double[num_bins]);
        });
    }

    public void loadCsvFile(String input) {
    	CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(input), ';');
			// read line by line
			String[] record = null;
			int count = 0;
			try {
				while ((record = reader.readNext()) != null) {
					if(count>0) {
						Id<Link> lid = Id.createLinkId(record[0]);
						int bin = Integer.parseInt(record[1]);
						double delay = Double.parseDouble(record[2]);
						this.linkId2timeBin2values.get(lid)[bin] = delay;
					}
					count ++;
				}
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				reader.close();
			} catch (IOException e) {
				log.error("Error while closing CSV file reader!");
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			log.error("CSV file not found!");
			e.printStackTrace();
		}
    }

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int bin = ExternalityUtils.getTimeBin(event.getTime(), this.binSize_s);
		Id<Link> lid = event.getLinkId();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }
		double delay = this.linkId2timeBin2values.get(lid)[bin];
		double previous = this.tempValues.get(personId).get("Delay");
		this.tempValues.get(personId).put("Delay", previous + delay);
		
		super.handleEvent(event); //add distance
	}

	public void writeCsvFile(String outputPath, String date) {
		String outputFileName = date + "_congestion.csv";
		super.writeCsvFile(outputPath, outputFileName);
	}

}