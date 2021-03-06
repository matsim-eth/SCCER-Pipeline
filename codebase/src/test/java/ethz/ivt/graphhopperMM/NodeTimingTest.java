package ethz.ivt.graphhopperMM;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.*;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by molloyj on 07.11.2017.
 */
public class NodeTimingTest {

    double freespeed = 3.6;	// this is m/s and corresponds to 50km/h
    double capacity = 500.;
    double numLanes = 1.;

    Network network;
    Node node1, node2, node3, node4, node5, node6;
    Link l1, l2, l2a, l2b, l3, l4, l5;

    double start_y = 47.375221;
    double start_x = 8.514318;
    double step = 0.002;
    double end_y = start_y + 2*step;
    private MapMatching mapMatching;

    Id<Person> personId = Id.createPersonId(1);
    Id<Vehicle> vehicleId = Id.createVehicleId(1);
    private GraphHopperMATSim graphhopper;

    @Before
    public void setUpTestNetwork() {



        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        network = scenario.getNetwork();

        node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(start_x,start_y));
        node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord(start_x + 1*step, start_y));
        node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId(3), new Coord(start_x + 2*step, start_y));
        node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId(4), new Coord(start_x + 3*step, start_y));
        node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId(5), new Coord(start_x + 4*step, end_y));
        node6 = NetworkUtils.createAndAddNode(network, Id.createNodeId(6), new Coord(start_x + 5*step, end_y));

        l1 = NetworkUtils.createAndAddLink(network,Id.create( 1, Link.class), node1, node2, 100, freespeed, capacity, numLanes);
        l2 = NetworkUtils.createAndAddLink(network,Id.create( 2, Link.class), node2, node3, 100, freespeed, capacity, numLanes );
        l2a = NetworkUtils.createAndAddLink(network,Id.create( 20, Link.class), node2, node3, 100, freespeed/2, capacity, numLanes );
        l2b = NetworkUtils.createAndAddLink(network,Id.create( 21, Link.class), node2, node3, 200, freespeed, capacity, numLanes );
        l3 = NetworkUtils.createAndAddLink(network,Id.create( 3, Link.class), node3, node4, 100, freespeed, capacity, numLanes );
        l4 = NetworkUtils.createAndAddLink(network,Id.create( 4, Link.class), node4, node5, 100, freespeed, capacity, numLanes );
        l5 = NetworkUtils.createAndAddLink(network,Id.create( 5, Link.class), node5, node6, 100, freespeed, capacity, numLanes );

        network.getLinks().values().forEach(l -> l.getAttributes().putAttribute("osm:way:highway", "motorroad"));

        setUpMapMatching();
    }

    public void setUpMapMatching() {
        GraphHopperMATSim hopper = GraphHopperMATSim.build(network, new IdentityTransformation());


// create MapMatching object, can and should be shared accross threads
        String algorithm = Parameters.Algorithms.DIJKSTRA_BI;

        Weighting weighting = new FastestWeighting(hopper.getEncodingManager().getEncoder("car"));
        AlgorithmOptions algoOptions = new AlgorithmOptions(algorithm, weighting);
        mapMatching = new MapMatching(hopper, algoOptions);
        graphhopper = hopper;
    }

    @Test
    public void testNodeTiming1() {

        List<EdgeMatch> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(start_y, start_x + 0.5*step,  1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(start_y, start_x + 1.5*step, 2000), null);

        path.add(new MockEdgeMatch(l1, Arrays.asList(g1)));
        path.add(new MockEdgeMatch(l2, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);
        List<LinkGPXStruct> res = gHtoEvents.calculateNodeVisitTimes(path);
        System.out.println(res);

        assert (int) res.get(0).exitTime == 1500;
    }


    @Test
    public void testNodeTiming2() {

        // x --- Node --- Node --- x
        List<EdgeMatch> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(start_y, start_x + 0.5*step,  1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(start_y, start_x + 2.5*step,  3000), null);

        path.add(new MockEdgeMatch(l1, Arrays.asList(g1)));
        path.add(new MockEdgeMatch(l2, Collections.emptyList()));
        path.add(new MockEdgeMatch(l3, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);
        List<LinkGPXStruct> res = gHtoEvents.calculateNodeVisitTimes(path);
        System.out.println(res);

        assertEquals(res.size(),3);
        assertEquals(Math.round(res.get(0).exitTime),1500);
        assertEquals(Math.round(res.get(1).exitTime),2500);
    }

    @Test
    public void testNodeTiming3() {

        // n1 -- X -- n2  -- n3 -- X -- n4 -- n5 -- X -- n6
        List<EdgeMatch> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(start_y, start_x + 0.5*step,  1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(start_y, start_x + 2.5*step,  3000), null);
        GPXExtension g3 = new GPXExtension(new GPXEntry(end_y, start_x + 4.5*step, 5000), null);

        path.add(new MockEdgeMatch(l1, Arrays.asList(g1)));
        path.add(new MockEdgeMatch(l2, Collections.emptyList()));
        path.add(new MockEdgeMatch(l3, Arrays.asList(g2)));
        path.add(new MockEdgeMatch(l4, Collections.emptyList()));
        path.add(new MockEdgeMatch(l5, Arrays.asList(g3)));

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching); //TODO: refactor graphhopper and mapmatcher so that both aren't needed
        List<LinkGPXStruct> res = gHtoEvents.calculateNodeVisitTimes(path);
        System.out.println(res);

        assertEquals(res.size(),5);
        assertEquals(Math.round(res.get(0).entryTime),g1.getEntry().getTime());
        assertEquals(Math.round(res.get(0).exitTime),1500);
        assertEquals(Math.round(res.get(1).exitTime),2500);
        assertEquals(Math.round(res.get(2).exitTime),3500);
        assertEquals(Math.round(res.get(3).exitTime),4500);

        for (int i=0; i < res.size()-1; i++) {
            assertEquals(res.get(i).exitTime, res.get(i+1).entryTime, 0.00001);
        }

        assertEquals(Math.round(res.get(res.size()-1).exitTime),g3.getEntry().getTime());
    }


    @Test
    public void testNodeTiming4speed() {

        // x --- Node --(fast)- Node --- x
        List<EdgeMatch> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(start_y, start_x + 0.5*step, 1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(start_y, start_x + 2.5*step, 4000), null);

        path.add(new MockEdgeMatch(l1, Arrays.asList(g1)));
        path.add(new MockEdgeMatch(l2a, Collections.emptyList()));
        path.add(new MockEdgeMatch(l3, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);
        List<LinkGPXStruct> res = gHtoEvents.calculateNodeVisitTimes(path);
        System.out.println(res);

        assertEquals(res.size(),3);
        assertEquals( Math.round(res.get(0).exitTime),1500);
        assertEquals( Math.round(res.get(1).exitTime),3500);
    }


    @Test
    public void testNodeTiming5distance() {

        // x --- Node --(fast)- Node --- x
        List<EdgeMatch> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(start_y, start_x + 0.5*step, 1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(start_y, start_x + 2.5*step, 4000), null);


        path.add(new MockEdgeMatch(l1, Arrays.asList(g1)));
        path.add(new MockEdgeMatch(l2b, Collections.emptyList()));
        path.add(new MockEdgeMatch(l3, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);
        List<LinkGPXStruct> res = gHtoEvents.calculateNodeVisitTimes(path);
        System.out.println(res);

        assertEquals(res.size(), 3);
        assertEquals(Math.round(res.get(0).exitTime),1500);
        assertEquals(Math.round(res.get(1).exitTime),3500);
    }

    @Test
    public void testWhole() {

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);

        List<GPXEntry> entries = Arrays.asList(
                new GPXEntry( start_y, start_x + 0.5*step,1000),
                new GPXEntry( start_y,start_x + 4.5*step, 4000)
        );

        MatchResult mr = mapMatching.doWork(entries);
        assertEquals(4, mr.getEdgeMatches().size());

        List<LinkGPXStruct> timings = gHtoEvents.mapMatchWithTravelTimes(entries);
        System.out.println(timings);

        assertEquals (timings.size(), 4);
        assertEquals (Math.round(timings.get(0).exitTime), 1536);
        assertEquals (Math.round(timings.get(1).exitTime), 2607);
        assertEquals (Math.round(timings.get(2).exitTime), 3679);


    }

    @Test
    public void testWhole2() { //same as test2, but using the mapMatching as well

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);

        List<GPXEntry> entries = Arrays.asList(
                new GPXEntry( start_y, start_x + 0.5*step,1000),
                new GPXEntry(start_y,start_x + 2.5*step,  3000)
        );
        List<LinkGPXStruct> timings = gHtoEvents.mapMatchWithTravelTimes(entries);
        System.out.println(timings);

        assertEquals (timings.size(), 3);
        assertEquals (Math.round(timings.get(0).exitTime), 1500);
        assertEquals (Math.round(timings.get(1).exitTime), 2500);

    }

    @Test
    public void testLinkGPXtoEvents() {
        List<EdgeMatch> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(start_y, start_x + 0.5*step,  1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(start_y, start_x + 2.5*step,  3000), null);
        GPXExtension g3 = new GPXExtension(new GPXEntry(end_y,  start_x + 4.5*step, 5000), null);

        path.add(new MockEdgeMatch(l1, Arrays.asList(g1)));
        path.add(new MockEdgeMatch(l2, Collections.emptyList()));
        path.add(new MockEdgeMatch(l3, Arrays.asList(g2)));
        path.add(new MockEdgeMatch(l4, Collections.emptyList()));
        path.add(new MockEdgeMatch(l5, Arrays.asList(g3)));

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);
        List<LinkGPXStruct> res = gHtoEvents.calculateNodeVisitTimes(path);
        List<Event> events = gHtoEvents.linkGPXToEvents(res.iterator(), vehicleId);

        assertEquals(events.size(),10);

        assertEquals(g1.getEntry().getTime()/1000, events.get(0).getTime(),0.001);
        assertTrue(events.get(0) instanceof PersonDepartureEvent);

        assertEquals(events.get(1).getTime(), 1.5, 0.0001);
        assertEquals(events.get(3).getTime(), 2.5, 0.0001);
        assertEquals(events.get(5).getTime(), 3.5, 0.0001);
        assertEquals(events.get(7).getTime(), 4.5, 0.0001);

        for (int i=1; i < events.size()-1;  i+=2) {
            assertEquals(events.get(i).getTime(), events.get(i+1).getTime(), 0.0001);
            assertTrue(events.get(i) instanceof LinkLeaveEvent);
            assertTrue(events.get(i+1) instanceof LinkEnterEvent);
        }

        assertEquals(Math.round(res.get(res.size()-1).exitTime),g3.getEntry().getTime());
    }

    @Test
    public void testWholeNoTiming() {

        GHtoEvents gHtoEvents = new GHtoEvents(graphhopper, mapMatching);

        List<GPXEntry> entries = Arrays.asList(
                new GPXEntry(start_y, start_x + 0.5*step, 0),
                new GPXEntry( start_y,start_x + 4.5*step, 0)
        );
        List<Link> links = gHtoEvents.networkRouteWithoutTravelTimes(entries);
        //MatchResult mr = mapMatching.doWork(entries);
        //String edges = mr.getEdgeMatches().stream().map(em -> em.getEdgeState().getName()).collect(Collectors.joining(","));
        String edges = gHtoEvents.getEdgeString(links);

        assertEquals ("1,2,3,4",edges);
        
    }


}
