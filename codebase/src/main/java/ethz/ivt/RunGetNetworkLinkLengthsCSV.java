package ethz.ivt;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RunGetNetworkLinkLengthsCSV {

    public static void main(String[] args) throws IOException {

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1])));


        String header = String.join(";",
                new String[] { "link_id", "length" });

        writer.write(header + "\n");
        writer.flush();

        for (Link link : network.getLinks().values()) {

            String entry = String.join(";", new String[] {
                    link.getId().toString(),
                    String.valueOf(link.getLength()), });

            writer.write(entry + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }
}
