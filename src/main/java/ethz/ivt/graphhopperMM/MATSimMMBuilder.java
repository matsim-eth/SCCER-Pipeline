package ethz.ivt.graphhopperMM;

/**
 * Created by molloyj on 10.11.2017.
 */

import com.graphhopper.GraphHopper;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Parameters;
import org.apache.commons.cli.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class MATSimMMBuilder {

    public GHtoEvents buildGhToEvents(String networkFilename, CoordinateTransformation trans) {
        GraphHopperMATSim hopper = buildGraphHopper(networkFilename, trans);
        MapMatching mapMatcher = createMapMatching(hopper);
        return new GHtoEvents(hopper, mapMatcher, hopper.network);

    }

    private GraphHopperMATSim buildGraphHopper(String networkFileName, CoordinateTransformation coordinateTransformation) {


        GraphHopperMATSim hopper = new GraphHopperMATSim(networkFileName, coordinateTransformation);

        hopper.setStoreOnFlush(false)
                .setGraphHopperLocation(new File("").getAbsolutePath());
        CarFlagEncoder encoder = new CarFlagEncoder();
        hopper.setEncodingManager(new EncodingManager(encoder));
        hopper.getCHFactoryDecorator().setEnabled(false);
        hopper.importOrLoad();

        return hopper;
    }

    private MapMatching createMapMatching(GraphHopper hopper) {
        String algorithm = Parameters.Algorithms.DIJKSTRA_BI;

        //TODO: dont just get first encoder
        Weighting weighting = new FastestWeighting(hopper.getEncodingManager().fetchEdgeEncoders().get(0));
        AlgorithmOptions algoOptions = new AlgorithmOptions(algorithm, weighting);
        MapMatching mapMatching = new MapMatching(hopper, algoOptions);

        return mapMatching;
    }
}
