package ethz.ivt.graphhopperMM;

import com.graphhopper.util.GPXEntry;
import ethz.ivt.graphhopperMM.outputters.LinkIdWriter;
import ethz.ivt.graphhopperMM.outputters.PipelineEventsWriter;
import ethz.ivt.graphhopperMM.outputters.PipelineWriter;
import org.apache.commons.cli.CommandLineParser;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Created by molloyj on 20.11.2017.
 */
public class PipelineStage2 {

    private static Logger logger = Logger.getLogger(PipelineStage2.class);

    private static final String CSV_DELIM = ",";
    private static final int DATE_COL = 0;
    private static final int LEG_COL = 1;
    private static final int MODE_COL = 2;
    private static final int LAT_COL = 3;
    private static final int LON_COL = 4;
    private static final int TIME_COL = 5;
    private final GHtoEvents gHtoEvents;

    public PipelineStage2(GHtoEvents gHtoEvents) {
        this.gHtoEvents = gHtoEvents;
    }


    //input is a folder or directory
    //files are in the format date_stage_person_id

    public static void main(String[] args) throws IOException {
        //arg 1 = network
        //arg 2 = gpx folder
        //arg 3 = output format

        if (args.length != 4){
            throw new IllegalArgumentException("there must be four arguments, in the following structure:....");
        }
        String networkFileName = args[0];
        String gpxFoldername = args[1];
        String outputFormat = args[2];
        String outputFilename = args[3];


        GHtoEvents gHtoEvents = new MATSimMMBuilder().buildGhToEvents(networkFileName, new CH1903LV03PlustoWGS84());
        PipelineStage2 pipelineStage2 = new PipelineStage2(gHtoEvents);

        //load GPS traces from folder
        Map<Id<Person>, Map<Tuple<Integer, String>, List<GPXEntryExt>>> personTraces = pipelineStage2.processDirectory(new File(gpxFoldername));

        //TODO: at the moment the writer does the conversion from points to the required format at well. this should be extracted
        // to make it easier to follow, and for better modularity.
        PipelineWriter outputter = null;
        if (outputFormat.equals("-asEvents")) {
            outputter = new PipelineEventsWriter(outputFilename, gHtoEvents);

        } else if (outputFormat.equals("-asLinkIds")) {
            outputter = new LinkIdWriter(outputFilename, gHtoEvents);
        } else {
            throw new IllegalArgumentException("Invalid option for output type");
        }
        outputter.write(personTraces);


        //convert to events


    }


    private List<Link> mapMatchWithoutTT(List<GPXEntryExt> entries) {
        List<Link> events = gHtoEvents.networkRouteWithoutTravelTimes(entries.stream()
                .map(GPXEntryExt::getGpxEntry)
                .collect(toList())
        );
        return events;

    }
    private Map<Id<Person>, Map<Tuple<Integer, String>, List<GPXEntryExt>>> processDirectory(File gpxFolder) throws IOException {
        Map<Id<Person>, Map<Tuple<Integer, String>, List<GPXEntryExt>>> entries = new HashMap<>();
        if (gpxFolder.isDirectory()) {
            for (File gpxFile : gpxFolder.listFiles()) {
                if (gpxFile.isFile()) {
                    String[] components = gpxFile.getName().split("_");
                    String date = components[0];
                    Id<Person> personId = Id.createPersonId(components[1]);

                    Map<Tuple<Integer, String>, List<GPXEntryExt>> legs = Files
                            .lines(gpxFile.toPath())
                            .map(this::processLine)
                            .collect(Collectors.groupingBy(GPXEntryExt::getLegModeTuple,
                                    Collectors.mapping(Function.identity(),
                                            Collectors.toList())));

                    assert !entries.containsKey(personId) : "Two files for the same person in directory";
                    entries.put(personId, legs);
                }
            }
        }
        return entries;


    }


    private GPXEntryExt processLine (String s) {
        String[] fields = s.split(CSV_DELIM);
        String date = fields[DATE_COL];
        int leg = Integer.parseInt(fields[LEG_COL]);
        String mode = fields[MODE_COL];
        GPXEntryExt entry = new GPXEntryExt(
                leg,
                mode,
                Double.parseDouble(fields[LAT_COL]),
                Double.parseDouble(fields[LON_COL]),
                Long.parseLong(fields[TIME_COL])
        );
        return entry;
    }

}
