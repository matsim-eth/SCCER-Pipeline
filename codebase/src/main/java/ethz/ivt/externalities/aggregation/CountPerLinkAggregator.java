package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.ExternalityUtils;
import ethz.ivt.externalities.counters.ExternalityCounter;
import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.CountPerLinkField;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CountPerLinkAggregator implements LinkEnterEventHandler, LinkLeaveEventHandler {
    private static final Logger log = Logger.getLogger(ExternalityCounter.class);
    protected final Scenario scenario;
    protected final Vehicle2DriverEventHandler drivers;
    public AggregateDataPerTimeImpl<Link> aggregateCountDataPerLinkPerTime;
    Map<Id<Person>, Double> person2enterTime = new HashMap<>();

    public CountPerLinkAggregator(Scenario scenario, Vehicle2DriverEventHandler drivers) {
        this.scenario = scenario;
        this.drivers = drivers;

        List<String> attributes = new LinkedList<>();
        attributes.add(CountPerLinkField.COUNT.getText());
        attributes.add(CountPerLinkField.DISTANCE.getText());
        attributes.add(CountPerLinkField.TIME.getText());

        String outputFileName = "aggregate_counts_per_link_per_time.csv";

        this.aggregateCountDataPerLinkPerTime = new AggregateDataPerTimeImpl<Link>(3600,
                scenario.getNetwork().getLinks().keySet(),
                attributes,
                outputFileName);

        scenario.getPopulation().getPersons().keySet().forEach(personId -> {
            person2enterTime.put(personId, 0.0);
        });
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        double enterTime = event.getTime();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());
        person2enterTime.replace(personId, enterTime);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Link> linkId = event.getLinkId();
        Id<Person> personId = drivers.getDriverOfVehicle(event.getVehicleId());

        double leaveTime = event.getTime();
        double enterTime = person2enterTime.get(personId);

        double dt = leaveTime - enterTime;
        double distance = scenario.getNetwork().getLinks().get(linkId).getLength();

        int enterTimeBin = ExternalityUtils.getTimeBin(enterTime, 3600.0);
        int leaveTimeBin = ExternalityUtils.getTimeBin(leaveTime, 3600.0);

        if (leaveTimeBin == enterTimeBin) {
            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CountPerLinkField.COUNT.getText(), 1.0);
            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CountPerLinkField.DISTANCE.getText(), distance);
            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CountPerLinkField.TIME.getText(), dt);
        }
        else if (leaveTimeBin > enterTimeBin) {
            double T = 3600.0 * leaveTimeBin;
            double t1 = T - enterTime;
            double t2 = leaveTime - T;

            double c1 = t1 / dt;
            double c2 = t2 / dt;

            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    enterTimeBin, CountPerLinkField.COUNT.getText(), c1);
            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CountPerLinkField.COUNT.getText(), c2);

            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    enterTimeBin, CountPerLinkField.DISTANCE.getText(), distance*c1);
            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CountPerLinkField.DISTANCE.getText(), distance*c2);

            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    enterTimeBin, CountPerLinkField.TIME.getText(), t1);
            aggregateCountDataPerLinkPerTime.addValue(linkId,
                    leaveTimeBin, CountPerLinkField.TIME.getText(), t2);
        }
    }
}
