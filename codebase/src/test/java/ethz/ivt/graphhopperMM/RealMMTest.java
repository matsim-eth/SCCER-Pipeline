package ethz.ivt.graphhopperMM;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.GPXEntry;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by molloyj on 14.11.2017.
 */
public class RealMMTest {

    @Test
    public void testTrimMatches() {
        String networkFilename = "src/test/resources/swiss_network.xml";
        GHtoEvents gHtoEvents = new GHtoEvents(null, null);

        List<GPXExtension> noGPSPoints = new ArrayList<>();
        EdgeMatch emptyEdge1 = new EdgeMatch(new MockEdgeIterator("empty1"), noGPSPoints);
        EdgeMatch emptyEdge2 = new EdgeMatch(new MockEdgeIterator("empty2"), noGPSPoints);
        EdgeMatch emptyEdge3 = new EdgeMatch(new MockEdgeIterator("empty3"), noGPSPoints);
        EdgeMatch emptyEdge4 = new EdgeMatch(new MockEdgeIterator("empty4"), noGPSPoints);
        EdgeMatch emptyEdge5 = new EdgeMatch(new MockEdgeIterator("empty5"), noGPSPoints);

        List<GPXExtension> someGPSPoints = new ArrayList<>();
        someGPSPoints.add(new GPXExtension(null, null));
        someGPSPoints.add(new GPXExtension(null, null));
        EdgeMatch edgeWithPoints1 = new EdgeMatch(new MockEdgeIterator("points1"), someGPSPoints);
        EdgeMatch edgeWithPoints2 = new EdgeMatch(new MockEdgeIterator("points2"), someGPSPoints);
        EdgeMatch edgeWithPoints3 = new EdgeMatch(new MockEdgeIterator("points3"), someGPSPoints);

        ArrayList<EdgeMatch> edgeMatches;

        //test 1 - good edges
        edgeMatches = new ArrayList<>();
        edgeMatches.add(edgeWithPoints1);
        edgeMatches.add(edgeWithPoints2);
        edgeMatches.add(edgeWithPoints3);

        List<EdgeMatch> res = gHtoEvents.trim_links(edgeMatches);
        assertEquals("All edges have matches and should not be removed", 3, res.size());

        //test 2 - trim from start
        edgeMatches = new ArrayList<>();
        edgeMatches.add(emptyEdge1);
        edgeMatches.add(edgeWithPoints1);
        edgeMatches.add(edgeWithPoints2);

        res = gHtoEvents.trim_links(edgeMatches);
        assertEquals("first edge should be removed", 2, res.size());

        //test 3 - trim from end
        edgeMatches = new ArrayList<>();
        edgeMatches.add(edgeWithPoints1);
        edgeMatches.add(edgeWithPoints2);
        edgeMatches.add(emptyEdge1);

        res = gHtoEvents.trim_links(edgeMatches);
        assertEquals("last edge should be removed", 2, res.size());

        //test 3 - dont trim from middle
        edgeMatches = new ArrayList<>();
        edgeMatches.add(edgeWithPoints1);
        edgeMatches.add(emptyEdge1);
        edgeMatches.add(edgeWithPoints2);

        res = gHtoEvents.trim_links(edgeMatches);
        assertEquals("middle edge should not be removed", 3, res.size());

        //test 3 - remove multiple edges from both sides
        edgeMatches = new ArrayList<>();
        edgeMatches.add(emptyEdge1);
        edgeMatches.add(emptyEdge2);
        edgeMatches.add(edgeWithPoints1);
        edgeMatches.add(emptyEdge3);
        edgeMatches.add(edgeWithPoints2);
        edgeMatches.add(emptyEdge4);
        edgeMatches.add(emptyEdge5);

        res = gHtoEvents.trim_links(edgeMatches);
        assertEquals("middle edge should not be removed", 3, res.size());
    }

    @Test
    public void noWaypointsCH() {
        GPXEntry start = new GPXEntry(47.44724, 8.16896, 0);
        GPXEntry end = new GPXEntry(47.44229, 8.16805, 0);

        GPXEntry end1 = new GPXEntry(47.1927405, 8.7224103, (2*60+1)*1000);
        GPXEntry start1 = new GPXEntry(47.2000532, 8.7955055, 0);

        Coord ch1 = new Coord( 2655083.0, 1255417.0);
        Coord ch = new WGS84toCH1903LV03Plus().transform(new Coord(start.getLon(), start.getLat()));
        assertEquals(ch1, ch);

        String networkFilename = "src/test/resources/swiss_network.xml";
        GHtoEvents gHtoEvents = new MATSimMMBuilder().buildGhToEvents(networkFilename, new CH1903LV03PlustoWGS84(), Paths.get(""));

        QueryResult qr = gHtoEvents.getHopper().getLocationIndex().findClosest(start.getLat(), start.getLon(), EdgeFilter.ALL_EDGES);
        assertNotNull(qr.getClosestEdge());

        List<Link> links = gHtoEvents.networkRouteWithoutTravelTimes(Arrays.asList(start1, end1));
        assertNotNull(links);
        assertTrue(links.size() > 1);

        links = gHtoEvents.networkRouteWithoutTravelTimes(Arrays.asList(start, end));
        assertNotNull(links);
        assertTrue(links.size() > 1);


        //    Coord test 8.09821439432	47.274493016  ->  x="2682108.0" y="1226718.0"
    }




}
