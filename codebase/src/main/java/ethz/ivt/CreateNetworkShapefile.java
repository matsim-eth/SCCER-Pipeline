package ethz.ivt;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import java.io.IOException;

public class CreateNetworkShapefile {

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("network-path", "coord-system", "output-path")
                .build();

        // assign command line arguments
        String networkPath = cmd.getOptionStrict("network-path");
        String coordSystem = cmd.getOptionStrict("coord-system");
        String outputPath = cmd.getOptionStrict("output-path");

        // read network
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        // write links to shapefile
        new Links2ESRIShape(network, outputPath, coordSystem).write();
    }
}
