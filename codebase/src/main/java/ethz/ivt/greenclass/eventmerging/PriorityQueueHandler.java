package ethz.ivt.greenclass.eventmerging;

import org.matsim.api.core.v01.Id;
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
    private final String prefix;
    private OutputMonitor outputMonitor;

    public PriorityQueueHandler(OutputMonitor outputMonitor, String f, String prefix) {
        this.outputMonitor = outputMonitor;
        this.eventFileName = f;
        this.outputMonitor.registerFile(f);
        this.prefix = prefix;
    }

    private void handleAllEvents(Event event) {

        outputMonitor.addToQueue(eventFileName, event);
        //TODO: check if files should be written out.
    }

    @Override

    public void handleEvent(LinkEnterEvent event) {
        //add prefix to event
        LinkEnterEvent event1 = new LinkEnterEvent(
                event.getTime(),
                Id.createVehicleId(getGPSPrefix() + event.getVehicleId()),
                event.getLinkId());
        handleAllEvents(event1);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        LinkLeaveEvent event1 = new LinkLeaveEvent(
                event.getTime(),
                Id.createVehicleId(getGPSPrefix() + event.getVehicleId()),
                event.getLinkId());
        handleAllEvents(event1);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {

        PersonArrivalEvent event1 = new PersonArrivalEvent(
                event.getTime(),
                Id.createPersonId(getGPSPrefix() + event.getPersonId()),
                event.getLinkId(),
                event.getLegMode());
        handleAllEvents(event1);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        PersonDepartureEvent event1 = new PersonDepartureEvent(
                event.getTime(),
                Id.createPersonId(getGPSPrefix() + event.getPersonId()),
                event.getLinkId(),
                event.getLegMode());
        handleAllEvents(event1);
    }

    public String getGPSPrefix() {
        return prefix;
    }
}
