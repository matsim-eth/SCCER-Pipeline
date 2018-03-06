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
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;

public class MATSimMMBuilder {

    public GHtoEvents buildGhToEvents(String networkFilename, CoordinateTransformation trans) {
        GraphHopperMATSim hopper = buildGraphHopper(networkFilename, trans);
        MapMatching mapMatcher = createMapMatching(hopper);
        return new GHtoEvents(hopper, mapMatcher);

    }

    private GraphHopperMATSim buildGraphHopper(String networkFileName, CoordinateTransformation coordinateTransformation) {


        GraphHopperMATSim hopper = new GraphHopperMATSim(networkFileName, coordinateTransformation);

        hopper.setStoreOnFlush(false)
                .setGraphHopperLocation(new File("").getAbsolutePath());

        //TODO: set up multiple encoders
        hopper.setEncodingManager(new EncodingManager("bike,car"));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        return hopper;
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
