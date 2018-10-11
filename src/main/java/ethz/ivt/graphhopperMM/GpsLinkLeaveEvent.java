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

public class GpsLinkLeaveEvent extends LinkLeaveEvent {
    public static final String EVENT_TYPE = "left link gps";
    public static final String ATTRIBUTE_GPS_COUNT = "gps_points";
    private final int gpsPointCount;

    public GpsLinkLeaveEvent(double time, Id<Vehicle> vehicleId, Id<Link> linkId, int gpsPointCount) {
        super(time, vehicleId, linkId);
        this.gpsPointCount = gpsPointCount;
        super.getAttributes().put(Event.ATTRIBUTE_TYPE, EVENT_TYPE);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attr = super.getAttributes();
        attr.put(ATTRIBUTE_GPS_COUNT, Integer.toString(this.gpsPointCount));
        return attr;
    }

    public LinkLeaveEvent getNormalLinkLeaveEvent() {
        return this;
    }

    public int getNumGpsPoints() {
        return gpsPointCount;
    }



    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

}

