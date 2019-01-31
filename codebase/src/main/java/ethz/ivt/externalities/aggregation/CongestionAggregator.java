package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionField;
import ethz.ivt.vsp.AgentOnLinkInfo;
import ethz.ivt.vsp.CongestionEvent;
import ethz.ivt.vsp.handlers.CongestionEventHandler;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    public AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
    public AggregateDataPerTimeImpl<Person> aggregateCongestionDataPerPersonPerTime;

    private Map<Id<Person>, AgentOnLinkInfo> person2linkinfo = new HashMap<>();

    private double congestionThresholdRatio = 0.65;

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers) {
        this.scenario = scenario;
        this.drivers = drivers;

        List<String> attributesPerLink = new LinkedList<>();
        attributesPerLink.add(CongestionField.COUNT.getText());
        attributesPerLink.add(CongestionField.DELAY_CAUSED.getText());
        attributesPerLink.add(CongestionField.DELAY_EXPERIENCED.getText());
        attributesPerLink.add("congestion_caused");
        attributesPerLink.add("congestion_experienced");

        List<String> attributesPerPerson = new LinkedList<>();
        attributesPerPerson.add(CongestionField.DELAY_CAUSED.getText());
        attributesPerPerson.add(CongestionField.DELAY_EXPERIENCED.getText());
        attributesPerPerson.add("congestion_caused");
        attributesPerPerson.add("congestion_experienced");

        String outputFileNamePerLink = "aggregate_delay_per_link_per_time.csv";
        String outputFileNamePerPerson = "aggregate_delay_per_person_per_time.csv";

        this.aggregateCongestionDataPerLinkPerTime = new AggregateDataPerTimeImpl<Link>(3600, scenario.getNetwork().getLinks().keySet(), attributesPerLink, outputFileNamePerLink);
        this.aggregateCongestionDataPerPersonPerTime = new AggregateDataPerTimeImpl<Person>(3600, scenario.getPopulation().getPersons().keySet(), attributesPerPerson, outputFileNamePerPerson);

        // set up person2linkinfo
        scenario.getPopulation().getPersons().keySet().forEach(personId -> {
            person2linkinfo.put(personId, new AgentOnLinkInfo(personId, null, -1.0, -1.0));
        });

        log.info("Number of congestion bins: " + this.aggregateCongestionDataPerLinkPerTime.getNumBins());
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

        // compute timebin
        int causingAgentTimeBin = ExternalityUtils.getTimeBin(causingAgentEnterTime, aggregateCongestionDataPerLinkPerTime.getBinSize());
        int affectedAgentTimeBin = ExternalityUtils.getTimeBin(affectedAgentLeaveTime, aggregateCongestionDataPerLinkPerTime.getBinSize());

        // store delay info
        aggregateCongestionDataPerLinkPerTime.addValue(causingAgentLinkId, causingAgentTimeBin, CongestionField.DELAY_CAUSED.getText(), delay);
        aggregateCongestionDataPerLinkPerTime.addValue(affectedAgentLinkId, affectedAgentTimeBin, CongestionField.DELAY_EXPERIENCED.getText() , delay);
        aggregateCongestionDataPerLinkPerTime.addValue(causingAgentLinkId, causingAgentTimeBin, "congestion_caused", congestion);
        aggregateCongestionDataPerLinkPerTime.addValue(affectedAgentLinkId, affectedAgentTimeBin, "congestion_experienced", congestion);

        aggregateCongestionDataPerPersonPerTime.addValue(causingAgentId, causingAgentTimeBin, CongestionField.DELAY_CAUSED.getText(), delay);
        aggregateCongestionDataPerPersonPerTime.addValue(affectedAgentId, affectedAgentTimeBin, CongestionField.DELAY_EXPERIENCED.getText(), delay);
        aggregateCongestionDataPerPersonPerTime.addValue(causingAgentId, causingAgentTimeBin, "congestion_caused", congestion);
        aggregateCongestionDataPerPersonPerTime.addValue(affectedAgentId, affectedAgentTimeBin, "congestion_experienced", congestion);

    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        double enterTime = event.getTime();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();

        Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
        double freespeedTravelTime = l.getLength() / l.getFreespeed();
        double freespeedLeaveTime = enterTime + freespeedTravelTime;

        AgentOnLinkInfo agentOnLinkInfo = new AgentOnLinkInfo(personId, linkId, enterTime, freespeedLeaveTime);
        person2linkinfo.replace(personId, agentOnLinkInfo);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Link> linkId = event.getLinkId();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());

        double leaveTime = event.getTime();
        double enterTime = person2linkinfo.get(personId).getEnterTime();

        updateVehicleCount(enterTime, leaveTime, linkId);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        double enterTime = event.getTime();
        Id<Person> personId = event.getPersonId();
        Id<Link> linkId = event.getLinkId();

        Link l = scenario.getNetwork().getLinks().get(event.getLinkId());
        double freespeedTravelTime = l.getLength() / l.getFreespeed();
        double freespeedLeaveTime = enterTime + freespeedTravelTime;

        AgentOnLinkInfo agentOnLinkInfo = new AgentOnLinkInfo(personId, linkId, enterTime, freespeedLeaveTime);
        person2linkinfo.replace(personId, agentOnLinkInfo);
    }

    public void updateVehicleCount(double enterTime, double leaveTime, Id<Link> linkId) {

        double binSize = aggregateCongestionDataPerLinkPerTime.getBinSize();

        int enterTimeBin = ExternalityUtils.getTimeBin(enterTime, binSize);
        int leaveTimeBin = ExternalityUtils.getTimeBin(leaveTime, binSize);

        // if agent on link during only one timebin, increment that timebin by 1
        if (leaveTimeBin == enterTimeBin) {
            aggregateCongestionDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CongestionField.COUNT.getText(), 1.0);

        }
        // if agent on link during two timebins, increment each one by 1
        else if (leaveTimeBin > enterTimeBin) {
            aggregateCongestionDataPerLinkPerTime.addValue(linkId,
                    enterTimeBin, CongestionField.COUNT.getText(), 1.0);
            aggregateCongestionDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CongestionField.COUNT.getText(), 1.0);
        }
        //TODO : Think what should happen for counts if arrival then departure occur within same timebin
    }
}
