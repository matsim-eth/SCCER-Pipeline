package playground.ivt.proj_sccer.traces2matsim;


import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.NodeAccess;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by molloyj on 23.10.2017.
 *
 * This converts the MATSim network into a graphhopper graph.
 * It assumes that the node structure of the network has already been simplified
 *
 */
public class MATSimNetwork2graphhopper implements DataReader  {

    private final Logger log = Logger.getLogger(MATSimNetwork2graphhopper.class);


    private final GraphStorage graphStorage;
    private final NodeAccess nodeAccess;
    protected final Graph graph;
    private Network network;
    private Map<Node, Integer> nodes = new HashMap<>();

    public MATSimNetwork2graphhopper(GraphHopperStorage ghStorage, Network network) {
        this.graphStorage = ghStorage;
        this.graph = ghStorage;
        this.nodeAccess = graph.getNodeAccess();

        this.network = network;

    }

    @Override
    public void readGraph() {
        graphStorage.create(1000);
        processJunctions();
        processRoads();
    }

    private void processRoads() {
        this.network.getLinks().values().forEach(l -> {
            Node fromNode = l.getFromNode();
            Node toNode = l.getToNode();
            if (l.getAllowedModes().contains(TransportMode.car)) {
                graph.edge(nodes.get(fromNode), nodes.get(toNode), l.getLength(), false);
            }
        });
    }

    private void processJunctions() {
        AtomicInteger i = new AtomicInteger();
        this.network.getNodes().values().forEach(x -> {
            int nodeId = nodes.computeIfAbsent(x, a -> i.getAndIncrement());
            nodeAccess.setNode(nodeId, x.getCoord().getX(), x.getCoord().getY());
        });

    }


    @Override
    public DataReader setFile(File file) {
        return this;
    }

    @Override
    public DataReader setElevationProvider(ElevationProvider ep) {
        return this;
    }

    @Override
    public DataReader setWorkerThreads(int workerThreads) {
        return this;
    }

    @Override
    public DataReader setWayPointMaxDistance(double wayPointMaxDistance) {
        return this;
    }

    @Override
    public Date getDataDate() {
        return null;
    }
}
