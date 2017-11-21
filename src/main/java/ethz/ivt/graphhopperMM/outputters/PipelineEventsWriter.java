package ethz.ivt.graphhopperMM.outputters;

import ethz.ivt.graphhopperMM.GHtoEvents;
import ethz.ivt.graphhopperMM.GPXEntryExt;
import ethz.ivt.graphhopperMM.LinkGPXStruct;
import ethz.ivt.graphhopperMM.PipelineStage2;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.collections.Tuple;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Created by molloyj on 21.11.2017.
 */
public class PipelineEventsWriter extends PipelineWriter<Event> {

    private static Logger logger = Logger.getLogger(PipelineEventsWriter.class);

    public PipelineEventsWriter(String outputFilename, GHtoEvents gHtoEvents) {
        super(outputFilename, gHtoEvents);

    }

    @Override
    public void write(Map<Id<Person>, Map<Tuple<Integer, String>, List<GPXEntryExt>>> processedGPXbyPerson) {
        EventWriterXML eventWriter = new EventWriterXML(outputFilename);

        List<Event> events = convertToEvents(mapMatch(processedGPXbyPerson));
        events.forEach(eventWriter::handleEvent);
        eventWriter.closeFile();
    }

    public Map<Id<Person>, Map<Tuple<Integer, String>, List<LinkGPXStruct>>> mapMatch(
            Map<Id<Person>, Map<Tuple<Integer, String>, List<GPXEntryExt>>> personGPX
    ) {
        //mapmatch traces
        Map<Id<Person>, Map<Tuple<Integer, String>, List<LinkGPXStruct>>> personLinks = personGPX.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                e1 -> e1.getKey(),
                                e1 -> mapMatch(e1.getValue()))
                        )
                ));
        return personLinks;
    }


    private List<LinkGPXStruct> mapMatch(List<GPXEntryExt> entries) {
        List<LinkGPXStruct> events = getGhToEvents().mapMatchWithTravelTimes(entries.stream()
                .map(GPXEntryExt::getGpxEntry)
                .collect(toList())
        );
        return events;

    }

    private List<Event> convertToEvents(Map<Id<Person>, Map<Tuple<Integer, String>, List<LinkGPXStruct>>> gpxStructs) {
        List<Event> personEvents = gpxStructs.entrySet().stream().flatMap(e -> {
            Id<Person> pid = e.getKey();
            return e.getValue().entrySet().stream().flatMap(e1 -> {
                int leg = e1.getKey().getFirst();
                String mode = e1.getKey().getSecond();
                return getGhToEvents().linkGPXToEvents(e1.getValue().iterator(), pid, null, mode).stream();
            });
        }).sorted(Comparator.comparingDouble(Event::getTime))
                .collect(Collectors.toList());

        logger.info(personEvents.size());

        return personEvents;
    }
}
