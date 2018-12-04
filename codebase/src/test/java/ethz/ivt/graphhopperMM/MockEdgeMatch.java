package ethz.ivt.graphhopperMM;

import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.util.EdgeIteratorState;
import org.matsim.api.core.v01.network.Link;

import java.util.List;

/**
 * Created by molloyj on 14.11.2017.
 */
public class MockEdgeMatch extends EdgeMatch {
    public MockEdgeMatch(Link link, List<GPXExtension> gpxExtension) {
        super(new MockEdgeIterator(link.getId()), gpxExtension);
    }
}
