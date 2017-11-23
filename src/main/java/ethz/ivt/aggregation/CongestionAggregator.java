package ethz.ivt.aggregation;

import ethz.ivt.aggregation.data.AggregateCongestionData;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import ethz.ivt.vsp.CongestionEvent;
import ethz.ivt.vsp.handlers.CongestionEventHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    
//    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    private AggregateCongestionData aggregateCongestionData;

    protected Map<Id<Link>, Map<Integer, List<Id<Person>>>> linkId2timeBin2personId = new HashMap<>();

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers, AggregateCongestionData acd) {
//        this.scenario = scenario;
        this.drivers = drivers;
        this.aggregateCongestionData = acd;

        setUpBinsForLinks(scenario);
        log.info("Number of congestion bins: " + aggregateCongestionData.getNumBins());
    }

    protected void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            linkId2timeBin2personId.put(l, new HashMap<>());
            for(int bin = 0; bin<aggregateCongestionData.getNumBins(); bin++) {
            	linkId2timeBin2personId.get(l).put(bin, new ArrayList<>());
            }
        });
    }

    @Override
    public void handleEvent(CongestionEvent event) {
        int bin = ExternalityUtils.getTimeBin(event.getEmergenceTime(), aggregateCongestionData.getBinSize());
        aggregateCongestionData.getLinkId2timeBin2values().get(event.getLinkId()).get("value")[bin] += event.getDelay();
    }
    
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int bin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionData.getBinSize());
		Id<Person> pid = drivers.getDriverOfVehicle(event.getVehicleId());
        if(!this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).contains(pid) ) {
        	this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).add(pid);
            aggregateCongestionData.getLinkId2timeBin2values().get(event.getLinkId()).get("count")[bin] += 1.0;
        }
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		int bin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionData.getBinSize());
		Id<Person> pid = drivers.getDriverOfVehicle(event.getVehicleId());
        if(!this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).contains(pid) ) {
        	this.linkId2timeBin2personId.get(event.getLinkId()).get(bin).add(pid);
            aggregateCongestionData.getLinkId2timeBin2values().get(event.getLinkId()).get("count")[bin] += 1.0;
        }
	}
}
