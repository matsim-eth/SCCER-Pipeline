package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.roadTypeMapping.OsmHbefaMapping;
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

import java.util.HashMap;
import java.util.Map;

public class AutobahnSplitCounter implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {
    private static final Logger log = Logger.getLogger(AutobahnSplitCounter.class);
    private Scenario scenario;

    public static final String AUTOBAHN_KEY = "autobahn_distance";
    public static final String NON_AUTOBAHN_KEY = "non_autobahn_distance";

    private Map<Id<Person>, Double> personLinkEntryTime = new HashMap<>();

    private ExternalityCounter externalityCounterDelegate;

    HashMap<String, String> mapping = new HashMap<>();


    public AutobahnSplitCounter(Scenario scenario,
                                ExternalityCounter externalityCounterDelegate) {

        this.scenario = scenario;
        this.externalityCounterDelegate = externalityCounterDelegate;

        initializeFields();

        mapping.put("motorway-Nat.", AUTOBAHN_KEY);
        mapping.put("motorway", AUTOBAHN_KEY);
        mapping.put("primary-Nat.", AUTOBAHN_KEY);
        mapping.put("primary", AUTOBAHN_KEY);
        mapping.put("trunk", NON_AUTOBAHN_KEY);
        mapping.put("secondary", NON_AUTOBAHN_KEY);
        mapping.put("tertiary", NON_AUTOBAHN_KEY);
        mapping.put("residential", NON_AUTOBAHN_KEY);
        mapping.put("service", NON_AUTOBAHN_KEY);
        mapping.put("living", NON_AUTOBAHN_KEY);
    }

    protected void initializeFields() {
        externalityCounterDelegate.addKey(AUTOBAHN_KEY);
        externalityCounterDelegate.addKey(NON_AUTOBAHN_KEY);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.personLinkEntryTime.put(event.getPersonId(), null);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());

        Double linkEntryTime =  this.personLinkEntryTime.get(personId);
        if (linkEntryTime != null) {
            Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
            String roadType = (String) link.getAttributes().getAttribute(OsmHbefaMapping.OSM_HIGHWAY_TAG);

            if (roadType != null) {
                externalityCounterDelegate.incrementTempValueBy(personId, mapping.get(roadType), link.getLength());
            }
            this.personLinkEntryTime.put(personId, null);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }

        //Now store the event for the person
        this.personLinkEntryTime.put(personId, event.getTime());

    }
}
