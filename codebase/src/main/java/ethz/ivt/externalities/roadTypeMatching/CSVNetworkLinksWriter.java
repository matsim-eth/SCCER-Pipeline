package ethz.ivt.externalities.roadTypeMatching;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class CSVNetworkLinksWriter {
    private Network network;

    public static void main(String[] args) throws IOException {
        Network network = NetworkUtils.readNetwork(args[0]);
        new CSVNetworkLinksWriter(network).write(args[1]);
    }

    public CSVNetworkLinksWriter(Network network) throws IOException {
        this.network = network;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Link item : this.network.getLinks().values()) {
            writer.write(formatItem(item) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(";", new String[] {
                "LinkId", "Length (m)", "Urbanity-level"
        });
    }

    private String formatItem(Link link) {
        String urbanity =  "Ungebaut".equalsIgnoreCase((String) link.getAttributes().getAttribute("CH_BEZ_D")) ? "rural" : "urban";
        return String.join(";", new String[] {
                link.getId().toString(),
                Double.toString(link.getLength()),
                urbanity
        });
    }
}

