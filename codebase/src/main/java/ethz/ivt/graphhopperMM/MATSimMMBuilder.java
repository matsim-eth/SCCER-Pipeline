package ethz.ivt.graphhopperMM;

/**
 * Created by molloyj on 10.11.2017.
 */

import com.graphhopper.GraphHopper;
import com.graphhopper.MapMatchingUnlimited;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.Parameters;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;
import java.nio.file.Path;

public class MATSimMMBuilder {

    public GHtoEvents buildGhToEvents(String networkFilename, CoordinateTransformation trans, Path hopper_location) {
        GraphHopperMATSim hopper = GraphHopperMATSim.build(networkFilename, trans, hopper_location);
        MapMatchingUnlimited mapMatcher = createMapMatching(hopper);
        return new GHtoEvents(hopper, mapMatcher);

    }


    public GHtoEvents buildGhToEvents(Network network, CoordinateTransformation trans, Path hopper_location) {
        GraphHopperMATSim hopper = GraphHopperMATSim.build(network, trans, hopper_location);
        MapMatchingUnlimited mapMatcher = createMapMatching(hopper);
        return new GHtoEvents(hopper, mapMatcher);

    }



    private MapMatchingUnlimited createMapMatching(GraphHopper hopper) {
        String algorithm = Parameters.Algorithms.DIJKSTRA_BI;

        //TODO: dont just get first encoder, let the searcher specify the transport type
        Weighting weighting = new FastestWeighting(hopper.getEncodingManager().getEncoder("car"));
        AlgorithmOptions algoOptions = new AlgorithmOptions(algorithm, weighting);
        MapMatchingUnlimited mapMatching = new MapMatchingUnlimited(hopper, algoOptions);

        return mapMatching;
    }
}
