package ethz.ivt.externalities.counters.congestion;

import ethz.ivt.externalities.gpsScenarioMerging.DisaggregateCongestionCounter;
import ethz.ivt.externalities.counters.ExternalityCounter;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class CorrectMatchingTest {

    @Test
    public void case1() {
        // case 1
        // imagine a gps agent enters link at 5 am
        // imagine first matsim agent enters link at 10 am and has a large queue behind it,
        // therefore likely causing delays
        // should the gps agent be charged with causing the delays?

        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with one link
        double length = 100.0; // m
        double freespeed = 14.0; // m/s
        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
        network.addNode(node0);
        network.addNode(node1);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
        network.addLink(link01);

        // create some people
        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
        scenario.getPopulation().addPerson(matsim1);
        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
        scenario.getPopulation().addPerson(gps1);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario);
        DisaggregateCongestionCounter congestionCounter = new DisaggregateCongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create some events
        List<Event> eventList = new LinkedList<>();
        // gps1 enters
        eventList.add(new PersonDepartureEvent(5 * 3600.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(5 * 3600.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(5 * 3600.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(5 * 3600.0 + 10.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        // matsim1 enters
        eventList.add(new PersonDepartureEvent(10 * 3600.0, matsim1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(10 * 3600.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(10 * 3600.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(10 * 3600.0 + 10.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));

        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertFalse(congestionCounter.getGpsAgents().get(link01.getId()).get(gps1.getId()).getMatsimAgentId().isPresent());
    }

    @Test
    public void case2() {
        // TODO : case 2
        // imagine a first matsim agent enters link at 5:00 with no queue behind it, therefore no delays caused
        // imagine a gps agent enters link at 9:00
        // imagine a second matsim agent enters link also at 9:00 (just behind gps agent) and
        // has a large queue behind it, therefore likely causing delays
        // the gps agent should probably be matched to the second agent, i.e. the closest temporally

        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with one link
        double length = 100.0; // m
        double freespeed = 14.0; // m/s
        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
        network.addNode(node0);
        network.addNode(node1);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
        network.addLink(link01);

        // create some people
        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
        scenario.getPopulation().addPerson(matsim1);
        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
        scenario.getPopulation().addPerson(gps1);
        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
        scenario.getPopulation().addPerson(matsim2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario);
        DisaggregateCongestionCounter congestionCounter = new DisaggregateCongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create some events
        List<Event> eventList = new LinkedList<>();
        // matsim1 enters
        eventList.add(new PersonDepartureEvent(5 * 3600.0, matsim1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(5 * 3600.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(5 * 3600.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(5 * 3600.0 + 10.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        // gps1 enters
        eventList.add(new PersonDepartureEvent(9 * 3600.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(9 * 3600.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(9 * 3600.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        // matsim2 enters
        eventList.add(new PersonDepartureEvent(9 * 3600.0, matsim2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(9 * 3600.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(9 * 3600.0, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
        // gps1 leaves and matsim2 leaves
        eventList.add(new LinkLeaveEvent(9 * 3600.0 + 10.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(9 * 3600.0 + 10.0, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));

        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertTrue(congestionCounter.getGpsAgents().get(link01.getId()).get(gps1.getId()).getMatsimAgentId().isPresent());
        assertEquals(matsim2.getId(), congestionCounter.getGpsAgents().get(link01.getId()).get(gps1.getId()).getMatsimAgentId().get());
    }

    @Test
    public void case3() {
        // TODO : case 3
        // imagine a first matsim agent enters link at 8:00 with a large queue behind it, therefore delays caused
        // the last agent in this queue leaves around 8:20
        // then a gps agent enters link at 13:00
        // then a second matsim agent enters link at 16:00 also with a large queue behind it, therefore causing delays
        // the gps agent should probably not be matched to any of the agents, i.e. some notion of temporal overall needed

        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with one link
        double length = 100.0; // m
        double freespeed = 14.0; // m/s
        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
        network.addNode(node0);
        network.addNode(node1);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
        network.addLink(link01);

        // create some people
        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
        scenario.getPopulation().addPerson(matsim1);
        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
        scenario.getPopulation().addPerson(gps1);
        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
        scenario.getPopulation().addPerson(matsim2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario);
        DisaggregateCongestionCounter congestionCounter = new DisaggregateCongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create some events
        List<Event> eventList = new LinkedList<>();
        // matsim1 enters
        eventList.add(new PersonDepartureEvent(8 * 3600.0, matsim1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(8 * 3600.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(8 * 3600.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(8 * 3600.0 + 10.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        // gps1 enters
        eventList.add(new PersonDepartureEvent(13 * 3600.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(13 * 3600.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(13 * 3600.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(13 * 3600.0 + 10.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        // matsim2 enters
        eventList.add(new PersonDepartureEvent(16 * 3600.0, matsim2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(16 * 3600.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(16 * 3600.0, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(16 * 3600.0 + 10.0, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));

        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertFalse(congestionCounter.getGpsAgents().get(link01.getId()).get(gps1.getId()).getMatsimAgentId().isPresent());
    }

    @Test
    public void case4() {
        // TODO : case 4
        // in the case when all agents are on the link within the same time period,
        // the gps agent should be matched to the closest matsim agent temporally

        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with one link
        double length = 100.0; // m
        double freespeed = 14.0; // m/s
        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
        network.addNode(node0);
        network.addNode(node1);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
        network.addLink(link01);

        // create some people
        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
        scenario.getPopulation().addPerson(matsim1);
        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
        scenario.getPopulation().addPerson(gps1);
        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
        scenario.getPopulation().addPerson(matsim2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario);
        DisaggregateCongestionCounter congestionCounter = new DisaggregateCongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create some events
        List<Event> eventList = new LinkedList<>();

        double matsim1EnterTime = 0.0;
        double matsim2EnterTime = 10.0;
        double travelTime = 20.0;

        for (int i = 0; i <=10; i++) {
            double gpsEnterTime = matsim1EnterTime + i;



            // matsim1 enters
            eventList.add(new PersonDepartureEvent(matsim1EnterTime, matsim1.getId(), link01.getId(), TransportMode.car));
            eventList.add(new VehicleEntersTrafficEvent(matsim1EnterTime, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
            eventList.add(new LinkEnterEvent(matsim1EnterTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
            // gps1 enters
            eventList.add(new PersonDepartureEvent(gpsEnterTime, gps1.getId(), link01.getId(), TransportMode.car));
            eventList.add(new VehicleEntersTrafficEvent(gpsEnterTime, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
            eventList.add(new LinkEnterEvent(gpsEnterTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
            // matsim2 enters
            eventList.add(new PersonDepartureEvent(matsim2EnterTime, matsim2.getId(), link01.getId(), TransportMode.car));
            eventList.add(new VehicleEntersTrafficEvent(matsim2EnterTime, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
            eventList.add(new LinkEnterEvent(matsim2EnterTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
            // agents leave
            eventList.add(new LinkLeaveEvent(matsim1EnterTime + travelTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
            eventList.add(new LinkLeaveEvent(gpsEnterTime + travelTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
            eventList.add(new LinkLeaveEvent(matsim2EnterTime + travelTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));

            // process events
            eventsManager.initProcessing();
            for (Event event : eventList) {
                eventsManager.processEvent(event);
            }
            eventsManager.finishProcessing();
            eventList.clear();

            // assertions
            Id<Person> expectedMatsimAgentId;

            double ratio = (gpsEnterTime - matsim1EnterTime) / (matsim2EnterTime - matsim1EnterTime);
            if (ratio <= 0.5) {
                expectedMatsimAgentId = matsim1.getId();
            } else {
                expectedMatsimAgentId = matsim2.getId();
            }

            assertTrue(congestionCounter.getGpsAgents().get(link01.getId()).get(gps1.getId()).getMatsimAgentId().isPresent());
            assertEquals("Matsim 1 enter time : " + matsim1EnterTime +
                    ", GPS enter time : " + gpsEnterTime +
                    ", Matsim 2 enter time : " + matsim2EnterTime, expectedMatsimAgentId, congestionCounter.getGpsAgents().get(link01.getId()).get(gps1.getId()).getMatsimAgentId().get());

            congestionCounter.reset(0);
        }


    }
}
