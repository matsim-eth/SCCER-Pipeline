package ethz.ivt.externalities.gpsScenarioMerging;

import ethz.ivt.graphhopperMM.GpsLinkLeaveEvent;
import org.matsim.core.events.handler.EventHandler;

public interface GpsLinkLeaveEventHandler extends EventHandler {
    public void handleEvent(GpsLinkLeaveEvent event);
}
