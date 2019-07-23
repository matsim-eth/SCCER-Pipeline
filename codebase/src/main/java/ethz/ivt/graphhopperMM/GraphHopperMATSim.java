package ethz.ivt.graphhopperMM;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.DataReader;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.GraphHopperStorage;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created by molloyj on 23.10.2017.
 */
public class GraphHopperMATSim extends GraphHopper {

    final Network network;
    CoordinateTransformation matsim2wgs;

    private GraphHopperMATSim(Network network, CoordinateTransformation matsim2wgs) {
        this.network =  network;
        this.matsim2wgs = matsim2wgs;
        this.setDataReaderFile("/");
    }

    public static GraphHopperMATSim build(Network network, CoordinateTransformation coordinateTransformation) {
        return new GraphHopperMATSim(network, coordinateTransformation);
    }

    public static GraphHopperMATSim build(String networkFilename, CoordinateTransformation coordinateTransformation, Path hopper_location) {
        return build(readNetwork(networkFilename), coordinateTransformation, hopper_location);
    }

    public static GraphHopperMATSim build(Network network, CoordinateTransformation coordinateTransformation, Path hopper_location) {
        GraphHopperMATSim hopper = new GraphHopperMATSim(network, coordinateTransformation);
        hopper.setGraphHopperLocation(hopper_location.toString());

        //TODO: set up multiple encoders
        hopper.setEncodingManager(EncodingManager.create("car"));

        hopper.getCHFactoryDecorator().setEnabled(false);
        //hopper.setCHEnabled(true);
        hopper.setPreciseIndexResolution(1000); //TODO: refactor this so that the index resoltion can be set on startup. but this is needed to find edges on some nodes
        hopper.importOrLoad();

        return hopper;
    }

    @Override
    public DataReader createReader(GraphHopperStorage ghStorage) {
        MATSimNetwork2graphhopper reader = new MATSimNetwork2graphhopper(ghStorage, network, matsim2wgs );

        return initDataReader(reader);


    }

    public GraphHopper test() {
        return new GraphHopperMATSim((Network) null, null)
                .setGraphHopperLocation(new File("").getAbsolutePath())
                .importOrLoad();
    }
    public static Network readNetwork(String path2Network) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(path2Network);
        return scenario.getNetwork();
    }

    public CoordinateTransformation getCoordinateTransform() {
        return matsim2wgs;
    }


    //input should be person_stage_date - ie for GPS - personid_input_date




}
