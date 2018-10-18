package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.data.CongestionField;
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
import org.matsim.core.utils.collections.Tuple;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CongestionCounter implements CongestionEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
	private Map<Id<Person>, Double> personLinkEntryTime = new HashMap<>();
    private Map<Id<Person>, Double> personLinkLeaveTime = new HashMap<>();
	private ExternalityCounter externalityCounterDelegate;

	private Map<Id<Link>, Tuple<Id<Person>, Double>> lastMatsimAgentEnteredLink = new HashMap<>();
	private Map<Id<Link>, Map<Id<Person>, CongestionMatchingInfo>> matsimAgent2GpsAgentMap = new HashMap<>();
    private Map<Id<Link>, Map<Id<Person>, CongestionMatchingInfo>> gpsAgent2ScenarioAgentMap = new HashMap<>();
	private Map<Id<Link>, List<Tuple<Id<Person>, Double>>> gpsAgentOnLinkToBeMatched = new HashMap<>();

	private Scenario scenario;

    private static String PREFIX_GPS = "gps";

	public CongestionCounter(Scenario scenario, ExternalityCounter externalityCounterDelegate) {

		this.scenario = scenario;
    	this.externalityCounterDelegate = externalityCounterDelegate;
    	initializeFields();
    }
    
    protected void initializeFields() {
		externalityCounterDelegate.addKey(CongestionField.DELAY_CAUSED.getText());
		externalityCounterDelegate.addKey(CongestionField.DELAY_EXPERIENCED.getText());
		externalityCounterDelegate.addKey("clock_time"); //time lost on gps trace
		externalityCounterDelegate.addKey("matsim_time"); //time lost on gps trace
		externalityCounterDelegate.addKey("matsim_delay"); //time lost on gps trace
    }

    @Override
    public void reset(int iteration) {
	    personLinkEntryTime.clear();
	    personLinkLeaveTime.clear();
	    lastMatsimAgentEnteredLink.clear();
	    matsimAgent2GpsAgentMap.clear();
	    gpsAgent2ScenarioAgentMap.clear();
	    gpsAgentOnLinkToBeMatched.clear();
    }

    public ExternalityCounter getExternalityCounterDelegate() {
        return externalityCounterDelegate;
    }

    public Map<Id<Link>, Tuple<Id<Person>, Double>> getLastMatsimAgentEnteredLink() {
        return lastMatsimAgentEnteredLink;
    }

    public Map<Id<Link>, Map<Id<Person>, CongestionMatchingInfo>> getMatsimAgent2GpsAgentMap() {
        return matsimAgent2GpsAgentMap;
    }

    public Map<Id<Link>, Map<Id<Person>, CongestionMatchingInfo>> getGpsAgent2ScenarioAgentMap() {
        return gpsAgent2ScenarioAgentMap;
    }

    public Map<Id<Link>, List<Tuple<Id<Person>, Double>>> getGpsAgentOnLinkToBeMatched() {
        return gpsAgentOnLinkToBeMatched;
    }

    @Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personLinkEntryTime.put(event.getPersonId(), null);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
	    Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());

	    if (matsimAgent2GpsAgentMap.containsKey(linkId)) {
	        if (matsimAgent2GpsAgentMap.get(linkId).containsKey(personId)) {
	            matsimAgent2GpsAgentMap.get(linkId).get(personId).setMatsimExitTime(event.getTime());

                if (matsimAgent2GpsAgentMap.get(linkId).get(personId).hasAllValuesSet()) {

                    Id<Person> gpsAgent = matsimAgent2GpsAgentMap.get(linkId).get(personId).getGpsAgent();
                    Id<Person> matsimAgent = matsimAgent2GpsAgentMap.get(linkId).get(personId).getMatsimAgent();
                    double causedDelay = matsimAgent2GpsAgentMap.get(linkId).get(personId).computeScaledGPSDelay();
                    double gpsExitTime = matsimAgent2GpsAgentMap.get(linkId).get(personId).getGpsExitTime();
                    double gpsEnterTime = matsimAgent2GpsAgentMap.get(linkId).get(personId).getGpsEnterTime();
                    double experiencedDelay = gpsExitTime - gpsEnterTime;

                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_CAUSED.getText(), causedDelay);
                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_EXPERIENCED.getText(), experiencedDelay);

                    gpsAgent2ScenarioAgentMap.get(linkId).remove(gpsAgent);
                    matsimAgent2GpsAgentMap.get(linkId).remove(matsimAgent);

                }

            }
        }
        else if (gpsAgent2ScenarioAgentMap.containsKey(linkId)) {
            if (gpsAgent2ScenarioAgentMap.get(linkId).containsKey(personId)) {
                gpsAgent2ScenarioAgentMap.get(linkId).get(personId).setGpsExitTime(event.getTime());

                if (gpsAgent2ScenarioAgentMap.get(linkId).get(personId).hasAllValuesSet()) {

                    Id<Person> gpsAgent = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getGpsAgent();
                    Id<Person> matsimAgent = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getMatsimAgent();
                    double causedDelay = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).computeScaledGPSDelay();
                    double gpsExitTime = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getGpsExitTime();
                    double gpsEnterTime = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getGpsEnterTime();
                    double experiencedDelay = gpsExitTime - gpsEnterTime;

                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_CAUSED.getText(), causedDelay);
                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_EXPERIENCED.getText(), experiencedDelay);

                    matsimAgent2GpsAgentMap.get(linkId).remove(matsimAgent);
                    gpsAgent2ScenarioAgentMap.get(linkId).remove(gpsAgent);
                }
            }
        }


	}

	@Override
	public void handleEvent(LinkEnterEvent event) {

		Id<Link> linkId = event.getLinkId();
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }

        if (personId.toString().contains(PREFIX_GPS)) {
            if (lastMatsimAgentEnteredLink.containsKey(linkId)) {

                Link link = scenario.getNetwork().getLinks().get(linkId);
                Id<Person> gpsAgent = personId;
                double gpsEnterTime = event.getTime();
                Id<Person> matsimAgent = lastMatsimAgentEnteredLink.get(linkId).getFirst();
                double matsimEnterTime = lastMatsimAgentEnteredLink.get(linkId).getSecond();

                if (hasMatsimAgentOnLinkBeenMatched(matsimAgent, linkId)) {
                    return;
                }

                CongestionMatchingInfo congestionMatchingInfo = new CongestionMatchingInfo(link, gpsAgent, matsimAgent, event.getTime(), matsimEnterTime);

                matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                matsimAgent2GpsAgentMap.get(linkId).putIfAbsent(matsimAgent, congestionMatchingInfo);

                gpsAgent2ScenarioAgentMap.putIfAbsent(linkId, new HashMap<>());
                gpsAgent2ScenarioAgentMap.get(linkId).putIfAbsent(gpsAgent, congestionMatchingInfo);

                lastMatsimAgentEnteredLink.remove(linkId);

            } else {
                gpsAgentOnLinkToBeMatched.putIfAbsent(linkId, new LinkedList<>());
                gpsAgentOnLinkToBeMatched.get(linkId).add(new Tuple<>(personId, event.getTime()));
            }
        }
        else {
            lastMatsimAgentEnteredLink.put(linkId, new Tuple<>(personId, event.getTime()));
            if (gpsAgentOnLinkToBeMatched.containsKey(linkId)) {
                if (gpsAgentOnLinkToBeMatched.get(linkId).size() > 0) {

                    Link link = scenario.getNetwork().getLinks().get(linkId);
                    Id<Person> gpsAgent = gpsAgentOnLinkToBeMatched.get(linkId).get(0).getFirst();
                    double gpsEnterTime = gpsAgentOnLinkToBeMatched.get(linkId).get(0).getSecond();
                    Id<Person> matsimAgent = personId;
                    double matsimEnterTime = event.getTime();

                    if (hasMatsimAgentOnLinkBeenMatched(matsimAgent, linkId)) {
                        return;
                    }

                    CongestionMatchingInfo congestionMatchingInfo = new CongestionMatchingInfo(link, gpsAgent, matsimAgent, gpsEnterTime, matsimEnterTime);

                    matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                    matsimAgent2GpsAgentMap.get(linkId).putIfAbsent(matsimAgent, congestionMatchingInfo);

                    gpsAgent2ScenarioAgentMap.putIfAbsent(linkId, new HashMap<>());
                    gpsAgent2ScenarioAgentMap.get(linkId).putIfAbsent(gpsAgent, congestionMatchingInfo);

                    gpsAgentOnLinkToBeMatched.get(linkId).remove(0);
                }
            }
        }

		//Now store the event time for the person
		this.personLinkEntryTime.put(personId, event.getTime());

	}

	public void handleEvent(CongestionEvent event) {

        Id<Link> linkId = event.getLinkId();
        Id<Person> causingAgentId = event.getCausingAgentId();

        if (matsimAgent2GpsAgentMap.containsKey(linkId)) {
            if (matsimAgent2GpsAgentMap.get(linkId).containsKey(causingAgentId)) {
                matsimAgent2GpsAgentMap.get(linkId).get(causingAgentId).setMatsimCausedDelay(event.getDelay());
            }
        }
    }

    private boolean hasGpsAgentOnLinkBeenMatched(Id<Person> gpsAgentId, Id<Link> linkId) {
	    if (this.gpsAgent2ScenarioAgentMap.containsKey(linkId)) {
	        return this.gpsAgent2ScenarioAgentMap.get(linkId).containsKey(gpsAgentId);
        }
	    return false;
    }

    private boolean hasMatsimAgentOnLinkBeenMatched(Id<Person> matsimAgentId, Id<Link> linkId) {
        if (this.matsimAgent2GpsAgentMap.containsKey(linkId)) {
            return this.matsimAgent2GpsAgentMap.get(linkId).containsKey(matsimAgentId);
        }
        return false;
    }






}
