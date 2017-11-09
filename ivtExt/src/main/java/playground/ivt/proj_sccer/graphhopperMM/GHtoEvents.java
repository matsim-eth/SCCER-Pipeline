package playground.ivt.proj_sccer.graphhopperMM;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

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

    public List<LinkGPXStruct> interpolateMMresult(List<GPXEntry> entries, Id<Person> personId, Id<Vehicle> vehicleId) {

        MatchResult mr = matcher.doWork(entries);
        List<LinkGPXStruct> edges = convertToMatsimLinks(mr.getEdgeMatches(), personId, vehicleId);
        //List<LinkGPXStruct> path = convertToMatsimLinks(matcher.calcPath(mr).calcEdges());
        //List<LinkGPXStruct> mergedPath = mergePaths(edges, path);
        return calculateNodeVisitTimes(edges);
    }

private <T> List<LinkGPXStruct> convertToMatsimLinks(List<T> edges, Id<Person> personId, Id<Vehicle> vehicleId) {
        return edges.stream().map(e -> {
            String edgeIndex = "";
            List<GPXExtension> gpxList = Collections.emptyList();
            if (e instanceof EdgeMatch) {
                edgeIndex = ((EdgeMatch) e).getEdgeState().getName();
                gpxList = ((EdgeMatch) e).getGpxExtensions();
            } else if (e instanceof EdgeIteratorState) {
                edgeIndex = ((EdgeIteratorState) e).getName();
            } else {
                throw new RuntimeException("Only EdgeMatch and EdgeStateIterator Supported");
            }
            Link link = network.getLinks().get(Id.createLinkId(edgeIndex));
            return new LinkGPXStruct(link, gpxList, personId, vehicleId);
        }).collect(Collectors.toList());
    }

    public List<LinkGPXStruct> calculateNodeVisitTimes(List<LinkGPXStruct> path) {
        ListIterator<LinkGPXStruct> pathEdges = path.listIterator();

        //list of nodes (x1... xk)
        ArrayList<LinkGPXStruct> currLinks = new ArrayList<>();
           //list of t0... tn
     //   ArrayList<LinkGPXStruct> T_list = new ArrayList<>(path.size() - 1); //we should have |edges|-1 nodes


        LinkGPXStruct e = pathEdges.next();
        GPXExtension x0 = e.getGpxExtensions().get(e.getGpxExtensions().size() - 1);//get node from e
        GPXExtension x1 = null;
        //can assume that first edge has a point. add end(e) to n_list, time(x, end(e)) to t_list
        double aTime = timeBetween(x0, e.getLink());
        double map_time = 0 + aTime;
    //    nodes.add(new NodeTimingStruct(e.getToNode(), aTime));
        e.exitTime = aTime;
        e.entryTime = x0.getEntry().getTime();

        LinkGPXStruct firstLink = e;

        while (pathEdges.hasNext()) {
            LinkGPXStruct prevE = e;
            e = pathEdges.next();
            e.entryTime = prevE.exitTime;
            currLinks.add(e);

            if(e.isEmpty()) {
                //add p to t_list, node to n_list
                aTime = timeBetween(e.getLink());
                map_time += aTime;
                e.exitTime = e.entryTime + aTime;
            } else { //finish off this section of road!
                x1 = e.getGpxExtensions().get(0);//first point of e
                aTime = timeBetween(e.getLink(), x1);

                map_time += aTime;

                final double real_time = timeBetween(x0, x1);
                final double lastNodeTime = x0.getEntry().getTime();
                 double final_map_time = map_time;

                firstLink.exitTime = lastNodeTime + firstLink.exitTime*(real_time / final_map_time);

                currLinks.forEach(n -> n.scaleTimesBy(lastNodeTime, real_time / final_map_time));

                x0 = e.getGpxExtensions().get(e.getGpxExtensions().size() - 1); //last point of e

                aTime = timeBetween(x0, e.getLink());

                if (pathEdges.hasNext()) e.exitTime = aTime;
                else e.exitTime = x1.getEntry().getTime();

                map_time = aTime;
                firstLink = e;
                currLinks.clear();
            }


        }
        return path;

    }


    public List<Event> LinkGPXToEvents(Iterator<LinkGPXStruct> x) {
        List<Event> events = new ArrayList<>();
        LinkGPXStruct firstE = x.next();
        events.add(new PersonDepartureEvent(firstE.entryTime, firstE.personId, firstE.getLink().getId(), TransportMode.car));
        events.add(new LinkLeaveEvent(firstE.exitTime, firstE.vehicleId, firstE.getLink().getId()));

        while (x.hasNext()) {
            LinkGPXStruct curr = x.next();
            if (x.hasNext()) {
                events.add(new LinkEnterEvent(curr.entryTime, firstE.vehicleId, firstE.getLink().getId()));
                events.add(new LinkLeaveEvent(curr.exitTime, firstE.vehicleId, firstE.getLink().getId()));
            } else { //process final element
                events.add(new LinkEnterEvent(curr.entryTime, firstE.vehicleId, firstE.getLink().getId()));
                events.add(new PersonArrivalEvent(curr.exitTime, firstE.personId, firstE.getLink().getId(), TransportMode.car));
            }
        }

        return events;
    }



    private Coordinate coordToCoordinate(Coord coord) {
        return new Coordinate(coord.getX(), coord.getY());
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
        double distance_covered = (ls.getEndIndex() - ls.project(x0_coord)) / ls.getEndIndex();
        double distance = distance_covered * l.getLength();
        double speed_e = l.getFreespeed();
        double time_e = distance / speed_e;

        return time_e;
    }

    private double timeBetween(Link l, GPXExtension x1) {
        //get distance from x0 to e.

        Coordinate x1_coord = new Coordinate(x1.getEntry().getLat(), x1.getEntry().getLon()); //TODO lon lat order?

        Coordinate start_coord = coordToCoordinate(l.getFromNode().getCoord());
        Coordinate end_coord = coordToCoordinate(l.getToNode().getCoord());

        LengthIndexedLine ls = new LengthIndexedLine( new GeometryFactory().createLineString(new Coordinate[]{start_coord,end_coord}));
        double distance_covered = (ls.project(x1_coord) - ls.getStartIndex()) / ls.getEndIndex();
        double distance = distance_covered * l.getLength();
        double speed_e = l.getFreespeed();;
        double time_e = distance / speed_e;

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
