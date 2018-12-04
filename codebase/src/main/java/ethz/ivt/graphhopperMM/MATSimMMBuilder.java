package ethz.ivt.graphhopperMM;

/**
 * Created by molloyj on 10.11.2017.
 */

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.Parameters;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;

public class MATSimMMBuilder {

    public GHtoEvents buildGhToEvents(String networkFilename, CoordinateTransformation trans) {
        GraphHopperMATSim hopper = GraphHopperMATSim.build(networkFilename, trans);
        MapMatching mapMatcher = createMapMatching(hopper);
        return new GHtoEvents(hopper, mapMatcher);

    }


    public GHtoEvents buildGhToEvents(Network network, CoordinateTransformation trans) {
        GraphHopperMATSim hopper = GraphHopperMATSim.build(network, trans);
        MapMatching mapMatcher = createMapMatching(hopper);
        return new GHtoEvents(hopper, mapMatcher);

    }



    private MapMatching createMapMatching(GraphHopper hopper) {
        String algorithm = Parameters.Algorithms.DIJKSTRA_BI;

        //TODO: dont just get first encoder, let the searcher specify the transport type
        Weighting weighting = new FastestWeighting(hopper.getEncodingManager().getEncoder("car"));
        AlgorithmOptions algoOptions = new AlgorithmOptions(algorithm, weighting);
        MapMatching mapMatching = new MapMatching(hopper, algoOptions);

        return mapMatching;
    }
}
