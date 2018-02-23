package ethz.ivt.graphhopperMM;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters;
import ethz.ivt.graphhopperMM.gtfs.GraphHopperGtfsMATSim;
import ethz.ivt.graphhopperMM.gtfs.GtfsStorage;
import ethz.ivt.graphhopperMM.gtfs.PtFlagEncoder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static ethz.ivt.graphhopperMM.gtfs.GtfsHelper.time;
import static org.junit.Assert.assertEquals;

/**
 * Created by molloyj on 29.11.2017.
 */
public class gtfsPTtest {

    private static final String GRAPH_LOC = "target/GraphHopperGtfsMATSimIT";
    private static final String gtfsFilename = "C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\gtfs_20171211.zip";
    private static final String networkFilename = "src/test/resources/swiss_network.xml";
    private static GraphHopperGtfsMATSim graphHopper;
    private static final ZoneId zoneId = ZoneId.of("Europe/Paris");
    private static GraphHopperStorage graphHopperStorage;
    private static LocationIndex locationIndex;

    @BeforeClass
    public static void init() {
        Helper.removeDir(new File(GRAPH_LOC));
        final PtFlagEncoder ptFlagEncoder = new PtFlagEncoder();
        EncodingManager encodingManager = new EncodingManager(Arrays.asList(ptFlagEncoder), 8);
        GHDirectory directory = GraphHopperGtfsMATSim.createGHDirectory(GRAPH_LOC);
        GtfsStorage gtfsStorage = GraphHopperGtfsMATSim.createEmptyGtfsStorage();

        CoordinateTransformation coordinateTransformation = new CH1903LV03PlustoWGS84();

        graphHopperStorage = GraphHopperGtfsMATSim.createOrLoad(directory, encodingManager, ptFlagEncoder,
                gtfsStorage, Collections.singleton(gtfsFilename), networkFilename, coordinateTransformation);
        locationIndex = GraphHopperGtfsMATSim.createOrLoadIndex(directory, graphHopperStorage, ptFlagEncoder);
        graphHopper = new GraphHopperGtfsMATSim(ptFlagEncoder, graphHopperStorage, locationIndex, gtfsStorage);
    }

    @Test()
    public void gtfsMATSimNetworkIT() {
        final double FROM_LAT = 47.374436, FROM_LON = 8.495779; // NADAV stop
        final double TO_LAT = 47.378272, TO_LON = 8.538780; // NANAA stop
        GHRequest ghRequest = new GHRequest(
                FROM_LAT, FROM_LON,
                TO_LAT, TO_LON
        );
        ghRequest.getHints().put(Parameters.PT.EARLIEST_DEPARTURE_TIME, LocalDateTime.of(2007,1,1,7,44).atZone(zoneId).toInstant());
        ghRequest.getHints().put(Parameters.PT.IGNORE_TRANSFERS, true);

        GHResponse response = graphHopper.route(ghRequest);

        assertEquals(1, response.getAll().size());
        assertEquals("Expected travel time == scheduled arrival time", time(0, 5), response.getBest().getTime(), 0.1);



    }
}
