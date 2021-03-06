package ethz.ivt.graphhopperMM;

import com.graphhopper.matching.GPXExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import java.util.List;

/**
 * Created by molloyj on 07.11.2017.
 */
public class LinkGPXStruct {
    private final List<GPXExtension> gpxExtensions;
    private final Link link;
    public double entryTime;
    public double exitTime;

    public LinkGPXStruct(Link link, List<GPXExtension> gpxExtensions) {
        this.link = link;
        this.gpxExtensions = gpxExtensions;
    }

    public List<GPXExtension> getGpxExtensions() {
        return gpxExtensions;
    }

    public Node getToNode() {
        return link.getToNode();
    }
    public Node getFromNode() {
        return link.getFromNode();
    }

    public boolean isEmpty() {
        return gpxExtensions.isEmpty();
    }

    public Link getLink() {
        return link;
    }

    public void scaleTimesBy(double lastNodeTime, double v) {
        entryTime = lastNodeTime + entryTime*v;
        exitTime = lastNodeTime + exitTime*v;
    }

    @Override
    public String toString() {
        return "{" +
                "gpx=" + gpxExtensions.size() +
                ", link=" + link.getId() +
                ", entryT=" + entryTime +
                ", exitT=" + exitTime +
                '}';
    }
}
