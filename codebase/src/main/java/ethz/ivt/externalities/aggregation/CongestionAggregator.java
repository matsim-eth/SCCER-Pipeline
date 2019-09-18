package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.vsp.congestion.events.CongestionEvent;
import ethz.ivt.vsp.congestion.handlers.CongestionEventHandler;
import ethz.ivt.vsp.congestion.handlers.CongestionUtils;
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

/**
 * Created by molloyj on 18.07.2017.
 */
public class CongestionAggregator implements CongestionEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler {
    private static final Logger log = Logger.getLogger(CongestionAggregator.class);
    private final Scenario scenario;
    private final Vehicle2DriverEventHandler drivers;
    private double binSize;
    private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;

    private double congestionThresholdRatio = 0.65;

    public CongestionAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers, double binSize) {
        this.scenario = scenario;
        this.drivers = drivers;
        this.binSize = binSize;
        this.aggregateCongestionDataPerLinkPerTime = AggregateDataPerTimeImpl.congestionLink(binSize);
    }

    @Override
    public void handleEvent(CongestionEvent event) {
        // get relevant link information
        Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
        double freespeedTravelTime = link.getLength() / CongestionUtils.getFreeSpeedVelocity(link);

        // get relevant causing and affected agent info
        Id<Person> causingAgentId = event.getCausingAgentId();
        Id<Person> affectedAgentId = event.getAffectedAgentId();

        double causingAgentEnterTime = event.getEmergenceTime(); // time when causing agent entered the link in the past
        double affectedAgentLeaveTime = event.getTime(); // time when the affected agent experiences the delay

        Id<Link> linkId = event.getLinkId(); // event triggered when affected agent leaves link with delay

        double delay = event.getDelay();
        double thresholdDelay = Math.ceil(freespeedTravelTime * ( (1 / congestionThresholdRatio) - 1));

        //delay must be larger than threshold to be considered as congestion
        double congestion = delay;
        if ( delay < thresholdDelay) {
            congestion = 0;
        }

        // record delays on links
        // only store caused delays on link if causing agent is not freight
        if (!causingAgentId.toString().contains("freight")) {
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(linkId, causingAgentEnterTime, "delay_caused", delay);
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(linkId, causingAgentEnterTime, "congestion_caused", congestion);
        }

        // only store experienced delays on link if affected agent is not freight
        if (!affectedAgentId.toString().contains("freight")) {
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(linkId, affectedAgentLeaveTime, "delay_experienced", delay);
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(linkId, affectedAgentLeaveTime, "congestion_experienced", congestion);
        }
    }

    /*
     * We only count the agent once when it enters the link,
     * since we also only count the delay caused once at the time of entry.
     */
    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());

        // only count non-freight agents
        if (!personId.toString().contains("freight")) {
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(event.getLinkId(), event.getTime(), "count_entering", 1);
        }
    }

    /*
     * At the first link, the agent does not have a link enter event, so consider this case.
     */
    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Id<Person> personId = event.getPersonId();

        // only count non-freight agents
        if (!personId.toString().contains("freight")) {
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(event.getLinkId(), event.getTime(), "count_entering", 1);
        }
    }

    /*
     * We only count the agent once when it leaves the link,
     * since we also only count the delay experienced once at the time of exit.
     */
    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());

        // only count non-freight agents
        if (!personId.toString().contains("freight")) {
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(event.getLinkId(), event.getTime(), "count_exiting", 1);
        }
    }

    /*
     * At the last link, the agent does not have a link leave event, so consider this case.
     */
    @Override
    public void handleEvent(PersonArrivalEvent event) {
        Id<Person> personId = event.getPersonId();

        // only count non-freight agents
        if (!personId.toString().contains("freight")) {
            aggregateCongestionDataPerLinkPerTime.addValueAtTime(event.getLinkId(), event.getTime(), "count_exiting", 1);
        }
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

    @Override
    public void reset(int iteration) {

    }
}
