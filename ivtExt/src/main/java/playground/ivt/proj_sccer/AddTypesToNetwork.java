package playground.ivt.proj_sccer;

import com.opencsv.CSVReader;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.NullProgressListener;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.GeometryUtils;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
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
public class AddTypesToNetwork {

    private static Logger logger = Logger.getLogger(AddTypesToNetwork.class);
    private static Map<String, Integer> link_types;
    private final Network network;


    Map<String, HBFEA> hbfeaMap = new HashMap<>();
    private SpatialIndex index;
    private static int  MAX_SEARCH_DISTANCE = 2000;
    private static String WORKING_FOLDER = "C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\network_editing\\version2\\";
    private Map<Id<Link>, String> linkMatches = new HashMap<>();

    /**
     * Constructor takes the matsim network and an OSM shapefile
     * @param network
     * @param osmShapefile
     * @throws IOException
     */
    public AddTypesToNetwork(Network network, File osmShapefile) throws IOException {

        FeatureSource<SimpleFeatureType, SimpleFeature> source = loadOSMShapefile(osmShapefile);
        index = buildSpatialIndex(source);
        this.network = network;

        //set up hebfa types

        hbfeaMap.put("motorway-Nat.", new HBFEA("MW-Nat.",80,130));
        hbfeaMap.put("motorway", new HBFEA("MW-City",60,90));
        hbfeaMap.put("primary-Nat.", new HBFEA("Trunk-Nat.",80,110));
        hbfeaMap.put("primary", new HBFEA("Trunk-City",50,80));
        hbfeaMap.put("secondary", new HBFEA("Distr",50,80));
        hbfeaMap.put("tertiary", new HBFEA("Local",50,60));
        hbfeaMap.put("residential", new HBFEA("Access",30,50));
        hbfeaMap.put("living", new HBFEA("Access",30,50));

    }

    public static void main (String[] args) throws Exception {
        Config config_old = ConfigUtils.createConfig();
        Scenario sc_old = ScenarioUtils.createScenario(config_old);
        MatsimNetworkReader reader = new MatsimNetworkReader(sc_old.getNetwork());
        reader.readFile("C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\mmNetwork.xml");

        //load shapefile of OSM network with types
        File osmShapefile = new File("C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\network_editing\\version2\\new_net_shp_plus\\network_lines.shp");

        AddTypesToNetwork osmReference = new AddTypesToNetwork(sc_old.getNetwork(), osmShapefile);

        osmReference.run();

        Path path = Paths.get(WORKING_FOLDER + "distinct_link_types.txt");
        Files.write(path, osmReference.buildHBEFAlinkTypes(link_types), StandardCharsets.UTF_8);

        new NetworkWriter(sc_old.getNetwork()).writeFileV2(WORKING_FOLDER + "network_zurich_w_types.xml");


    }

    private void run() {

        findMatchingLinks(network);
        link_types = buildRoadTypeIndex(network);
        replaceNetworkTypesWithId(network);

    }

    /**
     * Does a simple increasing search , starting with a 10m radius, and increasing to @MAX_SEARCH_DISTANCE, until a matching link is found
     * TODO: examined links could be filtered by appropriate speeds.
     * @param network
     */
    private void findMatchingLinks(Network network) {
        for (Link link : network.getLinks().values()) {
            SimpleFeature matchingLink = null;
            int searchRadius = 10;
            //keep searching with a larger radius, until a link is found
            while (searchRadius < MAX_SEARCH_DISTANCE && (matchingLink = getOSMLink(link, searchRadius)) == null) {
                searchRadius *= 2;
            }
            updateLinkType(link, matchingLink);

        }
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
            String roadType = (String) simpleFeature.getAttribute("type");
            Geometry geom = (MultiLineString) simpleFeature.getDefaultGeometry();
            //simpleFeature.setDefaultGeometry(new LocationIndexedLine(geom));
            // Just in case: check for  null or empty geometry
    //        System.out.println(simpleFeature.getAttribute("ID").toString() + "_" + roadType);
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
     * @param searchRadius The search radius (m) around the link envelope to examine for candidate links.
     * @return Returns the feature which is closest to the link (based on centroids)
     */
    public SimpleFeature getOSMLink(Link link, int searchRadius) {
        //algorithm for matching points
        //if they share both verticies, then copy over the type
        //if they share one vertex, take nearest neighbour by centre
        //if link has no shared edges with any in shapefile, then take closest centroid by type

        //TODO: check x,y order and projection
        Point pt = JTSFactoryFinder.getGeometryFactory().createPoint(getCentroidOfLink(link));
        Envelope search = getLinkEnvelope(link);
        search.expandBy(searchRadius);

        //get potential matches
        List<SimpleFeature> lines = index.query(search);

        //check the matches,
        double minDist = searchRadius + 1.0e-6;
        SimpleFeature minDistFeature = null;

        //TODO: improve this logic, to pick the best candidate
        for (SimpleFeature f : lines) {
            MultiLineString line = (MultiLineString) f.getDefaultGeometry();

            double dist = line.getCentroid().distance(pt);
            if (dist < minDist) {
                minDist = dist;
                minDistFeature = f;
            }
        }

        return minDistFeature;
    }
    //helper function from MATSim to Geotools
    private static Coordinate toGeoCoord(Coord c) {
        return new Coordinate(c.getX(), c.getY());
    }

    private static Coordinate getCentroidOfLink(Link link) {
        return toGeoCoord(link.getCoord());
    }

    /**
     * Updates the link to add the road type from the feature. Does some extra work to simplify the heirachy a bit,
     * i.e. living_street to residential. May not consider all OSM road types in its current implementation.
     * Also adds the speed, based on the MATSim freespeed of the link.
     * The type 'unclassified' will be used for any unlabeled links.
     * @param l
     * @param feature
     */
    private void updateLinkType(Link l, SimpleFeature feature) {

        if (!l.getAllowedModes().contains(TransportMode.car)) {
            NetworkUtils.setType(l, null);
            linkMatches.put(l.getId(), feature.getID());
        }
        else {
            String type;
            if (feature != null) {
                type = feature.getAttribute("type").toString();
            } else {
                type = "unclassified";
            }
            if (("living_street").equals(type)) type = "residential";
            if (type.contains("_link")) type = type.split("_")[0];
            if ("trunk".equals(type)) type = "primary";

            if (type != null && type.length() > 0) {

                long speedCat = Math.round(l.getFreespeed() * 3.6);
                if (Double.isInfinite(l.getFreespeed()))
                    speedCat = 130;
                speedCat = ((speedCat + 5) / 10) * 10;
                speedCat = Math.min(130, speedCat);
                type += "_" + speedCat;
                NetworkUtils.setType(l, type);

            }
        }
    }


    /**
     * build the mapping of link types needed for the emissions contrib.
     * @param link_type_map
     * @return
     */
    private List<String> buildHBEFAlinkTypes(Map<String, Integer> link_type_map) {

        List<String> linkTypeString = link_type_map.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(e -> e.getValue() + ";" + e.getKey() + ";" + getHEBFAtype(hbfeaMap, e.getKey()))
                .collect(Collectors.toList());

        List<String> linkTypeString_w_header = new ArrayList<String> ();
        linkTypeString_w_header.add("VISUM_RT_NR;VISUM_RT_NAME;HBEFA_RT_NAME");
        linkTypeString_w_header.addAll(linkTypeString);

        return linkTypeString_w_header;

    }

    /**
     * From a network with types, build an index of the types, sorted by name.
     * @param network A network with OSM types already added
     * @return
     */
    private Map<String, Integer> buildRoadTypeIndex(Network network) {
        AtomicInteger i = new AtomicInteger();
        Map<String, Integer> link_types = network.getLinks().values().stream()
                .map(NetworkUtils::getType)
                .distinct()
                .filter(o -> o != null && o.contains("_"))
                .sorted(Comparator.comparing(o -> o.split("_")[0]))
                .collect(Collectors.toMap(Function.identity(), x -> i.incrementAndGet()));

        Map<String, Long> counts = network.getLinks().values().stream()
                .map(NetworkUtils::getType)
                .filter(o -> o != null && o.contains("_"))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        counts.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).forEach(System.out::println);

        return link_types;
    }

    /**
     * replace the network types with the ID's generated in @buildRoadTypeIndex
     * @param network A network with OSM types already added
     */
    private void replaceNetworkTypesWithId(Network network) {
        //set link types to the id of the type
        for (Link l : network.getLinks().values()) {
            String type = NetworkUtils.getType(l);
            if (type != null && type.length() > 0) {
                int type_id = link_types.get(type);
                NetworkUtils.setType(l, Integer.toString(type_id));
            }
        }
    }


    /**
     * Get the HBEFA type for a road type, based on the @hbfeaMap.
     * If the speed is greater than 90kmh, the road is classified as National.
     * All types are classified as Urban. This could be updated if suitable information is available.
     * Unclassified types are assigned a type based on their MATSim freespeed.
     * @param hbfeaMap
     * @param road_type
     * @return
     */
    public String getHEBFAtype(Map<String, HBFEA>  hbfeaMap, String road_type) {


        String[] ss = road_type.split("_");
        String type = ss[0];
        int speed = Integer.parseInt(ss[1]);

        //TODO: could make distinction between national and city, based on shapefile, or regions.

        if (type.equals("unclassified")) {
            if (speed <= 50) type = "living";
            else if (speed == 60) type = "tertiary";
            else if (speed == 70) type = "secondary";
            else if (speed <= 90) type = "primary";
            else type = "motorway";
        }

        //specify that if speed > 90 and primary or motorway, then Nat.
        if (type.equals("motorway") || type.equals("primary") && speed >= 90) {
            type += "-Nat.";
        }
        if (hbfeaMap.get(type) == null) {
            throw new RuntimeException("'"+ type +"' not in hbefa map");
        }
        int min_speed = hbfeaMap.get(type).min;
        int max_speed = hbfeaMap.get(type).max;
        int capped_speed = Math.min(Math.max(min_speed, speed), max_speed);

        return "URB/" + hbfeaMap.get(type).name + "/" + capped_speed;


  /*      URB/MW-Nat./80 - 130
        URB/MW-City/60 - 110
        URB/Trunk-Nat./70 - 110
        URB/Trunk-City/50 - 90
        URB/Distr/50 - 80
        URB/Local/50 - 60
        URB/Access/30 - 50


        motorway;MW
        primary;Trunk
        secondary;Distr
        tertiary;Local
        residential;Access
        living;Access


*/







    }


}
