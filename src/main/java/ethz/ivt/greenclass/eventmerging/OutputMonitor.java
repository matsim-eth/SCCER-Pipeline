package ethz.ivt.greenclass.eventmerging;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class OutputMonitor {
    private static final int BUFFER_SIZE = 1000;

    private ConcurrentHashMap<String, Double> fileRegister = new ConcurrentHashMap<>();
    private PriorityBlockingQueue<Event> eventQueue;
    private EventWriterXML writerXML;

    public OutputMonitor(PriorityBlockingQueue<Event> eventQueue, String filename) {
        this.eventQueue = eventQueue;
        this.writerXML = new EventWriterXML(filename);
    }

    public synchronized void registerFile(String f) {
        fileRegister.put(f, 0.0);
    }

    public synchronized void addToQueue(String f, Event event) {
        eventQueue.add(event);
        fileRegister.put(f, event.getTime());

        if (eventQueue.size() % BUFFER_SIZE == 0) {
            this.flushBuffer(false);
        }
    }

    public synchronized void flushBuffer(boolean flushEverything) {
        //write stuff out here, when appropriate
        OptionalDouble minTime = flushEverything ?
                OptionalDouble.of(Double.MAX_VALUE) :
                fileRegister.values().stream().mapToDouble(Double::doubleValue).min();

        minTime.ifPresent(minVal -> {
            //write out all eventse until the minValue
            while (!eventQueue.isEmpty() && eventQueue.peek().getTime() < minVal) {
                writerXML.handleEvent(eventQueue.poll());
            }
        });
    }

    public void close() {
        flushBuffer(true);
        writerXML.closeFile();
    }

}
