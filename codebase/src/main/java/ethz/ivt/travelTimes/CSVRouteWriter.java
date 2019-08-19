package ethz.ivt.travelTimes;

import jdk.internal.dynalink.support.LinkerServicesImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class CSVRouteWriter {
    final private Collection<RouteItem> routeItems;

    public CSVRouteWriter(Collection<RouteItem> routeItems) {
        this.routeItems = routeItems;
    }

    public void write(String outputPath) throws IOException {
        System.out.println("Writing routes to " + outputPath);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        // format header
        writer.write(formatHeader() + "\n");
        writer.flush();

        Counter counter = new Counter("Route #");

        for (RouteItem routeItem : this.routeItems) {

            writer.write(formatItem(routeItem) + "\n");
            writer.flush();

            counter.incCounter();
        }

        writer.flush();
        writer.close();
        counter.printCounter();

        System.out.println("Done");
    }

    private String formatHeader() {
        return String.join(";", new String[] {
                "departure_time", "start_x", "start_y", "end_x", "end_y", "travel_time", "links",
        });
    }

    private String formatItem(RouteItem routeItem) {

        Collection<String> links = routeItem.links
                .stream()
                .map(link -> link.getId().toString())
                .collect(Collectors.toList());

        return String.join(";", new String[] {
                Double.toString(routeItem.departureTime),
                Double.toString(routeItem.startCoord.getX()),
                Double.toString(routeItem.startCoord.getY()),
                Double.toString(routeItem.endCoord.getX()),
                Double.toString(routeItem.endCoord.getY()),
                Double.toString(routeItem.travelTime),
                String.join(",", new ArrayList<>(links).toString()),
        });
    }
}
