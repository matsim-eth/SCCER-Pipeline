package ethz.ivt.greenclass.eventmerging;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class EventMerger {
    private static OutputMonitor outputMonitor;
    private static String prefix;
    //   EventsManagerImpl eml;
//    MatsimEventsReader

 //   Collection<>


    //have a list of events for each file, and record the last time.
    //have a timer running,

    //using futures to write out events.
    //if event time is less than

    //have a priority queue by time, add elements to the queue, only write/pop a certain number of elements when all
    //files are up to that time.

    private static Stream<Path> getPathsFromWalk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Stream.empty();
    }

    public static void main(String[] args) throws IOException {

        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            System.out.println("usage: program [-p prefix] inputfile1 inputfile2 ..... inputfile_n outputFile");
            System.out.println("\tInput files can be single event files or folders of event files.");
            System.out.println("\tThe command -p will add a prefix to all event names except those in the first file.");
            System.out.println("\tCompressed (.gz) files are supported.");
        }

        int startIndexFileLocations = 0;

        if (args[0].equals("-p")) {
            prefix = args[1];
            startIndexFileLocations = 2;
        } else {
            prefix = "";
        }

        String[] eventFileLocations = Arrays.copyOfRange(args, startIndexFileLocations, args.length-1);
        String outputFilename = args[args.length-1]; //last arg is output
        //String outputFilename = "C:\\Projects\\SCCER_project\\output_gc\\combined_events.xml";
        //Stream<Path> scenarioEvents = Stream.of(Paths.get("C:\\Projects\\SCCER_project\\scenarios\\zurich_1pct\\scenario\\800.events.xml.gz"));

        //check files exist
        Arrays.stream(eventFileLocations).forEach( p -> {
            if (Files.notExists(Paths.get(p))) {
                throw new RuntimeException(new FileNotFoundException(p));
            }
        });

  //      if (Files.notExists(Paths.get(outputFilename))) {
  //          throw new RuntimeException(new FileNotFoundException(outputFilename));
  //      }

        //Build output monitor

        Comparator<Event> eventTimeComparator = Comparator.comparingDouble(Event::getTime);
        PriorityBlockingQueue<Event> eventQueue = new PriorityBlockingQueue<>(10000, eventTimeComparator);
        outputMonitor = new OutputMonitor(eventQueue, outputFilename);

        //filter only event xml files

        Stream<Path> eventFiles = Arrays.stream(eventFileLocations).map(Paths::get).flatMap(EventMerger::getPathsFromWalk)
        //Stream<Path> eventFiles = Files.walk(Paths.get("C:\\Projects\\SCCER_project\\output_gc\\events\\"))
                .filter(p ->
                            Files.isRegularFile(p) && (
                            p.toString().endsWith(".xml") ||
                            p.toString().endsWith(".xml.gz")
                            )
                        );


        eventFiles.findFirst().ifPresent(path -> processEvents(path, ""));
        
        //process files in parallel
        eventFiles.skip(1).parallel().forEach(path -> processEvents(path, prefix));

        outputMonitor.close();

        System.out.println(eventQueue.size());
        System.out.println(eventQueue.size() == 0);

    }

    private static void processEvents(Path path, String prefix) {
        String f = path.toString();
        System.out.println(f);
        EventsManager eventsManager = new EventsManagerImpl();
        EventHandler priorityQueueHandler = new PriorityQueueHandler(outputMonitor, f, prefix);
        eventsManager.addHandler(priorityQueueHandler);

        new MatsimEventsReader(eventsManager).readFile(f);
    }

}
