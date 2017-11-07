package playground.ivt.proj_sccer.graphhopperMM;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    @Before
    public void setUpTestNetwork() {



        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        network = scenario.getNetwork();

        node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord(100,0));
        node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord(200, 0));
        node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId(3), new Coord(300, 0));
        node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId(4), new Coord(400, 0));
        node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId(5), new Coord(500, 0));
        node6 = NetworkUtils.createAndAddNode(network, Id.createNodeId(6), new Coord(600, 0));

        l1 = NetworkUtils.createAndAddLink(network,Id.create( 1, Link.class), node1, node2, 100, freespeed, capacity, numLanes );
        l2 = NetworkUtils.createAndAddLink(network,Id.create( 2, Link.class), node2, node3, 100, freespeed, capacity, numLanes );
        l2a = NetworkUtils.createAndAddLink(network,Id.create( 20, Link.class), node2, node3, 100, freespeed/2, capacity, numLanes );
        l2b = NetworkUtils.createAndAddLink(network,Id.create( 21, Link.class), node2, node3, 200, freespeed, capacity, numLanes );
        l3 = NetworkUtils.createAndAddLink(network,Id.create( 3, Link.class), node3, node4, 100, freespeed, capacity, numLanes );
        l4 = NetworkUtils.createAndAddLink(network,Id.create( 4, Link.class), node4, node5, 100, freespeed, capacity, numLanes );
        l5 = NetworkUtils.createAndAddLink(network,Id.create( 5, Link.class), node5, node6, 100, freespeed, capacity, numLanes );


    }

    @Test
    public void testNodeTiming1() {

        List<LinkGPXStruct> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(0, 150, 1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(0, 250, 2000), null);

        path.add(new LinkGPXStruct(l1, Arrays.asList(g1)));
        path.add(new LinkGPXStruct(l2, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(null, network);
        List<GHtoEvents.NodeArrivalStruct> res = gHtoEvents.gpxToNodeTimes(path);
        System.out.println(res);

        assert res.get(0).time == 1500;
    }


    @Test
    public void testNodeTiming2() {

        // x --- Node --- Node --- x
        List<LinkGPXStruct> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(0, 150, 1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(0, 350, 3000), null);

        path.add(new LinkGPXStruct(l1, Arrays.asList(g1)));
        path.add(new LinkGPXStruct(l2, Collections.emptyList()));
        path.add(new LinkGPXStruct(l3, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(null, network);
        List<GHtoEvents.NodeArrivalStruct> res = gHtoEvents.gpxToNodeTimes(path);
        System.out.println(res);

        assert(res.size() == 2);
        assert res.get(0).time == 1500;
        assert res.get(1).time == 2500;
    }

    @Test
    public void testNodeTiming3() {

        // n1 -- X -- n2  -- n3 -- X -- n4 -- n5 -- X -- n6
        List<LinkGPXStruct> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(0, 150, 1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(0, 350, 3000), null);
        GPXExtension g3 = new GPXExtension(new GPXEntry(0, 550, 5000), null);

        path.add(new LinkGPXStruct(l1, Arrays.asList(g1)));
        path.add(new LinkGPXStruct(l2, Collections.emptyList()));
        path.add(new LinkGPXStruct(l3, Arrays.asList(g2)));
        path.add(new LinkGPXStruct(l4, Collections.emptyList()));
        path.add(new LinkGPXStruct(l5, Arrays.asList(g3)));

        GHtoEvents gHtoEvents = new GHtoEvents(null, network);
        List<GHtoEvents.NodeArrivalStruct> res = gHtoEvents.gpxToNodeTimes(path);
        System.out.println(res);

        assert(res.size() == 4);
        assert res.get(0).time == 1500;
        assert res.get(1).time == 2500;
        assert res.get(2).time == 3500;
        assert res.get(3).time == 4500;
    }


    @Test
    public void testNodeTiming4speed() {

        // x --- Node --(fast)- Node --- x
        List<LinkGPXStruct> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(0, 150, 1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(0, 350, 4000), null);

        path.add(new LinkGPXStruct(l1, Arrays.asList(g1)));
        path.add(new LinkGPXStruct(l2a, Collections.emptyList()));
        path.add(new LinkGPXStruct(l3, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(null, network);
        List<GHtoEvents.NodeArrivalStruct> res = gHtoEvents.gpxToNodeTimes(path);
        System.out.println(res);

        assert(res.size() == 2);
        assert res.get(0).time == 1500;
        assert res.get(1).time == 3500;
    }


    @Test
    public void testNodeTiming5distance() {

        // x --- Node --(fast)- Node --- x
        List<LinkGPXStruct> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(0, 150, 1000), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(0, 350, 4000), null);

        path.add(new LinkGPXStruct(l1, Arrays.asList(g1)));
        path.add(new LinkGPXStruct(l2b, Collections.emptyList()));
        path.add(new LinkGPXStruct(l3, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(null, network);
        List<GHtoEvents.NodeArrivalStruct> res = gHtoEvents.gpxToNodeTimes(path);
        System.out.println(res);

        assert(res.size() == 2);
        assert res.get(0).time == 1500;
        assert res.get(1).time == 3500;
    }



}
