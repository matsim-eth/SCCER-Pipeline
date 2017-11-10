package playground.ivt.proj_sccer.graphhopperMM;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.DataReader;
import com.graphhopper.storage.GraphHopperStorage;
import contrib.baseline.lib.NetworkUtils;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.File;


/**
 * Created by molloyj on 23.10.2017.
 */
public class GraphHopperMATSim extends GraphHopper {

    Network network;
    CoordinateTransformation matsim2wgs;

    public GraphHopperMATSim(String networkFilename, CoordinateTransformation matsim2wgs) {

        this.network =  NetworkUtils.readNetwork(networkFilename);
        this.matsim2wgs = matsim2wgs;
        this.setDataReaderFile(networkFilename);
    }

    @Override
    protected DataReader createReader(GraphHopperStorage ghStorage) {
        MATSimNetwork2graphhopper reader = new MATSimNetwork2graphhopper(ghStorage, network, matsim2wgs );

        return initDataReader(reader);


    }

    public GraphHopper test() {
        return new GraphHopperMATSim(null, null)
                .setGraphHopperLocation(new File("").getAbsolutePath())
                .importOrLoad();
    }



}
