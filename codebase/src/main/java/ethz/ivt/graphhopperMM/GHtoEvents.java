package ethz.ivt.graphhopperMM;

import com.graphhopper.GraphHopper;
import com.graphhopper.MapMatchingUnlimited;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.linearref.LengthIndexedLine;
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

    private final MapMatchingUnlimited matcher;
    private GraphHopperMATSim hopper;
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public GHtoEvents(GraphHopperMATSim hopper, MapMatchingUnlimited matcher) {
        this.matcher = matcher;
        this.hopper = hopper;
    }

    public Id<Link> getNearestLinkId(GPXEntry point) {

        EdgeFilter ef = EdgeFilter.ALL_EDGES;
        QueryResult qr = hopper.getLocationIndex().findClosest(point.lat, point.lon, ef);
        try {
            return Id.createLinkId(qr.getClosestEdge().getName());
        } catch (NullPointerException ex) {
            logger.warning("Couldn't match point " + point + "to network");
            return null;
        }
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

    public List<Event> gpsToEvents(List<GPXEntry> entries, Id<Vehicle> vehicleId) {
        return linkGPXToEvents(mapMatchWithTravelTimes(entries).iterator(), vehicleId);
    }


    public List<LinkGPXStruct> mapMatchWithTravelTimes(List<GPXEntry> entries) {
        int numCandidates = Integer.MAX_VALUE;
        if (entries.size() < 2) return new ArrayList<>();
        if (entries.size() == 2) numCandidates = 1;

        try {
            MatchResult mr = getMatcher().doWork(entries, numCandidates);
            if (mr.getEdgeMatches().isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            List<LinkGPXStruct> timed_links =  calculateNodeVisitTimes(mr.getEdgeMatches());
            assert(timed_links.stream().allMatch(l -> l.exitTime > l.entryTime));
            return timed_links;
        } catch (IllegalArgumentException ex) {
            logger.warning(ex.getMessage());
            logger.warning(ex.toString());
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
        GPXExtension path_x0 = eLink.getGpxExtensions().get(0);//get node from eLink
        GPXExtension path_x1 = null;
        //can assume that first edge has a point. add end(eLink) to n_list, time(x, end(eLink)) to t_list
        double unmeasuredTime = timeBetween(eLink.getLink(), path_x0);
        double travelTime = timeBetween(path_x0, eLink.getLink());

        double firstLinkBeforeGPStime = unmeasuredTime;
        double firstLinkAfterGPStime = travelTime;

        //set entry time of first link
        eLink.entryTime = path_x0.getEntry().getTime() - unmeasuredTime;

        double real_time_start = eLink.entryTime;
        double network_time = timeBetween(eLink.getLink());

        resultLinks.add(eLink);

        LinkGPXStruct firstLink = eLink;

        if (!pathEdges.hasNext()) {
            eLink.exitTime =  eLink.entryTime + travelTime;
        }

        while (pathEdges.hasNext()) {
            LinkGPXStruct prevE = eLink;
            edgeMatch = pathEdges.next();
            eLink = convertToLinkStruct(edgeMatch);
            eLink.entryTime = prevE.exitTime;
            resultLinks.add(eLink);

            if(eLink.isEmpty()) {
                currLinks.add(eLink);

                //we will need to impute the travel time --cannot be the last link
                if (!pathEdges.hasNext()) {
                    throw new RuntimeException("last link in a path needs to have a gps point");
                }
                travelTime = timeBetween(eLink.getLink());
                network_time += travelTime;

                eLink.exitTime = eLink.entryTime + travelTime;
            } else { //finish off this section of road!
                path_x1 = eLink.getGpxExtensions().get(eLink.getGpxExtensions().size() - 1);//first point of eLink

                travelTime = timeBetween(eLink.getLink(), path_x1);
                unmeasuredTime = timeBetween(path_x1, eLink.getLink());

                network_time += travelTime;

                double real_time_end = path_x1.getEntry().getTime();
                final double real_travel_time = (real_time_end -  real_time_start) /1000;

                final double beginNodeTime = path_x0.getEntry().getTime();


                final double scaling_factor = real_travel_time / network_time;

                eLink.entryTime = beginNodeTime + eLink.entryTime * scaling_factor;

                currLinks.forEach(n -> n.scaleTimesBy(beginNodeTime, scaling_factor));

                if (currLinks.isEmpty()) {
                    firstLink.exitTime = eLink.entryTime;
                } else {
                    firstLink.exitTime = currLinks.get(0).entryTime;
                }
                currLinks.clear();

                path_x0 = eLink.getGpxExtensions().get(eLink.getGpxExtensions().size() - 1); //last point of eLink
                path_x1 = null;

                real_time_start = real_time_end;
                network_time = 0 + travelTime;

                firstLink = eLink;
                firstLinkBeforeGPStime = unmeasuredTime;
                firstLinkAfterGPStime  = travelTime;

            }
        }
        return resultLinks;
    }

    public List<Event> linkGPXToEvents(Iterator<LinkGPXStruct> x, Id<Vehicle> vehicleId) {
        if (!x.hasNext()) return Collections.emptyList();
        List<Event> events = new ArrayList<>();
        LinkGPXStruct firstE = x.next();
        double entryTimeSeconds = toSeconds(firstE.entryTime);
        double exitTimeSeconds = toSeconds(firstE.exitTime);

        events.add(new LinkLeaveEvent(exitTimeSeconds, vehicleId, firstE.getLink().getId() ));

        while (x.hasNext()) {
            LinkGPXStruct curr = x.next();
            double currEntryTimeSeconds = toSeconds(curr.entryTime);
            double currExitTimeSeconds = toSeconds(curr.exitTime);

            if (x.hasNext()) {
                events.add(new LinkEnterEvent(currEntryTimeSeconds, vehicleId, curr.getLink().getId()));
                events.add(new LinkLeaveEvent(currExitTimeSeconds, vehicleId, curr.getLink().getId()));
            } else { //process final element
                events.add(new LinkEnterEvent(currEntryTimeSeconds, vehicleId, curr.getLink().getId()));

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

    public MapMatchingUnlimited getMatcher() {
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
