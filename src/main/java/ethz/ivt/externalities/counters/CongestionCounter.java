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
		externalityCounterDelegate.addKey("clock_time"); //time lost on gps trace
		externalityCounterDelegate.addKey("matsim_time"); //time lost on gps trace
		externalityCounterDelegate.addKey("matsim_delay"); //time lost on gps trace
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


        if (personId.toString().contains(PREFIX_GPS)) {
            if (this.gpsAgents.containsKey(linkId)) {
                if ( this.gpsAgents.get(linkId).containsKey(personId) ) {
                    this.gpsAgents.get(linkId).get(personId).setGpsExitTime(Optional.of(event.getTime()));
                }
            }
        } else {
            Id<Person> matsimAgentId = personId;
            if (this.matsimAgent2GpsAgentMap.containsKey(linkId)) {
                if ( this.matsimAgent2GpsAgentMap.get(linkId).containsKey(matsimAgentId) ) {
                    Id<Person> gpsAgentId = this.matsimAgent2GpsAgentMap.get(linkId).get(matsimAgentId);
                    this.gpsAgents.get(linkId).get(gpsAgentId).setMatsimExitTime(Optional.of(event.getTime()));
                }
            }
            this.lastMatsimAgentOnLink.remove(linkId);
        }

//	    if (matsimAgent2GpsAgentMap.containsKey(linkId)) {
//	        if (matsimAgent2GpsAgentMap.get(linkId).containsKey(personId)) {
//	            matsimAgent2GpsAgentMap.get(linkId).get(personId).setMatsimExitTime(event.getTime());
//
//                if (matsimAgent2GpsAgentMap.get(linkId).get(personId).hasAllValuesSet()) {
//
//                    Id<Person> gpsAgent = matsimAgent2GpsAgentMap.get(linkId).get(personId).getGpsAgentId();
//                    Id<Person> matsimAgent = matsimAgent2GpsAgentMap.get(linkId).get(personId).getMatsimAgentId();
//                    double causedDelay = matsimAgent2GpsAgentMap.get(linkId).get(personId).computeScaledGPSDelay();
//                    double gpsExitTime = matsimAgent2GpsAgentMap.get(linkId).get(personId).getGpsExitTime();
//                    double gpsEnterTime = matsimAgent2GpsAgentMap.get(linkId).get(personId).getGpsEnterTime();
//                    double experiencedDelay = gpsExitTime - gpsEnterTime;
//
//                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_CAUSED.getText(), causedDelay);
//                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_EXPERIENCED.getText(), experiencedDelay);
//
//                    gpsAgent2ScenarioAgentMap.get(linkId).remove(gpsAgent);
//                    matsimAgent2GpsAgentMap.get(linkId).remove(matsimAgent);
//
//                }
//
//            }
//        }
//        else if (gpsAgent2ScenarioAgentMap.containsKey(linkId)) {
//            if (gpsAgent2ScenarioAgentMap.get(linkId).containsKey(personId)) {
//                gpsAgent2ScenarioAgentMap.get(linkId).get(personId).setGpsExitTime(event.getTime());
//
//                if (gpsAgent2ScenarioAgentMap.get(linkId).get(personId).hasAllValuesSet()) {
//
//                    Id<Person> gpsAgent = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getGpsAgentId();
//                    Id<Person> matsimAgent = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getMatsimAgentId();
//                    double causedDelay = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).computeScaledGPSDelay();
//                    double gpsExitTime = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getGpsExitTime();
//                    double gpsEnterTime = gpsAgent2ScenarioAgentMap.get(linkId).get(personId).getGpsEnterTime();
//                    double experiencedDelay = gpsExitTime - gpsEnterTime;
//
//                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_CAUSED.getText(), causedDelay);
//                    externalityCounterDelegate.incrementTempValueBy(gpsAgent, CongestionField.DELAY_EXPERIENCED.getText(), experiencedDelay);
//
//                    matsimAgent2GpsAgentMap.get(linkId).remove(matsimAgent);
//                    gpsAgent2ScenarioAgentMap.get(linkId).remove(gpsAgent);
//                }
//            }
//        }

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
            Id<Person> matsimAgentId = personId;
            double matsimEnterTime = event.getTime();

            lastMatsimAgentOnLink.put(linkId, new Tuple<>(matsimAgentId, matsimEnterTime));

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
                                congestionMatchingInfo.setMatsimAgentId(Optional.of(matsimAgentId));
                                congestionMatchingInfo.setMatsimEnterTime(Optional.of(matsimEnterTime));
                                this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                                this.matsimAgent2GpsAgentMap.get(linkId).put(matsimAgentId, congestionMatchingInfo.getGpsAgentId());
                                lastMatsimAgentOnLink.remove(linkId);
                            }
                        }
                        else {
                            congestionMatchingInfo.setMatsimAgentId(Optional.of(matsimAgentId));
                            congestionMatchingInfo.setMatsimEnterTime(Optional.of(matsimEnterTime));
                            this.matsimAgent2GpsAgentMap.putIfAbsent(linkId, new HashMap<>());
                            this.matsimAgent2GpsAgentMap.get(linkId).put(matsimAgentId, congestionMatchingInfo.getGpsAgentId());
                            lastMatsimAgentOnLink.remove(linkId);
                        }
                    }
                }
            }
        }
	}

	public void handleEvent(CongestionEvent event) {
//
//        Id<Link> linkId = event.getLinkId();
//        Id<Person> causingAgentId = event.getCausingAgentId();
//
//        if (matsimAgent2GpsAgentMap.containsKey(linkId)) {
//            if (matsimAgent2GpsAgentMap.get(linkId).containsKey(causingAgentId)) {
//                double delay = event.getDelay();
//                matsimAgent2GpsAgentMap.get(linkId).get(causingAgentId).setMatsimCausedDelay(Optional.of(delay));
//            }
//        }
    }

}
