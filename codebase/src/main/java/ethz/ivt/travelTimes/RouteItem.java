package ethz.ivt.travelTimes;

import org.matsim.api.core.v01.Coord;

import java.util.Collection;
import java.util.List;

public class RouteItem {
    double departureTime;
    Coord startCoord;
    Coord endCoord;
    double travelTime;
    Collection<String> links;

    public RouteItem(double departureTime, Coord startCoord, Coord endCoord, double travelTime, Collection<String> links) {
        this.departureTime = departureTime;
        this.startCoord = startCoord;
        this.endCoord = endCoord;
        this.travelTime = travelTime;
        this.links = links;
    }

}
