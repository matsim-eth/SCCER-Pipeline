//package ethz.ivt.externalities.counters.congestion;
//
//import ethz.ivt.externalities.counters.CongestionCounter;
//import ethz.ivt.externalities.counters.ExternalityCounter;
//import ethz.ivt.externalities.data.CongestionField;
//import ethz.ivt.vsp.handlers.CongestionHandler;
//import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
//import org.junit.Test;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.events.*;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.network.Node;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.core.api.experimental.events.EventsManager;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.events.EventsManagerImpl;
//import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
//import org.matsim.core.network.NetworkUtils;
//import org.matsim.core.population.PopulationUtils;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.vehicles.VehicleType;
//import org.matsim.vehicles.VehicleUtils;
//
//import java.util.LinkedList;
//import java.util.List;
//
//import static org.junit.Assert.*;
//
//public class CongestionCounterTest {
//
//
//
//
//
//    @Test
//    public void congestionCounterRecordsLinkExitTimesCorrectly() {
//        // set up config
//        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//        // create simple network with one link
//        double length = 100.0; // m
//        double freespeed = 14.0; // m/s
//        Network network = scenario.getNetwork();
//        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
//        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
//        network.addNode(node0);
//        network.addNode(node1);
//        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
//        network.addLink(link01);
//
//        // create some people
//        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
//        scenario.getPopulation().addPerson(matsim1);
//        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
//        scenario.getPopulation().addPerson(gps1);
//
//        // set up event manager and handlers
//        EventsManager eventsManager = new EventsManagerImpl();
//        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
//        eventsManager.addHandler(v2deh);
//
//        String date = "2018-07-27";
//        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
//        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);
//
//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
//        eventsManager.addHandler(congestionHandler);
//        eventsManager.addHandler(externalityCounter);
//        eventsManager.addHandler(congestionCounter);
//
//        // create some events
//        List<Event> eventList = new LinkedList<>();
//        // matsim1 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        // gps1 enters
//        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(0.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//
//        // they all leave, some with delays
//        double delay = 1.0;
//        eventList.add(new LinkLeaveEvent(20.0, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(30.0, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//
//        // process events
//        eventsManager.initProcessing();
//        for (Event event : eventList) {
//            eventsManager.processEvent(event);
//        }
//        eventsManager.finishProcessing();
//
//        // assertions
//        assertEquals(20.0, congestionCounter.getGpsAgent2ScenarioAgentMap().get(link01.getId()).get(gps1.getId()).getMatsimExitTime(), 0.0);
//        assertEquals(30.0, congestionCounter.getGpsAgent2ScenarioAgentMap().get(link01.getId()).get(gps1.getId()).getGpsExitTime(), 0.0);
//        assertEquals(20.0, congestionCounter.getMatsimAgent2GpsAgentMap().get(link01.getId()).get(matsim1.getId()).getMatsimExitTime(), 0.0);
//        assertEquals(30.0, congestionCounter.getMatsimAgent2GpsAgentMap().get(link01.getId()).get(matsim1.getId()).getGpsExitTime(), 0.0);
//
//        congestionCounter.reset(0);
//    }
//
//    @Test
//    public void gpsAgentEntersAndLeavesBetweenTwoMatsimAgents() {
//        // set up config
//        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//        // create simple network with one link
//        double length = 100.0; // m
//        double freespeed = 14.0; // m/s
//        Network network = scenario.getNetwork();
//        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
//        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
//        network.addNode(node0);
//        network.addNode(node1);
//        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
//        network.addLink(link01);
//
//        // create some people
//        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
//        scenario.getPopulation().addPerson(matsim1);
//        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
//        scenario.getPopulation().addPerson(gps1);
//        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
//        scenario.getPopulation().addPerson(matsim2);
//
//        // create some events
//        double startTime = 0.0;
//        double travelTime = Math.floor(length / freespeed) + 1.0;
//
//        List<Event> eventList = new LinkedList<>();
//        // matsim1 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        // gps1 enters
//        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        // matsim2 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // they all leave, some with delays
//        double delay = 1.0;
//        eventList.add(new LinkLeaveEvent(startTime + travelTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + travelTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + travelTime + delay, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // set up event manager and handlers
//        EventsManager eventsManager = new EventsManagerImpl();
//        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
//        eventsManager.addHandler(v2deh);
//
//        String date = "2018-07-27";
//        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
//        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);
//
//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
//        eventsManager.addHandler(congestionHandler);
//        eventsManager.addHandler(externalityCounter);
//        eventsManager.addHandler(congestionCounter);
//
//        //householdid, #autos, auto1, auto2, auto3
//        //get household id of person. Assign next vehicle from household.
//
//        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("Benzin", VehicleType.class));
//        car.setMaximumVelocity(100.0 / 3.6);
//        car.setPcuEquivalents(1.0);
//        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC-P-Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car);
//
//        VehicleType car_diesel = VehicleUtils.getFactory().createVehicleType(Id.create("Diesel", VehicleType.class));
//        car_diesel.setMaximumVelocity(100.0 / 3.6);
//        car_diesel.setPcuEquivalents(1.0);
//        car_diesel.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;1,4-<2L;PC D Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car_diesel);
//
//        //hybrids are only coming in hbefa vresion 4.
//
//        // process events
//        eventsManager.initProcessing();
//        for (Event event : eventList) {
//            eventsManager.processEvent(event);
//        }
//        eventsManager.finishProcessing();
//
//        double expectedDelay = 1.0;
//        double actualDelay = externalityCounter.getTempValue(gps1.getId(), CongestionField.DELAY_CAUSED.getText());
//        assertEquals("Wrong delay caused!", expectedDelay, actualDelay, 0.0);
//
//    }
//
//    @Test
//    public void gpsAgentEntersAndLeavesBeforeTwoMatsimAgents() {
//        // set up config
//        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//        // create simple network with one link
//        double length = 100.0; // m
//        double freespeed = 14.0; // m/s
//        Network network = scenario.getNetwork();
//        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
//        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
//        network.addNode(node0);
//        network.addNode(node1);
//        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
//        network.addLink(link01);
//
//        // create some people
//        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
//        scenario.getPopulation().addPerson(gps1);
//        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
//        scenario.getPopulation().addPerson(matsim1);
//        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
//        scenario.getPopulation().addPerson(matsim2);
//
//        // create some events
//        double startTime = 0.0;
//        double travelTime = Math.floor(length / freespeed) + 1.0;
//
//        List<Event> eventList = new LinkedList<>();
//        // gps1 enters
//        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        // matsim1 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        // matsim2 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // they all leave, some with delays
//        double delay = 1.0;
//        eventList.add(new LinkLeaveEvent(startTime + travelTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + travelTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + travelTime + delay, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // set up event manager and handlers
//        EventsManager eventsManager = new EventsManagerImpl();
//        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
//        eventsManager.addHandler(v2deh);
//
//        String date = "2018-07-27";
//        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
//        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);
//
//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
//        eventsManager.addHandler(congestionHandler);
//        eventsManager.addHandler(externalityCounter);
//        eventsManager.addHandler(congestionCounter);
//
//        //householdid, #autos, auto1, auto2, auto3
//        //get household id of person. Assign next vehicle from household.
//
//        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("Benzin", VehicleType.class));
//        car.setMaximumVelocity(100.0 / 3.6);
//        car.setPcuEquivalents(1.0);
//        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC-P-Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car);
//
//        VehicleType car_diesel = VehicleUtils.getFactory().createVehicleType(Id.create("Diesel", VehicleType.class));
//        car_diesel.setMaximumVelocity(100.0 / 3.6);
//        car_diesel.setPcuEquivalents(1.0);
//        car_diesel.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;1,4-<2L;PC D Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car_diesel);
//
//        //hybrids are only coming in hbefa vresion 4.
//
//        // process events
//        eventsManager.initProcessing();
//        for (Event event : eventList) {
//            eventsManager.processEvent(event);
//        }
//        eventsManager.finishProcessing();
//
//        double expectedDelay = 1.0;
//        double actualDelay = externalityCounter.getTempValue(gps1.getId(), CongestionField.DELAY_CAUSED.getText());
//        assertEquals("Wrong delay caused!", expectedDelay, actualDelay, 0.0);
//
//    }
//
//    @Test
//    public void gpsAgentEntersBetweenTwoMatsimAgentsAndLeavesBeforeBothMatsimAgents() {
//        // set up config
//        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//        // create simple network with one link
//        double length = 100.0; // m
//        double freespeed = 14.0; // m/s
//        Network network = scenario.getNetwork();
//        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
//        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
//        network.addNode(node0);
//        network.addNode(node1);
//        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
//        network.addLink(link01);
//
//        // create some people
//        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
//        scenario.getPopulation().addPerson(matsim1);
//        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
//        scenario.getPopulation().addPerson(gps1);
//        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
//        scenario.getPopulation().addPerson(matsim2);
//
//        // create some events
//        double startTime = 0.0;
//        double freeflowTravelTime = Math.floor(length / freespeed) + 1.0;
//
//        List<Event> eventList = new LinkedList<>();
//        // matsim1 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        // gps1 enters
//        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        // matsim2 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // gps agent leaves first, then both matsim agents
//        double delay = 1.0;
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + delay, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + 2*delay, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + 2*delay, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // set up event manager and handlers
//        EventsManager eventsManager = new EventsManagerImpl();
//        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
//        eventsManager.addHandler(v2deh);
//
//        String date = "2018-07-27";
//        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
//        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);
//
//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
//        eventsManager.addHandler(congestionHandler);
//        eventsManager.addHandler(externalityCounter);
//        eventsManager.addHandler(congestionCounter);
//
//        //householdid, #autos, auto1, auto2, auto3
//        //get household id of person. Assign next vehicle from household.
//
//        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("Benzin", VehicleType.class));
//        car.setMaximumVelocity(100.0 / 3.6);
//        car.setPcuEquivalents(1.0);
//        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC-P-Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car);
//
//        VehicleType car_diesel = VehicleUtils.getFactory().createVehicleType(Id.create("Diesel", VehicleType.class));
//        car_diesel.setMaximumVelocity(100.0 / 3.6);
//        car_diesel.setPcuEquivalents(1.0);
//        car_diesel.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;1,4-<2L;PC D Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car_diesel);
//
//        //hybrids are only coming in hbefa vresion 4.
//
//        // process events
//        eventsManager.initProcessing();
//        for (Event event : eventList) {
//            eventsManager.processEvent(event);
//        }
//        eventsManager.finishProcessing();
//
//        double expectedDelay = 2.0 * (9.0) / (10.0);
//        double actualDelay = externalityCounter.getTempValue(gps1.getId(), CongestionField.DELAY_CAUSED.getText());
//        assertEquals("Wrong delay caused!", expectedDelay, actualDelay, 0.0);
//
//    }
//
//    @Test
//    public void gpsAgentEntersBeforeTwoMatsimAgentsAndLeavesBetweenBothMatsimAgents() {
//        // set up config
//        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//        // create simple network with one link
//        double length = 100.0; // m
//        double freespeed = 14.0; // m/s
//        Network network = scenario.getNetwork();
//        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
//        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
//        network.addNode(node0);
//        network.addNode(node1);
//        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
//        network.addLink(link01);
//
//        // create some people
//        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
//        scenario.getPopulation().addPerson(matsim1);
//        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
//        scenario.getPopulation().addPerson(gps1);
//        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
//        scenario.getPopulation().addPerson(matsim2);
//
//        // create some events
//        double startTime = 0.0;
//        double freeflowTravelTime = Math.floor(length / freespeed) + 1.0;
//
//        List<Event> eventList = new LinkedList<>();
//        // gps1 enters
//        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        // matsim1 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        // matsim2 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // gps agent leaves first, then both matsim agents
//        double delay = 1.0;
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + delay, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + 2*delay, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + 2*delay, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // set up event manager and handlers
//        EventsManager eventsManager = new EventsManagerImpl();
//        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
//        eventsManager.addHandler(v2deh);
//
//        String date = "2018-07-27";
//        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
//        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);
//
//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
//        eventsManager.addHandler(congestionHandler);
//        eventsManager.addHandler(externalityCounter);
//        eventsManager.addHandler(congestionCounter);
//
//        //householdid, #autos, auto1, auto2, auto3
//        //get household id of person. Assign next vehicle from household.
//
//        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("Benzin", VehicleType.class));
//        car.setMaximumVelocity(100.0 / 3.6);
//        car.setPcuEquivalents(1.0);
//        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC-P-Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car);
//
//        VehicleType car_diesel = VehicleUtils.getFactory().createVehicleType(Id.create("Diesel", VehicleType.class));
//        car_diesel.setMaximumVelocity(100.0 / 3.6);
//        car_diesel.setPcuEquivalents(1.0);
//        car_diesel.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;1,4-<2L;PC D Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car_diesel);
//
//        //hybrids are only coming in hbefa vresion 4.
//
//        // process events
//        eventsManager.initProcessing();
//        for (Event event : eventList) {
//            eventsManager.processEvent(event);
//        }
//        eventsManager.finishProcessing();
//
//        double expectedDelay = 2.0 * (10.0 / 9.0);
//        double actualDelay = externalityCounter.getTempValue(gps1.getId(), CongestionField.DELAY_CAUSED.getText());
//        assertEquals("Wrong delay caused!", expectedDelay, actualDelay, 0.0);
//
//    }
//
//    @Test
//    public void gpsAgentEntersAfterBothMatsimAgentsAndLeavesBetweenBothMatsimAgents() {
//        // set up config
//        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//        // create simple network with one link
//        double length = 100.0; // m
//        double freespeed = 14.0; // m/s
//        Network network = scenario.getNetwork();
//        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
//        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
//        network.addNode(node0);
//        network.addNode(node1);
//        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
//        network.addLink(link01);
//
//        // create some people
//        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
//        scenario.getPopulation().addPerson(matsim1);
//        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
//        scenario.getPopulation().addPerson(gps1);
//        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
//        scenario.getPopulation().addPerson(matsim2);
//
//        // create some events
//        double startTime = 0.0;
//        double freeflowTravelTime = Math.floor(length / freespeed) + 1.0;
//
//        List<Event> eventList = new LinkedList<>();
//        // matsim1 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        // matsim2 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//        // gps1 enters
//        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//
//        // gps agent leaves first, then both matsim agents
//        double delay = 1.0;
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + delay, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + 2*delay, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + 2*delay, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // set up event manager and handlers
//        EventsManager eventsManager = new EventsManagerImpl();
//        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
//        eventsManager.addHandler(v2deh);
//
//        String date = "2018-07-27";
//        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
//        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);
//
//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
//        eventsManager.addHandler(congestionHandler);
//        eventsManager.addHandler(externalityCounter);
//        eventsManager.addHandler(congestionCounter);
//
//        //householdid, #autos, auto1, auto2, auto3
//        //get household id of person. Assign next vehicle from household.
//
//        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("Benzin", VehicleType.class));
//        car.setMaximumVelocity(100.0 / 3.6);
//        car.setPcuEquivalents(1.0);
//        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC-P-Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car);
//
//        VehicleType car_diesel = VehicleUtils.getFactory().createVehicleType(Id.create("Diesel", VehicleType.class));
//        car_diesel.setMaximumVelocity(100.0 / 3.6);
//        car_diesel.setPcuEquivalents(1.0);
//        car_diesel.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;1,4-<2L;PC D Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car_diesel);
//
//        //hybrids are only coming in hbefa vresion 4.
//
//        // process events
//        eventsManager.initProcessing();
//        for (Event event : eventList) {
//            eventsManager.processEvent(event);
//        }
//        eventsManager.finishProcessing();
//
//        double expectedDelay = 0.0;
//        double actualDelay = externalityCounter.getTempValue(gps1.getId(), CongestionField.DELAY_CAUSED.getText());
//        assertEquals("Wrong delay caused!", expectedDelay, actualDelay, 0.0);
//    }
//
//    @Test
//    public void gpsAgentEntersFirstLinkBetweenBothMatsimAgentsAndSecondLinkAfterBothMatsimAgents() {
//        // set up config
//        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//        // create simple network with one link
//        double length = 100.0; // m
//        double freespeed = 14.0; // m/s
//        Network network = scenario.getNetwork();
//        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
//        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
//        Node node2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0.0, 2 * length));
//        network.addNode(node0);
//        network.addNode(node1);
//        network.addNode(node2);
//        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 1, 1);
//        Link link12 = NetworkUtils.createLink(Id.createLinkId("1-2"), node1, node2, network, length, freespeed, 1, 1);
//        network.addLink(link01);
//        network.addLink(link12);
//
//        // create some people
//        Person matsim1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat1"));
//        scenario.getPopulation().addPerson(matsim1);
//        Person gps1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("gps1"));
//        scenario.getPopulation().addPerson(gps1);
//        Person matsim2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("mat2"));
//        scenario.getPopulation().addPerson(matsim2);
//
//        // create some events
//        double startTime = 0.0;
//        double freeflowTravelTime = Math.floor(length / freespeed) + 1.0;
//
//        List<Event> eventList = new LinkedList<>();
//
//        // matsim1 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim1.getId(), link01.getId(), Id.createVehicleId(matsim1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        // gps1 enters
//        eventList.add(new PersonDepartureEvent(0.0, gps1.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, gps1.getId(), link01.getId(), Id.createVehicleId(gps1.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        // matsim2 enters
//        eventList.add(new PersonDepartureEvent(0.0, matsim2.getId(), link01.getId(), TransportMode.car));
//        eventList.add(new VehicleEntersTrafficEvent(0.0, matsim2.getId(), link01.getId(), Id.createVehicleId(matsim2.getId().toString()), TransportMode.car, 0.0));
//        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//
//        // both matsim agents leave link then enter next, followed by gps agent
//        double delay = 1.0;
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime, Id.createVehicleId(matsim1.getId().toString()), link01.getId()));
//        eventList.add(new LinkEnterEvent(startTime + freeflowTravelTime, Id.createVehicleId(matsim1.getId().toString()), link12.getId()));
//
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + delay, Id.createVehicleId(matsim2.getId().toString()), link01.getId()));
//        eventList.add(new LinkEnterEvent(startTime + freeflowTravelTime + delay, Id.createVehicleId(matsim2.getId().toString()), link12.getId()));
//
//        eventList.add(new LinkLeaveEvent(startTime + freeflowTravelTime + delay, Id.createVehicleId(gps1.getId().toString()), link01.getId()));
//        eventList.add(new LinkEnterEvent(startTime + freeflowTravelTime + delay, Id.createVehicleId(gps1.getId().toString()), link12.getId()));
//
//        // matsim1 enters, then matsim2 enters and then gps1 leaves
//        eventList.add(new LinkLeaveEvent(startTime + 2*(freeflowTravelTime), Id.createVehicleId(matsim1.getId().toString()), link12.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + 2*(freeflowTravelTime + delay), Id.createVehicleId(matsim2.getId().toString()), link12.getId()));
//        eventList.add(new LinkLeaveEvent(startTime + 2*(freeflowTravelTime + delay), Id.createVehicleId(gps1.getId().toString()), link12.getId()));
//
//        // set up event manager and handlers
//        EventsManager eventsManager = new EventsManagerImpl();
//        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
//        eventsManager.addHandler(v2deh);
//
//        String date = "2018-07-27";
//        ExternalityCounter externalityCounter = new ExternalityCounter(scenario, date);
//        CongestionCounter congestionCounter = new CongestionCounter(scenario, externalityCounter);
//
//        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
//        eventsManager.addHandler(congestionHandler);
//        eventsManager.addHandler(externalityCounter);
//        eventsManager.addHandler(congestionCounter);
//
//        //householdid, #autos, auto1, auto2, auto3
//        //get household id of person. Assign next vehicle from household.
//
//        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("Benzin", VehicleType.class));
//        car.setMaximumVelocity(100.0 / 3.6);
//        car.setPcuEquivalents(1.0);
//        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);1,4-<2L;PC-P-Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car);
//
//        VehicleType car_diesel = VehicleUtils.getFactory().createVehicleType(Id.create("Diesel", VehicleType.class));
//        car_diesel.setMaximumVelocity(100.0 / 3.6);
//        car_diesel.setPcuEquivalents(1.0);
//        car_diesel.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;diesel;1,4-<2L;PC D Euro-4END_EMISSIONS");
//        scenario.getVehicles().addVehicleType(car_diesel);
//
//        //hybrids are only coming in hbefa vresion 4.
//
//        // process events
//        eventsManager.initProcessing();
//        for (Event event : eventList) {
//            eventsManager.processEvent(event);
//        }
//        eventsManager.finishProcessing();
//
//        double expectedDelay = delay * (freeflowTravelTime + delay) / freeflowTravelTime;
//        double actualDelay = externalityCounter.getTempValue(gps1.getId(), CongestionField.DELAY_CAUSED.getText());
//        assertEquals("Wrong delay caused!", expectedDelay, actualDelay, 0.0);
//    }
//
//
//}