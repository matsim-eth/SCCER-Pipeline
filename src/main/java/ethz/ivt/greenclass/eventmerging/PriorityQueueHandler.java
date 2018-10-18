package ethz.ivt.greenclass.eventmerging;

import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

public class PriorityQueueHandler implements
        LinkEnterEventHandler,
        LinkLeaveEventHandler,
        PersonDepartureEventHandler,
        PersonArrivalEventHandler {


    private final String eventFileName;
    private OutputMonitor outputMonitor;

    public PriorityQueueHandler(OutputMonitor outputMonitor, String f) {
        this.outputMonitor = outputMonitor;
        this.eventFileName = f;
        this.outputMonitor.registerFile(f);
    }

    private void handleAllEvents(Event event) {
        outputMonitor.addToQueue(eventFileName, event);
        //TODO: check if files should be written out.
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        handleAllEvents(event);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        handleAllEvents(event);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        handleAllEvents(event);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        handleAllEvents(event);
    }
}
