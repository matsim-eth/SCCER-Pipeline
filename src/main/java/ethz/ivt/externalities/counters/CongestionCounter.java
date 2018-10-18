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

            // if the last matsim agent to enter link is still on, then match them
            if (lastMatsimAgentOnLink.containsKey(linkId)) {

                Id<Person> matsimAgentId = lastMatsimAgentOnLink.get(linkId).getFirst();
                double matsimEnterTime = lastMatsimAgentOnLink.get(linkId).getSecond();

                this.gpsAgents.get(linkId).get(personId).setMatsimAgentId(Optional.of(matsimAgentId));
                this.gpsAgents.get(linkId).get(personId).setMatsimEnterTime(Optional.of(matsimEnterTime));

                this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                this.matsimAgent2GpsAgentMap.get(linkId).put(matsimAgentId, personId);

                this.lastMatsimAgentOnLink.remove(linkId);


            }

        }
        else {
            double matsimEnterTime = event.getTime();
            lastMatsimAgentOnLink.put(linkId, new Tuple<>(personId, matsimEnterTime));

            if (this.gpsAgents.containsKey(linkId)) {
                for (CongestionMatchingInfo congestionMatchingInfo : this.gpsAgents.get(linkId).values()) {

                    // check if gps agent already left link and if yes, ignore
                    if ( !congestionMatchingInfo.getGpsExitTime().isPresent() )  {
                        // check if match has already been made
                        if ( congestionMatchingInfo.getMatsimAgentId().isPresent() && congestionMatchingInfo.getMatsimEnterTime().isPresent() ) {
                            // check if new matsim enter time is temporally closer to gps enter time
                            double gpsEnterTime = congestionMatchingInfo.getGpsEnterTime();
                            double previousMatsimEnterTime = congestionMatchingInfo.getMatsimEnterTime().get();
                            if ( (Math.abs(matsimEnterTime - gpsEnterTime)) < (Math.abs(previousMatsimEnterTime - gpsEnterTime)) ) {
                                congestionMatchingInfo.setMatsimAgentId(Optional.of(personId));
                                congestionMatchingInfo.setMatsimEnterTime(Optional.of(matsimEnterTime));
                                this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                                this.matsimAgent2GpsAgentMap.get(linkId).put(personId, congestionMatchingInfo.getGpsAgentId());
                                lastMatsimAgentOnLink.remove(linkId);
                            }
                        }
                        else {
                            congestionMatchingInfo.setMatsimAgentId(Optional.of(personId));
                            congestionMatchingInfo.setMatsimEnterTime(Optional.of(matsimEnterTime));
                            this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                            this.matsimAgent2GpsAgentMap.get(linkId).put(personId, congestionMatchingInfo.getGpsAgentId());
                            lastMatsimAgentOnLink.remove(linkId);
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

        if (personId.toString().contains(PREFIX_GPS)) {
            if (this.gpsAgents.containsKey(linkId)) {
                if ( this.gpsAgents.get(linkId).containsKey(personId) ) {
                    this.gpsAgents.get(linkId).get(personId).setGpsExitTime(Optional.of(event.getTime()));
                    updateValues(gpsAgents.get(linkId).get(personId));
                }
            }
        } else {
            if (this.matsimAgent2GpsAgentMap.containsKey(linkId)) {
                if ( this.matsimAgent2GpsAgentMap.get(linkId).containsKey(personId) ) {
                    Id<Person> gpsAgentId = this.matsimAgent2GpsAgentMap.get(linkId).get(personId);
                    this.gpsAgents.get(linkId).get(gpsAgentId).setMatsimExitTime(Optional.of(event.getTime()));
                    if (!this.gpsAgents.get(linkId).get(gpsAgentId).getMatsimCausedDelay().isPresent()) {
                        this.gpsAgents.get(linkId).get(gpsAgentId).setMatsimCausedDelay(Optional.of(0.0));
                    }
                    updateValues(gpsAgents.get(linkId).get(gpsAgentId));
                }
            }
            this.lastMatsimAgentOnLink.remove(linkId);
        }
	}

	public void handleEvent(CongestionEvent event) {
        Id<Link> linkId = event.getLinkId();
        Id<Person> causingMatsimAgentId = event.getCausingAgentId();

        if (matsimAgent2GpsAgentMap.containsKey(linkId)) {
            if (matsimAgent2GpsAgentMap.get(linkId).containsKey(causingMatsimAgentId)) {
                double delay = event.getDelay();
                Id<Person> causingGpsAgentId = matsimAgent2GpsAgentMap.get(linkId).get(causingMatsimAgentId);
                gpsAgents.get(linkId).get(causingGpsAgentId).setMatsimCausedDelay(Optional.of(delay));
                updateValues(gpsAgents.get(linkId).get(causingGpsAgentId));
            }
        }
    }

    private void updateValues(CongestionMatchingInfo congestionMatchingInfo) {
	    if (congestionMatchingInfo.hasAllValuesSet()) {
            Id<Person> gpsAgentId = congestionMatchingInfo.getGpsAgentId();
            double causedDelay = congestionMatchingInfo.computeCausedDelay();
            double experiencedDelay = congestionMatchingInfo.computeExperiencedDelay();
            this.externalityCounterDelegate.incrementTempValueBy(gpsAgentId, CongestionField.DELAY_CAUSED.getText(), causedDelay);
            this.externalityCounterDelegate.incrementTempValueBy(gpsAgentId, CongestionField.DELAY_EXPERIENCED.getText(), experiencedDelay);
        }
    }

}
