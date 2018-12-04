package ethz.ivt.graphhopperMM.outputters;

import ethz.ivt.graphhopperMM.GHtoEvents;
import ethz.ivt.graphhopperMM.GPXEntryExt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by molloyj on 21.11.2017.
 */
public class LinkIdWriter extends PipelineWriter<Link> {

    public LinkIdWriter(String outputFilename, GHtoEvents gHtoEvents) {
        super(outputFilename, gHtoEvents);
    }

    public void write(Map<Id<Person>, Map<Tuple<Integer, String>, List<GPXEntryExt>>> processedGPXbyPerson) {

        processedGPXbyPerson.entrySet().forEach(e -> {
            Id<Person> pid = e.getKey();
            e.getValue().entrySet().forEach(e1 -> {
                int leg = e1.getKey().getFirst();
                String mode = e1.getKey().getSecond();
                List<Link> mapLinks = mapMatchWOTT(e1.getValue());
                mapLinks.forEach(x -> writeLine(pid, leg, mode, x));
            });


        });
    }

    private List<Link> mapMatchWOTT(List<GPXEntryExt> points) {
        return getGhToEvents().networkRouteWithoutTravelTimes(points.stream().map(GPXEntryExt::getGpxEntry).collect(Collectors.toList()));
    }

    private void writeLine(Id<Person> pid, int leg, String mode, Link link) {
        throw new RuntimeException(new NoSuchMethodException("writeLine for links is not yet implemented"));
    }
}
