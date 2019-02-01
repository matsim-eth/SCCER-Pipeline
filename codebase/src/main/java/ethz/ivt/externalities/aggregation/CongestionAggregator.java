package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import ethz.ivt.vsp.AgentOnLinkInfo;
import ethz.ivt.vsp.CongestionEvent;
import ethz.ivt.vsp.handlers.CongestionEventHandler;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    private double binSize;
    private Map<Id<Person>, AgentOnLinkInfo> person2linkinfo = new HashMap<>();
    private Map<Id<Link>, CongestionPerTime> aggregateCongestionDataPerLinkPerTime = new HashMap<>();
    private Map<Id<Person>, CongestionPerTime> aggregateCongestionDataPerPersonPerTime = new HashMap<>();

    private double congestionThresholdRatio = 0.65;

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers, double binSize) {
        this.scenario = scenario;
        this.drivers = drivers;
        this.binSize = binSize;
        
        // set up person2linkinfo
        scenario.getPopulation().getPersons().keySet().forEach(personId -> {
            person2linkinfo.put(personId, new AgentOnLinkInfo(personId, null, -1.0, -1.0));
        });

        // set up aggregateCongestionData
        for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
            aggregateCongestionDataPerLinkPerTime.putIfAbsent(linkId, new CongestionPerTime(this.binSize));
        }
        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            aggregateCongestionDataPerPersonPerTime.putIfAbsent(personId, new CongestionPerTime(this.binSize));
        }
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

        // store delay info
        aggregateCongestionDataPerLinkPerTime.get(causingAgentLinkId).addDelayCausedAtTime(delay, causingAgentEnterTime);
        aggregateCongestionDataPerLinkPerTime.get(affectedAgentLinkId).addDelayExperiencedAtTime(delay, affectedAgentLeaveTime);
        aggregateCongestionDataPerLinkPerTime.get(causingAgentLinkId).addCongestionCausedAtTime(congestion, causingAgentEnterTime);
        aggregateCongestionDataPerLinkPerTime.get(affectedAgentLinkId).addCongestionExperiencedAtTime(congestion, affectedAgentLeaveTime);

        aggregateCongestionDataPerPersonPerTime.get(causingAgentId).addDelayCausedAtTime(delay, causingAgentEnterTime);
        aggregateCongestionDataPerPersonPerTime.get(affectedAgentId).addDelayExperiencedAtTime(delay, affectedAgentLeaveTime);
        aggregateCongestionDataPerPersonPerTime.get(causingAgentId).addCongestionCausedAtTime(congestion, causingAgentEnterTime);
        aggregateCongestionDataPerPersonPerTime.get(affectedAgentId).addCongestionExperiencedAtTime(congestion, affectedAgentLeaveTime);
    }

    /*
     * We only count the agent once when it enters the link,
     * since we also only count the delay caused once at the time of entry.
     */
    @Override
    public void handleEvent(LinkEnterEvent event) {
        double enterTime = event.getTime();
        Id<Link> linkId = event.getLinkId();
        aggregateCongestionDataPerLinkPerTime.get(linkId).addCountAtTime(1.0, enterTime);
    }

    /*
     * At the first link, the agent does not have a link enter event, so consider this case.
     */
    @Override
    public void handleEvent(PersonDepartureEvent event) {
        double enterTime = event.getTime();
        Id<Link> linkId = event.getLinkId();
        aggregateCongestionDataPerLinkPerTime.get(linkId).addCountAtTime(1.0, enterTime);
    }

    public double getBinSize() {
        return binSize;
    }

    public double getCongestionThresholdRatio() {
        return congestionThresholdRatio;
    }

    public Map<Id<Link>, CongestionPerTime> getAggregateCongestionDataPerLinkPerTime() {
        return aggregateCongestionDataPerLinkPerTime;
    }

    public Map<Id<Person>, CongestionPerTime> getAggregateCongestionDataPerPersonPerTime() {
        return aggregateCongestionDataPerPersonPerTime;
    }
}
