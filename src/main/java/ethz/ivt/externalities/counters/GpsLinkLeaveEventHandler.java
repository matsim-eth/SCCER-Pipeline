package ethz.ivt.externalities.counters;

import ethz.ivt.graphhopperMM.GpsLinkLeaveEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.core.events.handler.EventHandler;

public interface GpsLinkLeaveEventHandler extends EventHandler {
    public void handleEvent(GpsLinkLeaveEvent event);
}