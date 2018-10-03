package ethz.ivt.externalities.aggregation;

import ethz.ivt.externalities.aggregation.CongestionAggregator;
import ethz.ivt.externalities.data.CongestionField;
import ethz.ivt.vsp.handlers.CongestionHandler;
import ethz.ivt.vsp.handlers.CongestionHandlerImplV3;
import org.junit.Assert;
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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.LinkedList;
import java.util.List;

public class MeasureAggregateCongestionFromEventsTest {

    @Test
    public void TwoVehiclesTwoLinkTest() {
        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with two links
        double length = 100.0; // m
        double freespeed = 14.0; // m/s
        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
        Node node2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0.0, 2 * length));
        network.addNode(node0);
        network.addNode(node1);
        network.addNode(node2);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 2, 1);
        Link link12 = NetworkUtils.createLink(Id.createLinkId("1-2"), node1, node2, network, length, freespeed, 2, 1);
        network.addLink(link01);
        network.addLink(link12);

        // create two persons
        Person person1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person1"));
        Person person2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person2"));
        scenario.getPopulation().addPerson(person1);
        scenario.getPopulation().addPerson(person2);

        // create some events
        double entryTime = 0.0;
        double travelTime = Math.ceil(length / freespeed);
        double delay = 1.0;
        List<Event> eventList = new LinkedList<>();
        eventList.add(new PersonDepartureEvent(0.0, person1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person1.getId(), link01.getId(), Id.createVehicleId(person1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person2.getId(), link01.getId(), Id.createVehicleId(person2.getId().toString()), TransportMode.car, 0.0));

        eventList.add(new LinkEnterEvent(entryTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(entryTime, Id.createVehicleId(person2.getId().toString()), link01.getId()));

        eventList.add(new LinkLeaveEvent(entryTime + travelTime + 0 * delay, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(entryTime + travelTime + 0 * delay, Id.createVehicleId(person1.getId().toString()), link12.getId()));
        eventList.add(new LinkLeaveEvent(entryTime + travelTime + 1 * delay, Id.createVehicleId(person2.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(entryTime + travelTime + 1 * delay, Id.createVehicleId(person2.getId().toString()), link12.getId()));

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);
            //easy option: add
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);

            scenario.getVehicles().addVehicle(v);
            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }

        // process events
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // tests
        Assert.assertEquals("Incorrect delay caused on " + link01.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced on " + link01.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect number of vehicles on " + link01.getId().toString() + " at time bin 0", 2, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.COUNT.getText()), 0.0);

        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person1.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person2.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
    }

    @Test
    public void FourVehiclesTwoLinks() {
        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with two links
        double length = 100.0; // m
        double freespeed = 14.0; // m/s
        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
        Node node2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0.0, 2 * length));
        network.addNode(node0);
        network.addNode(node1);
        network.addNode(node2);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 2, 1);
        Link link12 = NetworkUtils.createLink(Id.createLinkId("1-2"), node1, node2, network, length, freespeed, 2, 1);
        network.addLink(link01);
        network.addLink(link12);

        // create some people
        Person person1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person1"));
        Person person2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person2"));
        Person person3 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person3"));
        Person person4 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person4"));
        scenario.getPopulation().addPerson(person1);
        scenario.getPopulation().addPerson(person2);
        scenario.getPopulation().addPerson(person3);
        scenario.getPopulation().addPerson(person4);

        // create some events
        double startTime = 0.0;
        double travelTime = Math.floor(length / freespeed) + 1.0;
        double delay = 1.0;
        List<Event> eventList = new LinkedList<>();
        eventList.add(new PersonDepartureEvent(0.0, person1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person3.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person4.getId(), link01.getId(), TransportMode.car));

        eventList.add(new VehicleEntersTrafficEvent(0.0, person1.getId(), link01.getId(), Id.createVehicleId(person1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person2.getId(), link01.getId(), Id.createVehicleId(person2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person3.getId(), link01.getId(), Id.createVehicleId(person3.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person4.getId(), link01.getId(), Id.createVehicleId(person4.getId().toString()), TransportMode.car, 0.0));

        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person2.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person3.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person4.getId().toString()), link01.getId()));

        eventList.add(new LinkLeaveEvent(startTime + travelTime + 0 * delay, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 0 * delay, Id.createVehicleId(person1.getId().toString()), link12.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTime + 1 * delay, Id.createVehicleId(person2.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 1 * delay, Id.createVehicleId(person2.getId().toString()), link12.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTime + 2 * delay, Id.createVehicleId(person3.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 2 * delay, Id.createVehicleId(person3.getId().toString()), link12.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTime + 3 * delay, Id.createVehicleId(person4.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 3 * delay, Id.createVehicleId(person4.getId().toString()), link12.getId()));

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);
            //easy option: add
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);

            scenario.getVehicles().addVehicle(v);
            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }

        // process events
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        // tests
        Assert.assertEquals("Incorrect delay caused on " + link01.getId().toString() + " at time bin 0",6 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced on " + link01.getId().toString() + " at time bin 0",6 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect number of vehicles on " + link01.getId().toString() + " at time bin 0", 4, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.COUNT.getText()), 0.0);

        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person1.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 0",2 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person2.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person3.getId().toString() + " at time bin 0",3 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person3.getId().toString() + " at time bin 0",2 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person4.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person4.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person4.getId().toString() + " at time bin 0",3 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person4.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
    }

    @Test
    public void FourVehiclesTwoLinkWithStorageLinkTest() {
        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with two links + storage
        double lengthLink01 = 100.0; // 100 m
        double freespeed = 14.0; // 14 m/s
        double lengthLink12 = 1.0; // 1 m

        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0.0 * lengthLink01));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1.0 * lengthLink01));
        Node node2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0.0, 1.0 * lengthLink01 + lengthLink12));
        Node node3 = NetworkUtils.createNode(Id.createNodeId("3"), new Coord(0.0, 2.0 * lengthLink01 + lengthLink12));
        network.addNode(node0);
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, lengthLink01, freespeed, 2, 1);
        Link link12 = NetworkUtils.createLink(Id.createLinkId("1-2"), node1, node2, network, lengthLink12, freespeed, 1, 1);
        Link link23 = NetworkUtils.createLink(Id.createLinkId("2-3"), node2, node3, network, lengthLink01, freespeed, 2, 1);
        network.addLink(link01);
        network.addLink(link12);
        network.addLink(link23);

        // create some people
        Person person1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person1"));
        Person person2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person2"));
        Person person3 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person3"));
        scenario.getPopulation().addPerson(person1);
        scenario.getPopulation().addPerson(person2);
        scenario.getPopulation().addPerson(person3);

        // create some events
        double startTime = 0.0;
        double travelTimelink01 = Math.floor(lengthLink01 / freespeed) + 1.0;
        double travelTimelink12 = Math.floor(lengthLink12 / freespeed) + 1.0;
        double delay = 1.0;
        List<Event> eventList = new LinkedList<>();
        eventList.add(new PersonDepartureEvent(0.0, person1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person3.getId(), link01.getId(), TransportMode.car));

        eventList.add(new VehicleEntersTrafficEvent(0.0, person1.getId(), link01.getId(), Id.createVehicleId(person1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person2.getId(), link01.getId(), Id.createVehicleId(person2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person3.getId(), link01.getId(), Id.createVehicleId(person3.getId().toString()), TransportMode.car, 0.0));

        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person2.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person3.getId().toString()), link01.getId()));

        eventList.add(new LinkLeaveEvent(startTime + travelTimelink01, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTimelink01, Id.createVehicleId(person1.getId().toString()), link12.getId()));

        eventList.add(new LinkLeaveEvent(startTime + travelTimelink01 + 1.0 * travelTimelink12, Id.createVehicleId(person1.getId().toString()), link12.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTimelink01 + 1.0 * travelTimelink12, Id.createVehicleId(person1.getId().toString()), link23.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTimelink01 + 1.0 * travelTimelink12, Id.createVehicleId(person2.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTimelink01 + 1.0 * travelTimelink12, Id.createVehicleId(person2.getId().toString()), link12.getId()));

        eventList.add(new LinkLeaveEvent(startTime + travelTimelink01 + 2.0 * travelTimelink12 + 2 * delay, Id.createVehicleId(person2.getId().toString()), link12.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTimelink01 + 2.0 * travelTimelink12 + 2 * delay, Id.createVehicleId(person2.getId().toString()), link23.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTimelink01 + 2.0 * travelTimelink12 + 2 * delay, Id.createVehicleId(person3.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTimelink01 + 2.0 * travelTimelink12 + 2 * delay, Id.createVehicleId(person3.getId().toString()), link12.getId()));

        eventList.add(new LinkLeaveEvent(startTime + travelTimelink01 + 3.0 * travelTimelink12 + 4 * delay, Id.createVehicleId(person3.getId().toString()), link12.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTimelink01 + 3.0 * travelTimelink12 + 4 * delay, Id.createVehicleId(person3.getId().toString()), link23.getId()));

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);
            //easy option: add
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);

            scenario.getVehicles().addVehicle(v);
            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }

        // process events
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        //tests
        // link01
        Assert.assertEquals("Incorrect delay caused on " + link01.getId().toString() + " at time bin 0",5 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced on " + link01.getId().toString() + " at time bin 0", 5 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect number of vehicles on " + link01.getId().toString() + " at time bin 0", 3, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.COUNT.getText()), 0.0);

        // link12
        Assert.assertEquals("Incorrect delay caused on " + link12.getId().toString() + " at time bin 0",4 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link12.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced on " + link12.getId().toString() + " at time bin 0", 4 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link12.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect number of vehicles on " + link12.getId().toString() + " at time bin 0", 3, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link12.getId(), 0, CongestionField.COUNT.getText()), 0.0);

        // per person
        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 0",3 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person1.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person2.getId().toString() + " at time bin 0",6 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person2.getId().toString() + " at time bin 0",3 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person3.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person3.getId().toString() + " at time bin 0",6 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
    }


    @Test
    public void ThreeVehiclesTwoLinksTwoTimeBins() {
        // set up config
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        // create simple network with two links
        double length = 100.0; // m
        double freespeed = 14.0; // m/s
        Network network = scenario.getNetwork();
        Node node0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0.0, 0 * length));
        Node node1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0.0, 1 * length));
        Node node2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0.0, 2 * length));
        network.addNode(node0);
        network.addNode(node1);
        network.addNode(node2);
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 2, 1);
        Link link12 = NetworkUtils.createLink(Id.createLinkId("1-2"), node1, node2, network, length, freespeed, 2, 1);
        network.addLink(link01);
        network.addLink(link12);

        // create some people
        Person person1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person1"));
        Person person2 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person2"));
        Person person3 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person3"));
        Person person4 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person4"));
        scenario.getPopulation().addPerson(person1);
        scenario.getPopulation().addPerson(person2);
        scenario.getPopulation().addPerson(person3);
        scenario.getPopulation().addPerson(person4);

        // create some events
        double startTime = 3575.0; // start in time bin 0, but will shift to time bin 1
        double travelTime = Math.floor(length / freespeed) + 1.0;
        double delay = 10.0;
        List<Event> eventList = new LinkedList<>();
        eventList.add(new PersonDepartureEvent(0.0, person1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person2.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person3.getId(), link01.getId(), TransportMode.car));
        eventList.add(new PersonDepartureEvent(0.0, person4.getId(), link01.getId(), TransportMode.car));

        eventList.add(new VehicleEntersTrafficEvent(0.0, person1.getId(), link01.getId(), Id.createVehicleId(person1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person2.getId(), link01.getId(), Id.createVehicleId(person2.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person3.getId(), link01.getId(), Id.createVehicleId(person3.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person4.getId(), link01.getId(), Id.createVehicleId(person4.getId().toString()), TransportMode.car, 0.0));

        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person2.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person3.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person4.getId().toString()), link01.getId()));

        eventList.add(new LinkLeaveEvent(startTime + travelTime + 0 * delay, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 0 * delay, Id.createVehicleId(person1.getId().toString()), link12.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTime + 1 * delay, Id.createVehicleId(person2.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 1 * delay, Id.createVehicleId(person2.getId().toString()), link12.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTime + 2 * delay, Id.createVehicleId(person3.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 2 * delay, Id.createVehicleId(person3.getId().toString()), link12.getId()));
        eventList.add(new LinkLeaveEvent(startTime + travelTime + 3 * delay, Id.createVehicleId(person4.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + travelTime + 3 * delay, Id.createVehicleId(person4.getId().toString()), link12.getId()));

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);
            //easy option: add
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);

            scenario.getVehicles().addVehicle(v);
            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }

        // process events
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        //tests

        //time bin 0
        Assert.assertEquals("Incorrect delay caused on " + link01.getId().toString() + " at time bin 0",6 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced on " + link01.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect number of vehicles on " + link01.getId().toString() + " at time bin 0", 4, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.COUNT.getText()), 0.0);

        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person1.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 0",2 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person2.getId().toString() + " at time bin 0",1 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person3.getId().toString() + " at time bin 0",3 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person3.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person4.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person4.getId(), 0, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person4.getId().toString() + " at time bin 0",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person4.getId(), 0, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);

        //time bin 1
        Assert.assertEquals("Incorrect delay caused on " + link01.getId().toString() + " at time bin 1",0 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 1, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced on " + link01.getId().toString() + " at time bin 1",5 * delay, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 1, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect number of vehicles on " + link01.getId().toString() + " at time bin 1",2, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 1, CongestionField.COUNT.getText()), 0.0);

        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 1",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 1, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person1.getId().toString() + " at time bin 1",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person1.getId(), 1, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person1.getId().toString() + " at time bin 1",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 1, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person2.getId().toString() + " at time bin 1",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person2.getId(), 1, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person3.getId().toString() + " at time bin 1",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 1, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person3.getId().toString() + " at time bin 1",2 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person3.getId(), 1, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay caused by " + person4.getId().toString() + " at time bin 1",0 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person4.getId(), 1, CongestionField.DELAY_CAUSED.getText()), 0.0);
        Assert.assertEquals("Incorrect delay experienced by " + person4.getId().toString() + " at time bin 1",3 * delay, congestionAggregator.aggregateCongestionDataPerPersonPerTime.getValue(person4.getId(), 1, CongestionField.DELAY_EXPERIENCED.getText()), 0.0);

    }

    @Test
    public void VehicleReenteringSameLinkInSameTimeBinIsCountedTwice() {
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
        Link link01 = NetworkUtils.createLink(Id.createLinkId("0-1"), node0, node1, network, length, freespeed, 2, 1);
        network.addLink(link01);

        // create some people
        Person person1 = PopulationUtils.getFactory().createPerson(Id.createPersonId("person1"));
        scenario.getPopulation().addPerson(person1);

        // create some events
        double startTime = 0.0; // start in time bin 0, but will shift to time bin 1
        double travelTime = Math.floor(length / freespeed) + 1.0;
        List<Event> eventList = new LinkedList<>();
        eventList.add(new PersonDepartureEvent(0.0, person1.getId(), link01.getId(), TransportMode.car));
        eventList.add(new VehicleEntersTrafficEvent(0.0, person1.getId(), link01.getId(), Id.createVehicleId(person1.getId().toString()), TransportMode.car, 0.0));
        eventList.add(new LinkEnterEvent(startTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(startTime + 1.0 * travelTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkEnterEvent(startTime + 1.0 * travelTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));
        eventList.add(new LinkLeaveEvent(startTime + 2.0 * travelTime, Id.createVehicleId(person1.getId().toString()), link01.getId()));

        // set up event manager and handlers
        EventsManager eventsManager = new EventsManagerImpl();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        eventsManager.addHandler(v2deh);

        CongestionHandler congestionHandler = new CongestionHandlerImplV3(eventsManager, scenario);
        CongestionAggregator congestionAggregator = new CongestionAggregator(scenario, v2deh);
        eventsManager.addHandler(congestionHandler);
        eventsManager.addHandler(congestionAggregator);

        //householdid, #autos, auto1, auto2, auto3
        //get household id of person. Assign next vehicle from household.

        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
        car.setMaximumVelocity(60.0 / 3.6);
        car.setPcuEquivalents(1.0);
        car.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;petrol (4S);>=2L;PC-P-Euro-3END_EMISSIONS");
        scenario.getVehicles().addVehicleType(car);

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);
            //easy option: add
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);

            scenario.getVehicles().addVehicle(v);
            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }

        // process events
        for (Event event : eventList) {
            eventsManager.processEvent(event);
        }
        eventsManager.finishProcessing();

        //tests
        Assert.assertEquals("Incorrect number of vehicles on " + link01.getId().toString() + " at time bin 0", 2, congestionAggregator.aggregateCongestionDataPerLinkPerTime.getValue(link01.getId(), 0, CongestionField.COUNT.getText()), 0.0);
    }
}