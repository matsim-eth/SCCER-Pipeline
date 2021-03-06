package ethz.ivt.graphhopperMM;


import com.graphhopper.reader.DataReader;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.reader.dem.ElevationProvider;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.GraphStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.GHPoint;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.apache.log4j.Logger;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

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

    protected EncodingManager encodingManager;
    private final HashSet<EdgeAddedListener> edgeAddedListeners = new HashSet<>();

    private final GraphStorage graphStorage;
    private final NodeAccess nodeAccess;

    protected final Graph graph;
    private Network network;
    private Map<Coord, Integer> nodes = new HashMap<>();
    private final CoordinateTransformation convertor;

    public MATSimNetwork2graphhopper(GraphHopperStorage ghStorage, Network network, CoordinateTransformation mATSim2WGS84Conversion) {
        this.graphStorage = ghStorage;
        this.graph = ghStorage;
        this.nodeAccess = graph.getNodeAccess();
        this.encodingManager = ghStorage.getEncodingManager();
        this.convertor = mATSim2WGS84Conversion;

        this.network = network;

    }

    public interface EdgeAddedListener {
        void edgeAdded(ReaderWay way, EdgeIteratorState edge);
    }

    @Override
    public void readGraph() {
        //TODO: make it so that the network is read in here instead of earlier
        graphStorage.create(1000);
        processJunctions();
        processRoads();
    }

    private void processRoads() {
        this.network.getLinks().values().forEach(l -> {
            Node fromNode = l.getFromNode();
            Node toNode = l.getToNode();
            if (l.getAllowedModes().contains(TransportMode.car)) {

                Coord wgs_from_node = convertor.transform(fromNode.getCoord());
                Coord wgs_to_node = convertor.transform(toNode.getCoord());

                GHPoint estmCentre = new GHPoint(
                        0.5 * (wgs_from_node.getY() + wgs_to_node.getY()),
                        0.5 * (wgs_from_node.getX() + wgs_to_node.getX()));

                addEdge(nodes.get(wgs_from_node),  nodes.get(wgs_to_node), l, l.getLength(), estmCentre);

                //graph.edge(nodes.get(fromNode), nodes.get(toNode), l.getLength(), false);
            }
            else {
                //log.debug(l.getId());
            }
        });
    }


    private void addEdge(int fromTower, int toTower, Link road, double distance,
                         GHPoint estmCentre) {
        EdgeIteratorState edge = graph.edge(fromTower, toTower);

        // read the OSM id, should never be null
        long id = Integer.parseInt(String.valueOf(road.getId()));
        edge.setName(String.valueOf(road.getId()));

        // Make a temporary ReaderWay object with the properties we need so we
        // can use the enocding manager
        // We (hopefully don't need the node structure on here as we're only
        // calling the flag
        // encoders, which don't use this...
        ReaderWay way = new ReaderWay(id);

        way.setTag("estimated_distance", distance);
        way.setTag("estimated_center", estmCentre);
        way.setTag("motorroad", "yes");
        // read the highway type
        Object typeString = road.getAttributes().getAttribute("osm:way:highway");
        if (typeString == null) typeString = NetworkUtils.getType(road);
        way.setTag("highway", typeString.toString()); //TODO: this isnt great, we should use the names from the link types

        // read maxspeed filtering for 0 which for Geofabrik shapefiles appears
        // to correspond to no tag
        double maxSpeed = road.getFreespeed();

        if (maxSpeed > 0) {
            way.setTag("maxspeed", Double.toString(maxSpeed));
        }

        // read oneway

        way.setTag("oneway", "yes");

/* ignore as we are using the matsim network
        // Process the flags using the encoders
        long includeWay = encodingManager.acceptWay(way);
        if (includeWay == 0) {
            return;
        }
*/
        // TODO we're not using the relation flags
        long relationFlags = 0;
        long includeWay = 1;

        long wayFlags = encodingManager.handleWayTags(way, includeWay, relationFlags);
        //      log.info("default speed" + encodingManager.fetchEdgeEncoders().get(0).getSpeed(wayFlags));

        //set link speed to matsim speed, as we don't want to use the OSM defaults
        wayFlags = encodingManager.getEncoder("car").setSpeed(wayFlags, road.getFreespeed());

        //      log.info("matsim speed" + encodingManager.fetchEdgeEncoders().get(0).getSpeed(wayFlags));
        if (wayFlags == 0)
            return;

        edge.setDistance(distance);
        edge.setFlags(wayFlags);
        //    edge.setWayGeometry(pillarNodes);

        if (edgeAddedListeners.size() > 0) {
            // check size first so we only allocate the iterator if we have
            // listeners
            for (EdgeAddedListener l : edgeAddedListeners) {
                l.edgeAdded(way, edge);
            }
        }
    }

    public void addListener(EdgeAddedListener l) {
        edgeAddedListeners.add(l);
    }



    private void processJunctions() {
        AtomicInteger i = new AtomicInteger();
        //log.debug(network);

        this.network.getNodes().values().forEach(x -> {
            Coord wgs_from_node = convertor.transform(x.getCoord());

            int nodeId = nodes.computeIfAbsent(wgs_from_node, a -> i.getAndIncrement());
            nodeAccess.setNode(nodeId, wgs_from_node.getY(), wgs_from_node.getX());
            //log.debug("[id=" + x.getId() + ",lat=" + wgs_from_node.getY() + ",lon=" + wgs_from_node.getX() + "]");
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
