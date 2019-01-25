package ethz.ivt.graphhopperMM;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.GPXEntry;
import greenclass.WaypointRecord;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestBadTrace {
    static GHtoEvents gHtoEvents;

    public WaypointRecord mapToWaypointRecord(String csvLine) {
        //tl_id,longitude,latitude,time
        return WaypointRecord.parseFromCSV(csvLine, ',');
    }

    @BeforeClass
    public static void setUpTestNetwork() {

        String networkFilename = "src/test/resources/switzerland_network.xml.gz";
        gHtoEvents = new MATSimMMBuilder().buildGhToEvents(networkFilename, new CH1903LV03PlustoWGS84());

    }
    @Test
    public void poorTrace1() throws IOException {
        String badTraceFilename = "src/test/resources/1720-gpx.csv";
        double expected = 54814;
        poorTrace(badTraceFilename, 2390383, expected);


    }

    @Test
    public void InternationalPointPoorTrace2() throws IOException {
        String badTraceFilename = "src/test/resources/1670-gpx.csv";
        double expected = 3700;
        poorTrace(badTraceFilename, 3476563, expected);


    }


    public List<LinkGPXStruct> poorTrace(String trace_filename, long tl_id, double expected) throws IOException {


        List<GPXEntry> inputList;

        try {
            File inputF = new File(trace_filename);
            InputStream inputFS = new FileInputStream(inputF);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));
            // skip the header of the csv
            List<WaypointRecord> inputListWP = br.lines().skip(1)
                    .map(this::mapToWaypointRecord)
                    .collect(Collectors.toList());

            inputList = inputListWP.stream()
                    .filter(wp -> wp.accuracy() < 200)
                    .map(WaypointRecord::toGPX)
                    .collect(Collectors.toList());
            br.close();
        } catch (IOException e) {
            throw e;
        }
        long start_time = System.currentTimeMillis();
        List<LinkGPXStruct> links = gHtoEvents.mapMatchWithTravelTimes(inputList);
        long end_time = System.currentTimeMillis();
        double length = links.stream().mapToDouble(l -> l.getLink().getLength()).sum();
        System.out.println("distance found was " + length + " in time " + (end_time-start_time)/1000 + " seconds");
        assertTrue(String.format("distance %.2f in reasonable range of %.2f", length == expected), length < 54814 * 1.5);
        return links;
        //    Coord test 8.09821439432	47.274493016  ->  x="2682108.0" y="1226718.0"
    }

    //locationIndex.findNClosest( 7.99485276271, 47.4659583026, edgeFilter, 20000.0)
}

