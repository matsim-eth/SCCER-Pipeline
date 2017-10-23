package playground.ivt.proj_sccer.traces2matsim;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.DataReader;
import com.graphhopper.storage.GraphHopperStorage;
import org.matsim.api.core.v01.network.Network;

import java.io.File;


/**
 * Created by molloyj on 23.10.2017.
 */
public class GraphHopperMATSim extends GraphHopper {

    Network network;

    public GraphHopperMATSim(Network network) {
        this.network = network;
    }

    @Override
    protected DataReader createReader(GraphHopperStorage ghStorage) {
        MATSimNetwork2graphhopper reader = new MATSimNetwork2graphhopper(ghStorage, network);

        return initDataReader(reader);


    }

    public GraphHopper test() {
        return new GraphHopperMATSim(null)
                .setGraphHopperLocation(new File("").getAbsolutePath())
                .importOrLoad();
    }



}
