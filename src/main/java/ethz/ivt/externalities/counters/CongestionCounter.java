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
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CongestionCounter implements CongestionEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {
	private static final Logger log = Logger.getLogger(CongestionCounter.class);
	private Map<Id<Person>, Double> personLinkEntryTime = new HashMap<>();
    private Map<Id<Person>, Double> personLinkLeaveTime = new HashMap<>();
	private ExternalityCounter externalityCounterDelegate;

	private Map<Id<Link>, Map<Id<Person>, CongestionMatchingInfo>> gpsAgents = new HashMap<>();
	private Map<Id<Link>, Tuple<Id<Person>, Double>> lastMatsimAgentOnLink = new HashMap<>();
	private Map<Id<Link>, Map<Id<Person>, Id<Person>>> matsimAgent2GpsAgentMap = new HashMap<>();


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
    }

    @Override
    public void reset(int iteration) {
	    personLinkEntryTime.clear();
	    personLinkLeaveTime.clear();
	    lastMatsimAgentOnLink.clear();
	    matsimAgent2GpsAgentMap.clear();
	    gpsAgent2ScenarioAgentMap.clear();
	    gpsAgentOnLinkToBeMatched.clear();
	    gpsAgents.clear();
    }

    public ExternalityCounter getExternalityCounterDelegate() {
        return externalityCounterDelegate;
    }

    public Map<Id<Link>, Map<Id<Person>, CongestionMatchingInfo>> getGpsAgents() {
        return gpsAgents;
    }

    public Map<Id<Link>, Tuple<Id<Person>, Double>> getLastMatsimAgentOnLink() {
        return lastMatsimAgentOnLink;
    }

    public Map<Id<Link>, Map<Id<Person>, Id<Person>>> getMatsimAgent2GpsAgentMap() {
        return matsimAgent2GpsAgentMap;
    }

    @Override
	public void handleEvent(PersonDepartureEvent event) {
	    this.personLinkEntryTime.put(event.getPersonId(), null);
	}

    @Override
    public void handleEvent(LinkEnterEvent event) {

        Id<Link> linkId = event.getLinkId();
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(event.getVehicleId().toString());
        }

        if (personId.toString().contains(PREFIX_GPS)) {
            Link link = scenario.getNetwork().getLinks().get(linkId);
            CongestionMatchingInfo congestionMatchingInfo = new CongestionMatchingInfo(link, personId, event.getTime());
            this.gpsAgents.putIfAbsent(linkId, new HashMap<>());
            this.gpsAgents.get(linkId).putIfAbsent(personId, congestionMatchingInfo);

            // check if last matsim agent to enter link is still unmatched
            if (lastMatsimAgentOnLink.containsKey(linkId)) {

                Id<Person> matsimAgentId = lastMatsimAgentOnLink.get(linkId).getFirst();
                double matsimEnterTime = lastMatsimAgentOnLink.get(linkId).getSecond();
                this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());

                // if already matched, skip
                if (matsimAgent2GpsAgentMap.get(linkId).containsKey(matsimAgentId)) {
                    return;
                }
                // otherwise, match
                this.gpsAgents.get(linkId).get(personId).setMatsimAgentId(Optional.of(matsimAgentId));
                this.gpsAgents.get(linkId).get(personId).setMatsimEnterTime(Optional.of(matsimEnterTime));
                this.matsimAgent2GpsAgentMap.get(linkId).put(matsimAgentId, personId);
            }

        }
        else {
            // set as last matsim agent on this link with enter time
            double matsimEnterTime = event.getTime();
            lastMatsimAgentOnLink.put(linkId, new Tuple<>(personId, matsimEnterTime));

            // if there is gps agents on link which can potentially be matched
            if (this.gpsAgents.containsKey(linkId)) {
                // loop through them
                for (CongestionMatchingInfo congestionMatchingInfo : this.gpsAgents.get(linkId).values()) {

                    // check if gps agent already left link and if yes, ignore
                    if ( !congestionMatchingInfo.getGpsExitTime().isPresent() )  {
                        // if not, check if match has already been made
                        if ( congestionMatchingInfo.getMatsimAgentId().isPresent() && congestionMatchingInfo.getMatsimEnterTime().isPresent() ) {
                            // if yes, check if new matsim enter time is temporally closer to gps enter time
                            double gpsEnterTime = congestionMatchingInfo.getGpsEnterTime();
                            double previousMatsimEnterTime = congestionMatchingInfo.getMatsimEnterTime().get();
                            // if yes, set as new match
                            if ( (Math.abs(matsimEnterTime - gpsEnterTime)) < (Math.abs(previousMatsimEnterTime - gpsEnterTime)) ) {
                                congestionMatchingInfo.setMatsimAgentId(Optional.of(personId));
                                congestionMatchingInfo.setMatsimEnterTime(Optional.of(matsimEnterTime));
                                this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                                this.matsimAgent2GpsAgentMap.get(linkId).put(personId, congestionMatchingInfo.getGpsAgentId());
                            }
                        }
                        // if no match has been made
                        else {
                            this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                            // if current agent has not been matched, match it
                            if (!this.matsimAgent2GpsAgentMap.get(linkId).containsKey(personId)) {
                                congestionMatchingInfo.setMatsimAgentId(Optional.of(personId));
                                congestionMatchingInfo.setMatsimEnterTime(Optional.of(matsimEnterTime));
                                this.matsimAgent2GpsAgentMap.get(linkId).put(personId, congestionMatchingInfo.getGpsAgentId());
                            }
                        }
                    }
                }
            }
        }
    }

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
	    Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());

	    // check if gps agent
        if (personId.toString().contains(PREFIX_GPS)) {
            if (this.gpsAgents.containsKey(linkId)) {
                if ( this.gpsAgents.get(linkId).containsKey(personId) ) {
                    // update exit time
                    CongestionMatchingInfo congestionMatchingInfo = this.gpsAgents.get(linkId).get(personId);
                    congestionMatchingInfo.setGpsExitTime(Optional.of(event.getTime()));
                    // try to update temp values
                    if (congestionMatchingInfo.isTempValuesReadyToBeUpdated() && !congestionMatchingInfo.isTempValuesUpdated()) {
                        updateTempValues(congestionMatchingInfo);
                    }
                }
            }
        }
        // if matsim agent
        else {
            if (this.matsimAgent2GpsAgentMap.containsKey(linkId)) {
                if ( this.matsimAgent2GpsAgentMap.get(linkId).containsKey(personId) ) {
                    // update exit time
                    Id<Person> gpsAgentId = this.matsimAgent2GpsAgentMap.get(linkId).get(personId);
                    CongestionMatchingInfo congestionMatchingInfo = this.gpsAgents.get(linkId).get(gpsAgentId);
                    congestionMatchingInfo.setMatsimExitTime(Optional.of(event.getTime()));
                    // check if this was the last matsim agent to enter link
                    if (this.lastMatsimAgentOnLink.containsKey(linkId)) {
                        // if yes, then set caused delay to zero, since no one behind in queue
                        if (this.lastMatsimAgentOnLink.get(linkId).getFirst().equals(personId)) {
                            congestionMatchingInfo.setMatsimCausedDelay(Optional.of(0.0));
                        }
                    }
                    // try to update temp values
                    if (congestionMatchingInfo.isTempValuesReadyToBeUpdated() && !congestionMatchingInfo.isTempValuesUpdated()) {
                        updateTempValues(congestionMatchingInfo);
                    }
                }
            }
            if (this.lastMatsimAgentOnLink.get(linkId).getFirst().equals(personId)) {
                this.lastMatsimAgentOnLink.remove(linkId);
            }
        }
	}

	public void handleEvent(CongestionEvent event) {
        Id<Link> linkId = event.getLinkId();
        Id<Person> causingMatsimAgentId = event.getCausingAgentId();

        if (matsimAgent2GpsAgentMap.containsKey(linkId)) {
            if (matsimAgent2GpsAgentMap.get(linkId).containsKey(causingMatsimAgentId)) {
                double delay = event.getDelay();
                Id<Person> causingGpsAgentId = matsimAgent2GpsAgentMap.get(linkId).get(causingMatsimAgentId);
                CongestionMatchingInfo congestionMatchingInfo = gpsAgents.get(linkId).get(causingGpsAgentId);
                congestionMatchingInfo.setMatsimCausedDelay(Optional.of(delay));
                if (congestionMatchingInfo.isTempValuesReadyToBeUpdated() && !congestionMatchingInfo.isTempValuesUpdated()) {
                    updateTempValues(congestionMatchingInfo);
                }
            }
        }
    }

    private void updateTempValues(CongestionMatchingInfo congestionMatchingInfo) {
        if (congestionMatchingInfo.getGpsCausedDelay().isPresent() && congestionMatchingInfo.getGpsExperiencedDelay().isPresent()) {
            Id<Person> gpsAgentId = congestionMatchingInfo.getGpsAgentId();
            double causedDelay = congestionMatchingInfo.getGpsCausedDelay().getAsDouble();
            double experiencedDelay = congestionMatchingInfo.getGpsExperiencedDelay().getAsDouble();
            this.externalityCounterDelegate.incrementTempValueBy(gpsAgentId, CongestionField.DELAY_CAUSED.getText(), causedDelay);
            this.externalityCounterDelegate.incrementTempValueBy(gpsAgentId, CongestionField.DELAY_EXPERIENCED.getText(), experiencedDelay);
            congestionMatchingInfo.setTempValuesUpdated(true);
        }
    }

}
