package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CongestionField;
import ethz.ivt.vsp.AgentOnLinkInfo;
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
import ethz.ivt.vsp.CongestionEvent;
import ethz.ivt.vsp.handlers.CongestionEventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.*;

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

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers) {
        this.scenario = scenario;
        this.drivers = drivers;

        List<String> attributes1 = new LinkedList<>();
        attributes1.add(CongestionField.COUNT.getText());
        attributes1.add(CongestionField.DELAY_CAUSED.getText());
        attributes1.add(CongestionField.DELAY_EXPERIENCED.getText());

        List<String> attributes2 = new LinkedList<>();
        attributes2.add(CongestionField.DELAY_CAUSED.getText());
        attributes2.add(CongestionField.DELAY_EXPERIENCED.getText());

        String outputFileName1 = "aggregate_delay_per_link_per_time.csv";
        String outputFileName2 = "aggregate_delay_per_person_per_time.csv";

        this.aggregateCongestionDataPerLinkPerTime = new AggregateDataPerTimeImpl<Link>(3600, scenario.getNetwork().getLinks().keySet(), attributes1, outputFileName1);
        this.aggregateCongestionDataPerPersonPerTime = new AggregateDataPerTimeImpl<Person>(3600, scenario.getPopulation().getPersons().keySet(), attributes2, outputFileName2);

        // set up person2linkinfo
        scenario.getPopulation().getPersons().keySet().forEach(personId -> {
            person2linkinfo.put(personId, new AgentOnLinkInfo(personId, null, -1.0, -1.0));
        });

        log.info("Number of congestion bins: " + this.aggregateCongestionDataPerLinkPerTime.getNumBins());
    }

    @Override
    public void handleEvent(CongestionEvent event) {

        // get relevant causing and affected agent info
        Id<Person> causingAgentId = event.getCausingAgentId();
        Id<Person> affectedAgentId = event.getAffectedAgentId();

        double causingAgentEnterTime = event.getEmergenceTime(); // time when causing agent entered the link in the past
        double affectedAgentLeaveTime = event.getTime(); // time when the affected agent experiences the delay

        Id<Link> causingAgentLinkId = person2linkinfo.get(causingAgentId).getSetLinkId();
        Id<Link> affectedAgentLinkId = event.getLinkId(); // event triggered when affected agent leaves link with delay

        if (!affectedAgentLinkId.equals(person2linkinfo.get(affectedAgentId).getSetLinkId())) {
            log.warn("Affected agent do not links match! This should be understood!");
        }

        double delay = event.getDelay();

        // compute timebin
        int causingAgentTimeBin = ExternalityUtils.getTimeBin(causingAgentEnterTime, aggregateCongestionDataPerLinkPerTime.getBinSize());
        int affectedAgentTimeBin = ExternalityUtils.getTimeBin(affectedAgentLeaveTime, aggregateCongestionDataPerLinkPerTime.getBinSize());

        // store delay info
        aggregateCongestionDataPerLinkPerTime.addValue(causingAgentLinkId, causingAgentTimeBin, CongestionField.DELAY_CAUSED.getText(), delay);
        aggregateCongestionDataPerLinkPerTime.addValue(affectedAgentLinkId, affectedAgentTimeBin, CongestionField.DELAY_EXPERIENCED.getText() , delay);

        aggregateCongestionDataPerPersonPerTime.addValue(causingAgentId, causingAgentTimeBin, CongestionField.DELAY_CAUSED.getText(), delay);
        aggregateCongestionDataPerPersonPerTime.addValue(affectedAgentId, affectedAgentTimeBin, CongestionField.DELAY_EXPERIENCED.getText(), delay);
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        double enterTime = event.getTime();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();
        updateAgentOnLinkInfo(enterTime, personId, linkId);
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
        updateAgentOnLinkInfo(enterTime, personId, linkId);
        //TODO : Think what should happen for counts if arrival then departure occur within same timebin
    }

    public void updateAgentOnLinkInfo(double enterTime, Id<Person> personId, Id<Link> linkId) {
        Link link = this.scenario.getNetwork().getLinks().get(linkId);
        double length = link.getLength();
        double freeSpeed = link.getFreespeed();
        double freeSpeedLeaveTime = enterTime + length / freeSpeed;
        this.person2linkinfo.replace(personId, new AgentOnLinkInfo(personId, linkId, enterTime, freeSpeedLeaveTime));
        //TODO : Think what should happen for counts if arrival then departure occur within same timebin
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
