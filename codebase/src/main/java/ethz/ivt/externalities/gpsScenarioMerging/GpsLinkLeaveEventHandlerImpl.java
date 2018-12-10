package ethz.ivt.externalities.gpsScenarioMerging;

import ethz.ivt.graphhopperMM.GpsLinkLeaveEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

public class GpsLinkLeaveEventHandlerImpl implements GenericEventHandler, GpsLinkLeaveEventHandler {
    EventsManager eventsManager;

    public GpsLinkLeaveEventHandlerImpl(EventsManager eventsManager) {
        this.eventsManager = eventsManager;
    }

    //since the LinkLeaveEvent was extended to include the number of gps points, it is read as a generic event.
    //hence this handler reads the generic event, and rethrows the GpsLinkLeaveEvent, which is then processed by the
    //eventsManager, which also throws a normal linkleave event, to stop any other functionality breaking.
    //This is really just useful for getting the number of gps points per link for the LinkSpeedAnalysis script
    @Override
    public void handleEvent(GenericEvent event) {
        if (GpsLinkLeaveEvent.EVENT_TYPE.equals(event.getEventType())) {
            GpsLinkLeaveEvent glle = new GpsLinkLeaveEvent(
                    event.getTime(),
                    Id.createVehicleId(event.getAttributes().get(LinkLeaveEvent.ATTRIBUTE_VEHICLE)),
                    Id.createLinkId(event.getAttributes().get(LinkLeaveEvent.ATTRIBUTE_LINK)),
                    Integer.parseInt(event.getAttributes().get(GpsLinkLeaveEvent.ATTRIBUTE_GPS_COUNT))
            );
            eventsManager.processEvent(glle);
        }
    }

    @Override
    public void handleEvent(GpsLinkLeaveEvent event) {
        eventsManager.processEvent(event.getNormalLinkLeaveEvent());
    }
}


