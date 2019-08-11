package ethz.ivt.externalities.data.congestion;

import ethz.ivt.externalities.counters.ExternalityCounter;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.contrib.zone.util.NetworkWithZonesUtils.createLinkToZoneMap;

public class PtChargingZones {

    Map<Id<Zone>, Zone> pt_zones;
    Map<Id<Link>, Zone> linkToZoneMap;
    Map<Id<Zone>, Set<Id<Zone>>> peakODpairs;

    public static void main (String[] args) {
        new PtChargingZones(null, Paths.get("C:\\Projects\\SCCER_project\\data\\pt_zones\\pt_zones_shapefile\\pt_zones.shp")
                ,  Paths.get("C:\\Projects\\SCCER_project\\data\\pt_zones\\od_pairs.csv"));
    }

    public PtChargingZones(Scenario scenario, Path zonesShpFile, Path odPairsFile) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(zonesShpFile.toString());

        pt_zones = features.stream().map(ft -> {
            String id = ft.getAttribute("gmdqnr").toString();
            Id<Zone> zone_id = Id.create(ft.getAttribute("gmdqnr").toString(), Zone.class);
            Zone z = new Zone(zone_id, "pt", (MultiPolygon)ft.getDefaultGeometry());
            return z;
        }).collect(Collectors.toMap(Zone::getId, z -> z));

        ZoneFinder zoneFinder = new ZoneFinderImpl(pt_zones, 0);
        linkToZoneMap = createLinkToZoneMap(scenario.getNetwork(), zoneFinder);
        peakODpairs = loadODPairs(odPairsFile);

    }

    private Map<Id<Zone>, Set<Id<Zone>>> loadODPairs(Path odPairsFile) {
        Map<Id<Zone>, Set<Id<Zone>>> zoneMapping = new HashMap<>();
        try {
            Files.lines(odPairsFile).forEach(l -> {
                String[] ss = l.split(",");
                Id<Zone> origin = Id.create(ss[0].trim(), Zone.class);
                List<Id<Zone>> destinations = Arrays.stream(ss[1].replaceAll("\"", "").split(" "))
                        .map(s -> Id.create(s.trim(), Zone.class)).collect(Collectors.toList());
                zoneMapping.put(origin, new HashSet<>(destinations));
            });
            return zoneMapping;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean is_peak_connection(Id<Link> e1, Id<Link> e2) {
        boolean is_peak = false;
        if (e1 != null && e2 != null) {
            Zone zone1 = linkToZoneMap.get(e1);
            Zone zone2 = linkToZoneMap.get(e2);
            if (zone1 != null && zone2 != null) {
                Set<Id<Zone>> dest_zones = peakODpairs.get(zone1.getId());
                if (dest_zones != null) {
                    is_peak = dest_zones.contains(zone2.getId());
                }
            }
        }
        return is_peak;
    }

}
