package ethz.ivt.externalities.counters;

import org.matsim.core.events.handler.EventHandler;

public interface ExtendedPersonDepartureEventEventHandler extends EventHandler {
    public void handleEvent (ExtendedPersonDepartureEvent event);
}
