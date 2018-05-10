package ethz.ivt.externalities.roadTypeMatching;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.NullProgressListener;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.roadTypeMapping.OsmHbefaMapping;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by molloyj on 27.07.2017.
 *
 * This class prepares a MATSim network for use with the emissions contrib by assigning types that
 * match the HBEFA database. The road types are taken from an OSM shapefile, in the same coordinate reference system.
 * Based on the MATSim link speed and the nearest OSM link, a road type is defined.
 * These types are then matched to the HBEFA types, and a new network with type ides, and a type mapping (as csv) are generated.
 * These can then be used as inputs to MATSim simulations that include the emissions module.
 * N.B. The links or attributes of the network are not changed, only the types are added.
 */
//TODO: document!!!!!!!!!!
public class printRoadTypes {


    public static void main (String[] args) throws Exception {
        Network network = NetworkUtils.createNetwork();
        MatsimNetworkReader reader = new MatsimNetworkReader(network);
        reader.readFile(args[0]);


        OsmHbefaMapping osmmap = OsmHbefaMapping.build();

        try {
            PrintWriter pw = new PrintWriter(new FileWriter(new File("links.csv")));
            network.getLinks().values().forEach(l -> {
                String osmType = (String) l.getAttributes().getAttribute("osm:way:highway");
                if (osmType == null)
                    osmType = "unclassified";
                String type = getHEBFAtype(osmType, l.getFreespeed());
                pw.println(l.getId().toString() + ","  +  osmType +  ","  +  type);
            });
            pw.close();

        } catch (IOException ex) {
        }
        System.out.println("types printed");

    }


    static private String getHEBFAtype(String roadType, double freeVelocity) {


        String[] ss = roadType.split("_"); //want to remove
        String type = ss[0];

        //TODO: could make distinction between national and city, based on shapefile, or regions.

        if (type.equals("unclassified") || type.equals("road")) {
            if (freeVelocity <= 50) type = "living";
            else if (freeVelocity == 60) type = "tertiary";
            else if (freeVelocity == 70) type = "secondary";
            else if (freeVelocity <= 90) type = "primary";
            else type = "motorway";
        }

        //specify that if speed > 90 and primary or motorway, then Nat.
        if (type.equals("motorway") || type.equals("primary") && freeVelocity >= 90) {
            type += "-Nat.";
        }
        return type;



    }


}
