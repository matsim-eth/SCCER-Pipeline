package ethz.ivt;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ProcessNetwork {

    private final Network network;

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("network-path", "output-path")
                .build();

        String networkPath = cmd.getOptionStrict("network-path");
        String outputPath = cmd.getOptionStrict("output-path");

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        new ProcessNetwork(network).write(outputPath);

    }

    public ProcessNetwork(Network network) {
        this.network = network;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Link link : network.getLinks().values()) {
            writer.write(formatEntry(link) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(";", new String[] { //
                "link_id",
                "x",
                "y",
                "is_motorway"
        });
    }

    private String formatEntry(Link link) {

        // get if road type is motorway
        String roadType = (String) link.getAttributes().getAttribute("osm:way:highway");
        if (roadType == null) {
            roadType = "none";
        }
        boolean isMotorway = roadType.contains("motorway");

        return String.join(";", new String[] { //
                String.valueOf(link.getId().toString()), //
                String.valueOf(link.getCoord().getX()), //
                String.valueOf(link.getCoord().getY()), //
                String.valueOf(isMotorway)
        });
    }


}
