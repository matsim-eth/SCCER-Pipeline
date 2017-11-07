package playground.ivt.proj_sccer.graphhopperMM;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.Path;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.*;

/**
 * Created by molloyj on 07.11.2017.
 */
public class GHtoEvents {

    private final MapMatching matcher;
    Network network;

    public GHtoEvents(MapMatching matcher, Network network) {
        this.network = network;
        this.matcher = matcher;
    }

    protected class NodeArrivalStruct {
        public double time;
        public final Node node;

        public NodeArrivalStruct(Node node, double time) {
            this.node = node; this.time = time;
        }

        @Override
        public String toString() {
            return "{" +
                    "node=" + node.getId() +
                    ", time=" + time +
                    '}';
        }
    }


    public List<NodeArrivalStruct> interpolateMMresult(List<GPXEntry> entries) {

        MatchResult mr = matcher.doWork(entries);
        List<LinkGPXStruct> edges = createPath(mr);
        return gpxToNodeTimes(edges);
    }

    private List<LinkGPXStruct> createPath(MatchResult mr) {

        Path path = matcher.calcPath(mr);
        ListIterator<EdgeMatch> matchedededges = mr.getEdgeMatches().listIterator();
        ListIterator<EdgeIteratorState> pathEdges = path.calcEdges().listIterator();
        EdgeMatch e = matchedededges.next();
        EdgeIteratorState p = pathEdges.next();

        List<LinkGPXStruct> edges = new ArrayList<>();

        while (pathEdges.hasNext()) {
            Link link = network.getLinks().get(Id.createLinkId(p.getEdge()));
            if (p.getEdge() != e.getEdgeState().getEdge()) {
                LinkGPXStruct emptyMatch = new LinkGPXStruct(link, Collections.emptyList());
                edges.add(emptyMatch);
            } else if (e.isEmpty()) {
                LinkGPXStruct emptyMatch = new LinkGPXStruct(link, Collections.emptyList());
                edges.add(emptyMatch);
                e = matchedededges.next();
            } else { //e == p
                LinkGPXStruct match = new LinkGPXStruct(link, e.getGpxExtensions());
                edges.add(match);
                e = matchedededges.next();
            }
            p = pathEdges.next();
        }
        return edges;
    }

    public List<NodeArrivalStruct> gpxToNodeTimes(List<LinkGPXStruct> path) {
        ListIterator<LinkGPXStruct> pathEdges = path.listIterator();

        //list of nodes (x1... xk)
        ArrayList<NodeArrivalStruct> nodes = new ArrayList<>();
        //list of t0... tn
        ArrayList<NodeArrivalStruct> T_list = new ArrayList<>(path.size() - 1); //we should have |edges|-1 nodes


        LinkGPXStruct e = pathEdges.next();
        GPXExtension x0 = e.getGpxExtensions().get(e.getGpxExtensions().size() - 1);//get node from e
        GPXExtension x1 = null;
        //can assume that first edge has a point. add end(e) to n_list, time(x, end(e)) to t_list
        double aTime = timeBetween(x0, e.getLink());
        double map_time = 0 + aTime;
        nodes.add(new NodeArrivalStruct(e.getToNode(), aTime));

        while (pathEdges.hasNext()) {
            e = pathEdges.next();

            Node endNode = e.getToNode();

            if(e.isEmpty()) {
                //add p to t_list, node to n_list
                aTime = timeBetween(e.getLink());
                map_time += aTime;
                nodes.add(new NodeArrivalStruct(endNode, aTime));
            } else { //finish off this section!
                x1 = e.getGpxExtensions().get(0);//first point of e
                //add time(start(e), x1) to t_list)
                aTime = timeBetween(e.getLink(), x1);
                map_time += aTime;

                //T_list = t_list / sum(t_list) * (x1 - x0)

                final double real_time = timeBetween(x0, x1);
                final double lastNodeTime = x0.getEntry().getTime();
                //cum_sum T_list
                final double final_map_time = map_time;

                nodes.forEach(n -> n.time = n.time * (real_time / final_map_time));
                addStartValue(nodes, lastNodeTime); //recursion for fun!

                T_list.addAll(nodes);
                nodes.clear();

                x0 = e.getGpxExtensions().get(e.getGpxExtensions().size() - 1); //last point of e

                aTime = timeBetween(x0, e.getLink());
                map_time = aTime;
                nodes.add(new NodeArrivalStruct(e.getToNode(), aTime));

            }

        }
        return T_list;

    }

    private void addStartValue(List<NodeArrivalStruct> nodes, double lastNodeTime) {
        if (!nodes.isEmpty()) {
            nodes.get(0).time += lastNodeTime;
            addStartValue(nodes.subList(1, nodes.size()), nodes.get(0).time);
        }
    }
/*
    public List<Event> timedEdgesToEvents(Iterator<EdgeMatch> routedEdges) {
        EdgeMatch currEdge = null;
        List<Event> events = new ArrayList<>();
        while (routedEdges.hasNext()) {
            EdgeMatch prevEdge = currEdge;
            currEdge = routedEdges.next();

            Id<Link> link = toLink(currEdge);

            if (currEdge.getEdgeState() == null)
                //get first and last timestamp & location on each link

                //what if the link has no GPXextensions?

                //routing problem with constraints - need to make sure person was at certain point at certain time
                //allocate travel time porportional to free flow travel time - could also use Hellinga 2008 - Decomposing travel times

                if (!edgeIter.hasPrevious()) { //first link
                    linkEnterTime = currEdge.getGpxExtensions().get(0).getEntry().getTime();
                    events.add(new PersonDepartureEvent(linkEnterTime, agentId, link, TransportMode.car));
                    events.add(new LinkLeaveEvent(0, vehicleId, link));
                }
                else if (edgeIter.hasNext()) { //middle links
                    events.add(new LinkEnterEvent(0, vehicleId, link));
                    events.add(new LinkLeaveEvent(0, vehicleId, link));
                } else { //last link
                    events.add(new LinkEnterEvent(0, vehicleId, link));
                    events.add(new PersonArrivalEvent(0, agentId, link, TransportMode.car));
                }
            linkEnterTime = linkLeaveTime;
        }

        return events;
    }
*/


    private Coordinate coordToCoordinate(Coord coord) {
        return new Coordinate(coord.getY(), coord.getX());
    }

    private double timeBetween(GPXExtension x0, GPXExtension x1) {
        return x1.getEntry().getTime() - x0.getEntry().getTime();
    }

    private double timeBetween(GPXExtension x0, Link l) {
        //get distance from x0 to e.

        Coordinate x0_coord = new Coordinate(x0.getEntry().getLat(), x0.getEntry().getLon()); //TODO lon lat order?

        Coordinate start_coord = coordToCoordinate(l.getFromNode().getCoord());
        Coordinate end_coord = coordToCoordinate(l.getToNode().getCoord());

        LengthIndexedLine ls = new LengthIndexedLine( new GeometryFactory().createLineString(new Coordinate[]{start_coord,end_coord}));
        double distance_x0_e = ls.getEndIndex() - ls.project(x0_coord);
        double speed_e = l.getFreespeed();
        double time_e = distance_x0_e / speed_e;

        return time_e;
    }

    private double timeBetween(Link l, GPXExtension x1) {
        //get distance from x0 to e.

        Coordinate x1_coord = new Coordinate(x1.getEntry().getLat(), x1.getEntry().getLon()); //TODO lon lat order?

        Coordinate start_coord = coordToCoordinate(l.getFromNode().getCoord());
        Coordinate end_coord = coordToCoordinate(l.getToNode().getCoord());

        LengthIndexedLine ls = new LengthIndexedLine( new GeometryFactory().createLineString(new Coordinate[]{start_coord,end_coord}));
        double distance_x0_e = ls.project(x1_coord) - ls.getStartIndex();
        double speed_e = l.getFreespeed();;
        double time_e = distance_x0_e / speed_e;

        return time_e;
    }

    private double timeBetween(Link l) {
        double distance_x0_e = l.getLength();
        double speed_e = l.getFreespeed();
        double time_e = distance_x0_e / speed_e;

        return time_e;
    }
    private Coordinate getNodeCoordinate(int node) {
        return coordToCoordinate(network.getNodes().get(Id.createNodeId(node)).getCoord());
    }

}
