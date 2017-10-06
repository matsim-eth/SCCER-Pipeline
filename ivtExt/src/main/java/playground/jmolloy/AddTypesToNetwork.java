package playground.jmolloy;

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileReader;
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
 */
public class AddTypesToNetwork {



    public static void main (String[] args) throws Exception {
        Config config_old = ConfigUtils.createConfig();
        	Scenario sc_old = ScenarioUtils.createScenario(config_old);
        	MatsimNetworkReader reader = new MatsimNetworkReader(sc_old.getNetwork());
        	reader.readFile("C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\mmNetwork.xml");

        CSVReader csvReader = new CSVReader(
                new FileReader("C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\link_types.csv"),
                ','
        );
        String[] ss;
        csvReader.readNext();
        while ((ss = csvReader.readNext()) != null) {
            Id<Link> id = Id.createLinkId(ss[0]);
            Link l = sc_old.getNetwork().getLinks().get(id);

            if (!l.getAllowedModes().contains(TransportMode.car)) {
                NetworkUtils.setType(l, null);
            }
            else {
                String type = ss[ss.length - 2].split("_")[0];
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
        AtomicInteger i = new AtomicInteger();
        Map<String, Integer> link_types = sc_old.getNetwork().getLinks().values().stream()
                .map(NetworkUtils::getType)
                .distinct()
                .filter(o -> o != null && o.contains("_"))
                .sorted(Comparator.comparing(o -> o.split("_")[0]))
                .collect(Collectors.toMap(Function.identity(), x -> i.incrementAndGet()));

        Map<String, Long> counts = sc_old.getNetwork().getLinks().values().stream()
                .map(NetworkUtils::getType)
                .filter(o -> o != null && o.contains("_"))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        counts.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue)).forEach(System.out::println);

        for (Link l : sc_old.getNetwork().getLinks().values()) {
            String type = NetworkUtils.getType(l);
            if (type != null && type.length() > 0) {
                int type_id = link_types.get(type);
                NetworkUtils.setType(l, Integer.toString(type_id));
            }
        }


        Map<String, HBFEA> hbfeaMap = new HashMap<>();
        hbfeaMap.put("motorway-Nat.", new HBFEA("MW-Nat.",80,130));
        hbfeaMap.put("motorway", new HBFEA("MW-City",60,90));
        hbfeaMap.put("primary-Nat.", new HBFEA("Trunk-Nat.",80,110));
        hbfeaMap.put("primary", new HBFEA("Trunk-City",50,80));
        hbfeaMap.put("secondary", new HBFEA("Distr",50,80));
        hbfeaMap.put("tertiary", new HBFEA("Local",50,60));
        hbfeaMap.put("residential", new HBFEA("Access",30,50));
        hbfeaMap.put("living", new HBFEA("Access",30,50));



        List<String> linkTypeString = link_types.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .map(e -> e.getValue() + ";" + e.getKey() + ";" + getHEBFAtype(hbfeaMap, e.getKey()))
                .collect(Collectors.toList());

        List<String> linkTypeString_w_header = new ArrayList<String> ();
        linkTypeString_w_header.add("VISUM_RT_NR;VISUM_RT_NAME;HBEFA_RT_NAME");
        linkTypeString_w_header.addAll(linkTypeString);


        Path path = Paths.get("C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\distinct_link_types.txt");
        Files.write(path, linkTypeString_w_header, StandardCharsets.UTF_8);

        new NetworkWriter(sc_old.getNetwork()).writeFileV1("C:\\Users\\molloyj\\Documents\\SCCER\\zurich_1pc\\network_zurich_w_types.xml");

    }


    public static String getHEBFAtype(Map<String, HBFEA>  hbfeaMap, String road_type) {


        String[] ss = road_type.split("_");
        String type = ss[0];
        int speed = Integer.parseInt(ss[1]);

        //TODO: make distinction between national and city, based on shapefile, or regions.

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
            System.out.println("error");
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
