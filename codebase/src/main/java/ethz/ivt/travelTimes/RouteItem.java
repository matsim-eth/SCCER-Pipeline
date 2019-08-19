package ethz.ivt.travelTimes;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

import java.util.Collection;

public class RouteItem {
    double departureTime;
    Coord startCoord;
    Coord endCoord;
    double travelTime;
    Collection<Link> links;

    public RouteItem(double departureTime, Coord startCoord, Coord endCoord, double travelTime, Collection<Link> links) {
        this.departureTime = departureTime;
        this.startCoord = startCoord;
        this.endCoord = endCoord;
        this.travelTime = travelTime;
        this.links = links;
    }

}
