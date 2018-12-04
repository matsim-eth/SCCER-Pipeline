package ethz.ivt.graphhopperMM;

import ethz.ivt.externalities.counters.GpsLinkLeaveEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public class GpsLinkLeaveEvent extends Event {
    public static final String EVENT_TYPE = "left link gps";
    public static final String ATTRIBUTE_GPS_COUNT = "gps_points";

    private final LinkLeaveEvent linkLeaveEvent;
    private final int gpsPointCount;

    public GpsLinkLeaveEvent(double time, Id<Vehicle> vehicleId, Id<Link> linkId, int gpsPointCount) {
        super(time);
        linkLeaveEvent = new LinkLeaveEvent(time, vehicleId, linkId);
        this.gpsPointCount = gpsPointCount;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_GPS_COUNT, Integer.toString(this.gpsPointCount));
        attr.putAll(linkLeaveEvent.getAttributes());
        return attr;
    }

    public LinkLeaveEvent getNormalLinkLeaveEvent() {
        return linkLeaveEvent;
    }

    public int getNumGpsPoints() {
        return gpsPointCount;
    }



    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public Id<Link> getLinkId() {
        return linkLeaveEvent.getLinkId();
    }
}

