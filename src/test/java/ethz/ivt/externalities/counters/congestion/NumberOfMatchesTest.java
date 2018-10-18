package ethz.ivt.externalities.counters.congestion;

import ethz.ivt.externalities.counters.CongestionCounter;
import ethz.ivt.externalities.counters.ExternalityCounter;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NumberOfMatchesTest {

    @Test
    public void case1() {
        // one gps agent and no matsim agents all enter at same time => no match

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
        Person gps2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps2"));
        scenario.getPopulation().addPerson(gps2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create events
        List<Event> eventList = new LinkedList<>();
        // gps1 enters
        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));

        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertTrue(congestionCounter.getGpsAgent2ScenarioAgentMap().isEmpty());
        assertTrue(congestionCounter.getMatsimAgent2GpsAgentMap().isEmpty());
    }

    @Test
    public void case2() {
        // case 2
        // one gps agent and one matsim agent all enter at same time => 1 match

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
        Person gps2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps2"));
        scenario.getPopulation().addPerson(gps2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create events
        List<Event> eventList = new LinkedList<>();
        // gps1 enters
        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        // matsim1 enters
        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));

        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertTrue(congestionCounter.getGpsAgent2ScenarioAgentMap().containsKey(link01.getId()));
        assertTrue(congestionCounter.getMatsimAgent2GpsAgentMap().containsKey(link01.getId()));
        assertEquals(1, congestionCounter.getGpsAgent2ScenarioAgentMap().get(link01.getId()).size());
        assertEquals(1, congestionCounter.getMatsimAgent2GpsAgentMap().get(link01.getId()).size());
    }

    @Test
    public void case3() {
        // case 3
        // one gps agent and two matsim agents all enter at same time => 1 match

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
        Person gps2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps2"));
        scenario.getPopulation().addPerson(gps2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create events
        List<Event> eventList = new LinkedList<>();
        // gps1 enters
        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        // matsim1 enters
        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        // matsim2 enters
        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));


        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertTrue(congestionCounter.getGpsAgent2ScenarioAgentMap().containsKey(link01.getId()));
        assertTrue(congestionCounter.getMatsimAgent2GpsAgentMap().containsKey(link01.getId()));
        assertEquals(1, congestionCounter.getGpsAgent2ScenarioAgentMap().get(link01.getId()).size());
        assertEquals(1, congestionCounter.getMatsimAgent2GpsAgentMap().get(link01.getId()).size());
    }

    @Test
    public void case4() {
        // case 4
        // two gps agents and one matsim agents all enter at same time => 1 match

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
        Person gps2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps2"));
        scenario.getPopulation().addPerson(gps2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create events
        List<Event> eventList = new LinkedList<>();
        // gps1 enters
        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        // matsim1 enters
        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        // gps2 enters
        eventList.add(new PersonDepartureEvent(0.0, gps2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, gps2.getId(), link01.getId(), Id.createVehicleId(gps2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps2.getId().toString()), link01.getId()));


        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertTrue(congestionCounter.getGpsAgent2ScenarioAgentMap().containsKey(link01.getId()));
        assertTrue(congestionCounter.getMatsimAgent2GpsAgentMap().containsKey(link01.getId()));
        assertEquals(1, congestionCounter.getGpsAgent2ScenarioAgentMap().get(link01.getId()).size());
        assertEquals(1, congestionCounter.getMatsimAgent2GpsAgentMap().get(link01.getId()).size());
    }

    @Test
    public void case5() {
        // case 5
        // two gps agents and two matsim agents all enter at same time => 2 matches

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
        Person gps2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps2"));
        scenario.getPopulation().addPerson(gps2);

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        String date = "2018-07-27";
        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(externalityCounter);
        eventsManager.addHandler(congestionCounter);

        // create events
        List<Event> eventList = new LinkedList<>();
        // gps1 enters
        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
        // matsim1 enters
        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
        // matsim2 enters
        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
        // gps2 enters
        eventList.add(new PersonDepartureEvent(0.0, gps2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, gps2.getId(), link01.getId(), Id.createVehicleId(gps2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps2.getId().toString()), link01.getId()));


        // process events
        eventsManager.initProcessing();
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // assertions
        assertTrue(congestionCounter.getGpsAgent2ScenarioAgentMap().containsKey(link01.getId()));
        assertTrue(congestionCounter.getMatsimAgent2GpsAgentMap().containsKey(link01.getId()));
        assertEquals(2, congestionCounter.getGpsAgent2ScenarioAgentMap().get(link01.getId()).size());
        assertEquals(2, congestionCounter.getMatsimAgent2GpsAgentMap().get(link01.getId()).size());
    }
}
