package playground.ivt.proj_sccer.graphhopperMM;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
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
import java.util.List;

/**
 * Created by molloyj on 07.11.2017.
 */
public class NodeTimingTest {

    private Network network;

    @Test
    public void testNodeTiming1() {

        double freespeed = 2.7;	// this is m/s and corresponds to 50km/h
        double capacity = 500.;
        double numLanes = 1.;


        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();

        Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord((double) 0, (double) 100));
        Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord((double) 0, (double) 200));
        Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId(3), new Coord((double) 0, (double) 300));

        Link l1 = NetworkUtils.createAndAddLink(network,Id.create( 1, Link.class), node1, node2, 100, freespeed, capacity, numLanes );
        Link l2 = NetworkUtils.createAndAddLink(network,Id.create( 2, Link.class), node2, node3, 100, freespeed, capacity, numLanes );


        // x --- Node --- x
        List<LinkGPXStruct> path = new ArrayList<>();

        GPXExtension g1 = new GPXExtension(new GPXEntry(0, 150, 100), null);
        GPXExtension g2 = new GPXExtension(new GPXEntry(0, 250, 200), null);

        path.add(new LinkGPXStruct(l1, Arrays.asList(g1)));
        path.add(new LinkGPXStruct(l2, Arrays.asList(g2)));

        GHtoEvents gHtoEvents = new GHtoEvents(null, network);
        List<GHtoEvents.NodeArrivalStruct> res = gHtoEvents.gpxToNodeTimes(path);
        System.out.println(res);

        assert false;
    }
}
