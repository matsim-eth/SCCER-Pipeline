package ethz.ivt.graphhopperMM;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.GPXEntry;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03Plus;

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
    public void noWaypointsCH() {
        GPXEntry start = new GPXEntry(47.44724, 8.16896, 0);
        GPXEntry end = new GPXEntry(47.44229, 8.16805, 0);

        GPXEntry end1 = new GPXEntry(47.1927405, 8.7224103, (2*60+1)*1000);
        GPXEntry start1 = new GPXEntry(47.2000532, 8.7955055, 0);

        Coord ch1 = new Coord( 2655083.0, 1255417.0);
        Coord ch = new WGS84toCH1903LV03Plus().transform(new Coord(start.getLon(), start.getLat()));
        assertEquals(ch1, ch);

        String networkFilename = "src/test/resources/swiss_network.xml";
        GHtoEvents gHtoEvents = new MATSimMMBuilder().buildGhToEvents(networkFilename, new CH1903LV03PlustoWGS84());

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
