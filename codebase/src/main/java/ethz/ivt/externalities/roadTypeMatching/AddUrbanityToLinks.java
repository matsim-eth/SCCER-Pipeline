package ethz.ivt.externalities.roadTypeMatching;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.SyslogQuietWriter;
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import javax.swing.text.html.Option;
import java.io.File;
import java.io.IOException;
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
 * This class adds the seidlung status to the links in the network
 */
public class AddUrbanityToLinks {

    private static Logger logger = Logger.getLogger(AddUrbanityToLinks.class);
    private static Map<String, Integer> link_types;
    private final Network network;


    private SpatialIndex index;

    /**
     * Constructor takes the matsim network and an OSM shapefile
     * @param network
     * @param attributeShapefile
     * @throws IOException
     */
    public AddUrbanityToLinks(Network network, File attributeShapefile) throws IOException {

        FeatureSource<SimpleFeatureType, SimpleFeature> source = loadOSMShapefile(attributeShapefile);
        index = buildSpatialIndex(source);
        this.network = network;

    }

    public static void main (String[] args) throws Exception {
        Network network = NetworkUtils.readNetwork(args[0]);

        File outputFile = new File(args[1]);
        //load shapefile of OSM network with types
        File osmShapefile = new File(args[2]);

        AddUrbanityToLinks addUrbanityToLinks = new AddUrbanityToLinks(network, osmShapefile);

        addUrbanityToLinks.run();

        new NetworkWriter(network).writeFileV2(outputFile.toString());


    }

    private void run() {
        network.getLinks().values().forEach(l -> {
            Optional<SimpleFeature> sf = getLandUse(l);
            Object landuse = sf.map(sf1 -> sf1.getAttribute("CH_BEZ_D")).orElse("Ungebaut");
            l.getAttributes().putAttribute("CH_BEZ_D", landuse);
        });
    }

    /**
     * This function loads the OSM shapefile
     * @param file The osm shapefile
     * @return the set of features
     * @throws IOException
     */
    private FeatureSource<SimpleFeatureType, SimpleFeature> loadOSMShapefile(File file) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
                .getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

        //FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);

        return source;

    }

    /**
     * From the FeatureSource loaded in @loadOSMShapefile, a @final @{@link SpatialIndex} is created, that
     * allows for the fast searching of of OSM links
     * @param source
     * @return
     * @throws IOException
     */
    private static SpatialIndex buildSpatialIndex(FeatureSource<SimpleFeatureType, SimpleFeature> source)  throws IOException {

        final SpatialIndex index = new STRtree();
        FeatureCollection features = source.getFeatures();
        System.out.println("Slurping in features ...");
        features.accepts(feature -> {
            SimpleFeature simpleFeature = (SimpleFeature) feature;
            Geometry geom = (MultiPolygon) simpleFeature.getDefaultGeometry();

            if (geom != null) {
                Envelope env = geom.getEnvelopeInternal();
                if (!env.isNull()) {
                    index.insert(env, simpleFeature);
                }
            }
        }, new NullProgressListener());

        return index;
    }

    /**
     * Get the Envelope of the MATSim link.
     * @param link
     * @return
     */
    private Envelope getLinkEnvelope(Link link) {
        Coordinate c1 = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
        Coordinate c2 = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
        return new Envelope(c1, c2);
    }

    /**
     * The algorithm for finding the matching link, using the @{@link SpatialIndex}.
     * The list of potential candidates within the searchRadius is collected.
     * Then, the distance between the centroids of the @{@link Link} and the candidates are compared,
     * with the link with the smallest distance being returned. There is room for more advanced methods there, but it
     * works fast enough.
     *
     * @param link The MATSim link to find a match for.
     * @return Returns the feature which is closest to the link (based on centroids)
     */
    public Optional<SimpleFeature> getLandUse(Link link) {
        //algorithm for matching points
        //if they share both verticies, then copy over the type
        //if they share one vertex, take nearest neighbour by centre
        //if link has no shared edges with any in shapefile, then take closest centroid by type

        //TODO: check x,y order and projection
        Envelope search = getLinkEnvelope(link);
        Coordinate[] link_coords = {toGeoCoord(link.getFromNode().getCoord()), toGeoCoord(link.getToNode().getCoord())};
        LineString linkLineString = JTSFactoryFinder.getGeometryFactory().createLineString(link_coords);

        //get potential matches
        List<SimpleFeature> areas = index.query(search);

        //check the matches,
        double maxOverlap = 0.0;
        Optional<SimpleFeature> maxFeature = Optional.empty();
        //TODO: improve this logic, to pick the best candidate
        for (SimpleFeature f : areas) {
            Geometry poly = (Geometry) f.getDefaultGeometry();
            double dist = poly.intersection(linkLineString).getLength();

            if (dist > maxOverlap) {
                maxOverlap = dist;
                maxFeature = Optional.of(f);
            }
        }

        String landuse = maxFeature.map(sf -> sf.getAttribute("CH_BEZ_D")).orElse("None").toString();
        System.out.println("number of matches for " + link.getId() + ": " + areas.size() + ", found: " + landuse);

        return maxFeature;
    }
    //helper function from MATSim to Geotools
    private static Coordinate toGeoCoord(Coord c) {
        return new Coordinate(c.getX(), c.getY());
    }

    private static Coordinate getCentroidOfLink(Link link) {
        return toGeoCoord(link.getCoord());
    }

    private void updateLinkLandUse(Link l, SimpleFeature feature) {

    }

}
