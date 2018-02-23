package ethz.ivt.graphhopperMM;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.DataReader;
import com.graphhopper.storage.GraphHopperStorage;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;


/**
 * Created by molloyj on 23.10.2017.
 */
public class GraphHopperMATSim extends GraphHopper {

    final Network network;
    CoordinateTransformation matsim2wgs;
    //TODO; refactor this so that the reader makes more sense and stuff
    public GraphHopperMATSim(String networkFilename, CoordinateTransformation matsim2wgs) {

        this.network =  readNetwork(networkFilename);
        this.matsim2wgs = matsim2wgs;
        this.setDataReaderFile(networkFilename);
    }

    public GraphHopperMATSim(Network network, CoordinateTransformation matsim2wgs) {
        this.network =  network;
        this.matsim2wgs = matsim2wgs;
        this.setDataReaderFile("/");
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
