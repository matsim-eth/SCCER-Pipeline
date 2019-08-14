package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.vsp.congestion.AgentOnLinkInfo;
import ethz.ivt.vsp.congestion.events.CongestionEvent;
import ethz.ivt.vsp.congestion.handlers.CongestionEventHandler;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    private double binSize;
    private Map<Id<Person>, AgentOnLinkInfo> person2linkinfo = new HashMap<>();
    private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
    private AggregateDataPerTimeImpl<Person> aggregateCongestionDataPerPersonPerTime;

    private double congestionThresholdRatio = 0.65;

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers, double binSize) {
        this.scenario = scenario;
        this.drivers = drivers;
        this.binSize = binSize;
        aggregateCongestionDataPerLinkPerTime = AggregateDataPerTimeImpl.congestionLink(binSize);
        aggregateCongestionDataPerPersonPerTime =  AggregateDataPerTimeImpl.congestionPerson(binSize);

        // set up person2linkinfo
        scenario.getPopulation().getPersons().keySet().forEach(personId -> {
            person2linkinfo.put(personId, new AgentOnLinkInfo(personId, null, -1.0, -1.0));
        });

    }

    @Override
    public void handleEvent(CongestionEvent event) {
        // get relevant link information
        Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
        double freespeedTravelTime = l.getLength() / l.getFreespeed();

        // get relevant causing and affected agent info
        Id<Person> causingAgentId = event.getCausingAgentId();
        Id<Person> affectedAgentId = event.getAffectedAgentId();

        double causingAgentEnterTime = event.getEmergenceTime(); // time when causing agent entered the link in the past
        double affectedAgentLeaveTime = event.getTime(); // time when the affected agent experiences the delay

        Id<Link> causingAgentLinkId = event.getLinkId();
        Id<Link> affectedAgentLinkId = event.getLinkId(); // event triggered when affected agent leaves link with delay

        double delay = event.getDelay();
        double congestion = delay;
        double thresholdDelay = (freespeedTravelTime * ( (1 / congestionThresholdRatio) - 1));

        //delay must be larger than threshold to be considered as congestion
        if ( congestion < thresholdDelay) {
            congestion = 0;
        }

        // record delays on links
        aggregateCongestionDataPerLinkPerTime.addValueAtTime(affectedAgentLinkId,affectedAgentLeaveTime, "delay_experienced", delay);
        aggregateCongestionDataPerLinkPerTime.addValueAtTime(affectedAgentLinkId,affectedAgentLeaveTime, "congestion_experienced", congestion);
        // only store caused delay on link if causing agents is not freight
        if (!causingAgentId.toString().contains("freight")) {
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(causingAgentLinkId,causingAgentEnterTime, "delay_caused", delay);
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(causingAgentLinkId,causingAgentEnterTime, "congestion_caused", congestion);
        }

        // record delays by person
        aggregateCongestionDataPerPersonPerTime.addValueAtTime(causingAgentId,causingAgentEnterTime, "delay_caused", delay);
        aggregateCongestionDataPerPersonPerTime.addValueAtTime(affectedAgentId,affectedAgentLeaveTime, "delay_experienced", delay);
        aggregateCongestionDataPerPersonPerTime.addValueAtTime(causingAgentId,causingAgentEnterTime, "congestion_caused", congestion);
        aggregateCongestionDataPerPersonPerTime.addValueAtTime(affectedAgentId,affectedAgentLeaveTime, "congestion_experienced", congestion);

        // TODO: fix this
        // add 1 to person count values for each congestion event (also need a count for the write to work)
        aggregateCongestionDataPerPersonPerTime.addValueAtTime(causingAgentId,causingAgentEnterTime, "count", 1);
        aggregateCongestionDataPerPersonPerTime.addValueAtTime(affectedAgentId,affectedAgentLeaveTime, "count", 1);
    }

    /*
     * We only count the agent once when it enters the link,
     * since we also only count the delay caused once at the time of entry.
     */
    @Override
    public void handleEvent(LinkEnterEvent event) {
        double enterTime = event.getTime();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();

        Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
        double freespeedTravelTime = l.getLength() / l.getFreespeed();
        double freespeedLeaveTime = enterTime + freespeedTravelTime;

        if (person2linkinfo.get(personId).getSetLinkId() == null || !person2linkinfo.get(personId).getSetLinkId().toString().equals(linkId.toString())) {
            AgentOnLinkInfo agentOnLinkInfo = new AgentOnLinkInfo(personId, linkId, enterTime, freespeedLeaveTime);
            person2linkinfo.replace(personId, agentOnLinkInfo);

            // only count non-freight agents
            if (!personId.toString().contains("freight")) {
                aggregateCongestionDataPerLinkPerTime.addValueAtTime(linkId, enterTime, "count", 1);
            }
        }
    }

    /*
     * At the first link, the agent does not have a link enter event, so consider this case.
     */
    @Override
    public void handleEvent(PersonDepartureEvent event) {
        double enterTime = event.getTime();
        Id<Person> personId = event.getPersonId();
        Id<Link> linkId = event.getLinkId();

        Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
        double freespeedTravelTime = l.getLength() / l.getFreespeed();
        double freespeedLeaveTime = enterTime + freespeedTravelTime;

        if (person2linkinfo.get(personId).getSetLinkId() == null || !person2linkinfo.get(personId).getSetLinkId().toString().equals(linkId.toString())) {
            AgentOnLinkInfo agentOnLinkInfo = new AgentOnLinkInfo(personId, linkId, enterTime, freespeedLeaveTime);
            person2linkinfo.replace(personId, agentOnLinkInfo);

            // only count non-freight agents
            if (!personId.toString().contains("freight")) {
                aggregateCongestionDataPerLinkPerTime.addValueAtTime(linkId, enterTime, "count", 1);
            }
        }
    }

    /*
     * Set agent's current link to null when leaving.
     */
    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        AgentOnLinkInfo agentOnLinkInfo = new AgentOnLinkInfo(personId, null, -1.0, -1.0);
        person2linkinfo.replace(personId, agentOnLinkInfo);
    }

    /*
     * Set agent's current link to null when arriving at location.
     */
    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Id<Person> personId = event.getPersonId();
        AgentOnLinkInfo agentOnLinkInfo = new AgentOnLinkInfo(personId, null, -1.0, -1.0);
        person2linkinfo.replace(personId, agentOnLinkInfo);
    }

    public double getBinSize() {
        return binSize;
    }

    public double getCongestionThresholdRatio() {
        return congestionThresholdRatio;
    }

    public AggregateDataPerTimeImpl<Link> getAggregateCongestionDataPerLinkPerTime() {
        return aggregateCongestionDataPerLinkPerTime;
    }

    public AggregateDataPerTimeImpl<Person> getAggregateCongestionDataPerPersonPerTime() {
        return aggregateCongestionDataPerPersonPerTime;
    }
}
