package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionPerLinkField;
import ethz.ivt.externalities.data.CongestionPerPersonField;
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
    public AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
    public AggregateDataPerTimeImpl<Person> aggregateCongestionDataPerPersonPerTime;

    private Map<Id<Person>, AgentOnLinkInfo> person2linkinfo = new HashMap<>();

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers) {
        this.drivers = drivers;

        List<String> attributes1 = new LinkedList<>();
        attributes1.add(CongestionPerLinkField.COUNT.getText());
        attributes1.add(CongestionPerLinkField.DELAY.getText());

        List<String> attributes2 = new LinkedList<>();
        attributes2.add(CongestionPerPersonField.DELAY_CAUSED.getText());
        attributes2.add(CongestionPerPersonField.DELAY_EXPERIENCED.getText());

        String outputFileName1 = "aggregate_delay_per_link_per_time.csv";
        String outputFileName2 = "aggregate_delay_per_person_per_time.csv";

        this.aggregateCongestionDataPerLinkPerTime = new AggregateDataPerTimeImpl<Link>(3600, scenario.getNetwork().getLinks().keySet(), attributes1, outputFileName1);
        this.aggregateCongestionDataPerPersonPerTime = new AggregateDataPerTimeImpl<Person>(3600, scenario.getPopulation().getPersons().keySet(), attributes2, outputFileName2);

        // set up person2linkinfo
        scenario.getPopulation().getPersons().keySet().forEach(personId -> {
            person2linkinfo.put(personId, new AgentOnLinkInfo(personId, null, 0.0, 0.0));
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
        if (linkId.equals(this.person2linkinfo.get(pid).getSetLinkId())) {
            return this.person2linkinfo.get(pid).getEnterTime() == timeBin;
        }
        return false;
    }

    private void updateVehicleOnLink(Id<Vehicle> vid, Id<Link> linkId, int timeBin) {
        Id<Person> pid = drivers.getDriverOfVehicle(vid);
        this.person2linkinfo.put(pid, new AgentOnLinkInfo(pid, linkId, (double)timeBin, 0.0));
    }
}
