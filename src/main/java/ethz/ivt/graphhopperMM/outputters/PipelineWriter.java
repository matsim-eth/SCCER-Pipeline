package ethz.ivt.graphhopperMM.outputters;

import ethz.ivt.graphhopperMM.GHtoEvents;
import ethz.ivt.graphhopperMM.GPXEntryExt;
import ethz.ivt.graphhopperMM.LinkGPXStruct;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.List;
import java.util.Map;

/**
 * Created by molloyj on 21.11.2017.
 */
public abstract class PipelineWriter<O> {

    private final GHtoEvents ghToEvents;
    String outputFilename;

    public PipelineWriter(String outputFilename, GHtoEvents gHtoEvents) {
        this.outputFilename = outputFilename;
        this.ghToEvents = gHtoEvents;
    }

    public abstract void write(Map<Id<Person>, Map<Tuple<Integer,String>,List<GPXEntryExt>>> processedGPXbyPerson);

    public GHtoEvents getGhToEvents() {
        return ghToEvents;
    }
}
