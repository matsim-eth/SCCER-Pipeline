package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateCongestionDataPerLinkPerTime;
import ethz.ivt.externalities.data.AggregateCongestionDataPerPersonPerTime;
import ethz.ivt.vsp.AgentOnLinkInfo;
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
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    private final Vehicle2DriverEventHandler drivers;
    private AggregateCongestionDataPerLinkPerTime aggregateCongestionDataPerLinkPerTime;
    private AggregateCongestionDataPerPersonPerTime aggregateCongestionDataPerPersonPerTime;

    private Map<Id<Person>, AgentOnLinkInfo> person2linkinfo = new HashMap<>();

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers,
                                AggregateCongestionDataPerLinkPerTime aggregateCongestionDataPerLinkPerTime,
                                AggregateCongestionDataPerPersonPerTime aggregateCongestionDataPerPersonPerTime) {
        this.drivers = drivers;
        this.aggregateCongestionDataPerLinkPerTime = aggregateCongestionDataPerLinkPerTime;
        this.aggregateCongestionDataPerPersonPerTime = aggregateCongestionDataPerPersonPerTime;

        // set up person2linkinfo
        scenario.getPopulation().getPersons().keySet().forEach(personId -> {
            person2linkinfo.put(personId, null);
        });

        log.info("Number of congestion bins: " + this.aggregateCongestionDataPerLinkPerTime.getNumBins());
    }

    @Override
    public void handleEvent(CongestionEvent event) {
        int bin = ExternalityUtils.getTimeBin(event.getEmergenceTime(), aggregateCongestionDataPerLinkPerTime.getBinSize());
        aggregateCongestionDataPerLinkPerTime.addValue(event.getLinkId(), bin, "delay", event.getDelay());
        aggregateCongestionDataPerPersonPerTime.addValue(event.getAffectedAgentId(), bin, "delay_experienced", event.getDelay());
        aggregateCongestionDataPerPersonPerTime.addValue(event.getCausingAgentId(), bin, "delay_caused", event.getDelay());
    }
    
	@Override
	public void handleEvent(LinkLeaveEvent event) {
        Id<Vehicle> vid = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionDataPerLinkPerTime.getBinSize());

        if (!vehicleOnLinkDuringTimeBin(vid,linkId, timeBin)) {
            aggregateCongestionDataPerLinkPerTime.addValue(linkId, timeBin, "count", 1.0);
        }
        updateVehicleOnLink(vid, linkId, timeBin);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Vehicle> vid = event.getVehicleId();
		Id<Link> linkId = event.getLinkId();
        int timeBin = ExternalityUtils.getTimeBin(event.getTime(), aggregateCongestionDataPerLinkPerTime.getBinSize());

        aggregateCongestionDataPerLinkPerTime.addValue(linkId, timeBin, "count", 1.0);
        updateVehicleOnLink(vid, linkId, timeBin);

	}

	private boolean vehicleOnLinkDuringTimeBin(Id<Vehicle> vid, Id<Link> linkId, int timeBin) {
        Id<Person> pid = drivers.getDriverOfVehicle(vid);
        if (this.person2linkinfo.get(pid).getSetLinkId().equals(linkId)) {
            return this.person2linkinfo.get(pid).getEnterTime() == timeBin;
        }
        return false;
    }

    private void updateVehicleOnLink(Id<Vehicle> vid, Id<Link> linkId, int timeBin) {
        Id<Person> pid = drivers.getDriverOfVehicle(vid);
        this.person2linkinfo.put(pid, new AgentOnLinkInfo(pid, linkId, (double)timeBin, 0.0));
    }
}
