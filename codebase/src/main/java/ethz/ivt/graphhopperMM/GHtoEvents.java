package ethz.ivt.graphhopperMM;

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
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
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by molloyj on 07.11.2017.
 */
public class GHtoEvents {

    private final MapMatching matcher;
    private GraphHopperMATSim hopper;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public GHtoEvents(GraphHopperMATSim hopper, MapMatching matcher) {
        this.matcher = matcher;
        this.hopper = hopper;
    }



    public List<Link> reduceLinkGPX(List<LinkGPXStruct> points) {
        List<Link> links = points
                .stream()
                .map(em -> em.getLink())
                .collect(Collectors.toList());
        return links;
    }
    public List<Link> networkRouteWithoutTravelTimes(List<GPXEntry> points) {
        MatchResult mr = matcher.doWork(points);
        List<Link> links = mr.getEdgeMatches()
                .stream()
                .map(em -> getNetwork().getLinks().get(Id.createLinkId(em.getEdgeState().getName()))
                ).collect(Collectors.toList());
        return links;
    }

    public List<Event> gpsToEvents(List<GPXEntry> entries, Id<Person> personId, Id<Vehicle> vehicleId, String mode) {
        return linkGPXToEvents(mapMatchWithTravelTimes(entries).iterator(), personId, vehicleId, mode);
    }


    public List<LinkGPXStruct> mapMatchWithTravelTimes(List<GPXEntry> entries) {
        if (entries.size() < 2) return new ArrayList<>();
        try {
            MatchResult mr = getMatcher().doWork(entries);
            return calculateNodeVisitTimes(mr.getEdgeMatches());
        } catch (IllegalArgumentException ex) {
            logger.warning(ex.getMessage());
            return Collections.EMPTY_LIST;

        }
    }

    public String getEdgeString(List<Link> links) {
        return links.stream().map(l -> l.getId().toString()).collect(Collectors.joining(","));
    }


    private LinkGPXStruct convertToLinkStruct(EdgeMatch e) {
        String edgeIndex = e.getEdgeState().getName();
        Link link = getNetwork().getLinks().get(Id.createLinkId(edgeIndex));
        return new LinkGPXStruct(link, e.getGpxExtensions());
    }

    List<LinkGPXStruct> calculateNodeVisitTimes(List<EdgeMatch> path) {
        ListIterator<EdgeMatch> pathEdges = path.listIterator();

        ArrayList<LinkGPXStruct> currLinks = new ArrayList<>();
        ArrayList<LinkGPXStruct> resultLinks = new ArrayList<>();

        EdgeMatch edgeMatch = pathEdges.next();
        LinkGPXStruct eLink = convertToLinkStruct(edgeMatch);
        resultLinks.add(eLink);
        GPXExtension x0 = eLink.getGpxExtensions().get(eLink.getGpxExtensions().size() - 1);//get node from eLink
        GPXExtension x1 = null;
        //can assume that first edge has a point. add end(eLink) to n_list, time(x, end(eLink)) to t_list
        double aTime = timeBetween(x0, eLink.getLink());
        double map_time = 0 + aTime;
        eLink.exitTime = aTime;
        eLink.entryTime = x0.getEntry().getTime();


        LinkGPXStruct firstLink = eLink;

        while (pathEdges.hasNext()) {
            LinkGPXStruct prevE = eLink;
            edgeMatch = pathEdges.next();
            eLink = convertToLinkStruct(edgeMatch);
            resultLinks.add(eLink);
            eLink.entryTime = prevE.exitTime;
            currLinks.add(eLink);

            if(eLink.isEmpty()) {
                //add p to t_list, node to n_list
                aTime = timeBetween(eLink.getLink());
                map_time += aTime;
                eLink.exitTime = eLink.entryTime + aTime;
            } else { //finish off this section of road!
                x1 = eLink.getGpxExtensions().get(0);//first point of eLink
                aTime = timeBetween(eLink.getLink(), x1);

                map_time += aTime;

                final double real_time = timeBetween(x0, x1);
                final double lastNodeTime = x0.getEntry().getTime();
                double final_map_time = map_time;

                firstLink.exitTime = lastNodeTime + firstLink.exitTime*(real_time / final_map_time);

                currLinks.forEach(n -> n.scaleTimesBy(lastNodeTime, real_time / final_map_time));

                x0 = eLink.getGpxExtensions().get(eLink.getGpxExtensions().size() - 1); //last point of eLink

                aTime = timeBetween(x0, eLink.getLink());

                if (pathEdges.hasNext()) eLink.exitTime = aTime;
                else eLink.exitTime = x1.getEntry().getTime();

                map_time = aTime;
                firstLink = eLink;
                currLinks.clear();
            }
        }
        return resultLinks;
    }

    public List<Event> linkGPXToEvents(Iterator<LinkGPXStruct> x, Id<Person> personId, Id<Vehicle> vehicleId, String mode) {
        if (!x.hasNext()) return Collections.emptyList();
        List<Event> events = new ArrayList<>();
        LinkGPXStruct firstE = x.next();
        double entryTimeSeconds = toSeconds(firstE.entryTime);
        double exitTimeSeconds = toSeconds(firstE.exitTime);
        int numGpsPoints = firstE.getGpxExtensions().size();
        events.add(new PersonDepartureEvent(entryTimeSeconds, personId, firstE.getLink().getId(), mode));
        events.add(new GpsLinkLeaveEvent(exitTimeSeconds, vehicleId, firstE.getLink().getId(), numGpsPoints ));

        while (x.hasNext()) {
            LinkGPXStruct curr = x.next();
            double currEntryTimeSeconds = toSeconds(curr.entryTime);
            double currExitTimeSeconds = toSeconds(curr.exitTime);
            int currNumGpsPoints = curr.getGpxExtensions().size();

            if (x.hasNext()) {
                events.add(new LinkEnterEvent(currEntryTimeSeconds, vehicleId, curr.getLink().getId()));
                events.add(new GpsLinkLeaveEvent(currExitTimeSeconds, vehicleId, curr.getLink().getId(), currNumGpsPoints));
            } else { //process final element
                events.add(new LinkEnterEvent(currEntryTimeSeconds, vehicleId, curr.getLink().getId()));
                events.add(new PersonArrivalEvent(currExitTimeSeconds, personId, curr.getLink().getId(), mode));
            }
        }

        return events;
    }

    private double toSeconds(double time) {
        return time / 1000;
    }



    private Coordinate coordToCoordinate(Coord coord) {
        return new Coordinate(coord.getX(), coord.getY());
    }

    private double timeBetween(GPXExtension x0, GPXExtension x1) {
        return x1.getEntry().getTime() - x0.getEntry().getTime();
    }

    private double timeBetween(GPXExtension x0, Link l) {
        //get distance from x0 to e.

        Coordinate x0_coord = new Coordinate(x0.getEntry().getLon(), x0.getEntry().getLat()); //TODO lon lat order?

        Coordinate start_coord = coordToCoordinate(getCoordinateTransform().transform(l.getFromNode().getCoord()));
        Coordinate end_coord = coordToCoordinate(getCoordinateTransform().transform(l.getToNode().getCoord()));

        LengthIndexedLine ls = new LengthIndexedLine( new GeometryFactory().createLineString(new Coordinate[]{start_coord,end_coord}));
        double distance_covered = (ls.getEndIndex() - ls.project(x0_coord)) / ls.getEndIndex();
        double distance = distance_covered * l.getLength();
        double speed_e = l.getFreespeed();
        double time_e = distance / speed_e;

        return time_e;
    }

    private double timeBetween(Link l, GPXExtension x1) {
        //get distance from x0 to e.

        Coordinate x1_coord = new Coordinate(x1.getEntry().getLon(), x1.getEntry().getLat()); //TODO lon lat order?

        Coordinate start_coord = coordToCoordinate(getCoordinateTransform().transform(l.getFromNode().getCoord()));
        Coordinate end_coord = coordToCoordinate(getCoordinateTransform().transform(l.getToNode().getCoord()));

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
        return coordToCoordinate(getNetwork().getNodes().get(Id.createNodeId(node)).getCoord());
    }

    public MapMatching getMatcher() {
        return matcher;
    }


    public MapMatching getMapper() {
        return matcher;
    }

    public GraphHopper getHopper() {
        return hopper;
    }

    public CoordinateTransformation getCoordinateTransform() {
        return hopper.getCoordinateTransform();
    }

    public Network getNetwork() {
        return hopper.network;
    }


}
