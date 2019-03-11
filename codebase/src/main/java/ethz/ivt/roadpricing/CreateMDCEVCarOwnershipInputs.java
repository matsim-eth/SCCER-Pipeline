//package ethz.ivt.roadpricing;
//
//import com.opencsv.CSVWriter;
//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.Point;
//import org.geotools.geometry.jts.JTSFactoryFinder;
//import org.geotools.referencing.CRS;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.contrib.socnetgen.sna.gis.Zone;
//import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
//import org.matsim.contrib.socnetgen.sna.gis.io.ZoneLayerSHP;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.population.PersonUtils;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.households.Household;
//import org.opengis.referencing.FactoryException;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.Map;
//import java.util.Objects;
//import java.util.stream.Stream;
//
///**
// * Created by molloyj on 15.08.2017.
// */
//public class CreateMDCEVCarOwnershipInputs {
//
//    private Scenario scenario;
//    private Config config;
//    private ZoneLayer gemeinde;
//
//    public static void main(String[] args) {
//        CreateMDCEVCarOwnershipInputs createFlottenInputs = new CreateMDCEVCarOwnershipInputs();
//        createFlottenInputs.run(args);
//    }
//
//    private void run(String[] args) {
//        config = ConfigUtils.loadConfig(args[0]);
//
//        scenario = ScenarioUtils.loadScenario(config);
//
//
//
//        try (FileWriter f = new FileWriter(new File("output/hh.csv"))) {
//            CSVWriter writer = new CSVWriter(f, '\t');
//            writer.writeNext("HHNR\tZPERS\tAUSB\tEINK\tAUTOS\tZGDE".split("\t"), false);
//
//            gemeinde = ZoneLayerSHP.read("kantons\\G1G10.shp");
//            gemeinde.overwriteCRS(CRS.decode("EPSG:21781"));
//
//            scenario.getHouseholds().getHouseholds().values().stream()
//                    .filter(h -> scenario.getPopulation().getPersons().get(h.getMemberIds().get(0)) != null)
//                .map(this::toFlottenInput)
//                .forEach( l -> writer.writeNext(l, false));
//        } catch (IOException | FactoryException ioex) {
//            throw new RuntimeException(ioex);
//        }
//
//
//        try (FileWriter f = new FileWriter(new File("output/pop.csv"))) {
//
//            CSVWriter writer = new CSVWriter(f, ';');
//            writer.writeNext("householdId;personId;sex;age;license;car_avail;employed".split(";"), false);
//            scenario.getHouseholds().getHouseholds().values().stream()
//                    .flatMap(this::processHouseholdMembers)
//                    .forEach(l -> writer.writeNext(l, false));
//        } catch (IOException ioex) {
//            throw new RuntimeException(ioex);
//        }
//
//    }
//
//
//    private Stream<String[]> processHouseholdMembers(Household hh) {
//           return hh.getMemberIds().stream()
//                   .map(pid -> scenario.getPopulation().getPersons().get(pid))
//                   .filter(Objects::nonNull)
//                   .map(p -> toFlottenPersonInput(hh, p));
//    }
//
//    private String[] toFlottenPersonInput(Household hh, Person p) {
//        String[] ret = new String[7];
//        ret[0] = hh.getId().toString();
//        ret[1] = p.getId().toString();
//        ret[2] = PersonUtils.getSex(p);
//        ret[3] = PersonUtils.getAge(p).toString();
//        ret[4] = PersonUtils.getLicense(p);
//        ret[5] = PersonUtils.getCarAvail(p);
//        ret[6] = PersonUtils.isEmployed(p).toString();
//
//        return ret;
//
//    }
//
//    private String[] toFlottenInput(Household hh) {
//        Id<Household> hhid = hh.getId();
//        int AUSB = 0; //set to zero as it isnt used
//        int ZPERSONS = hh.getMemberIds().size();
//        //int AUTOS = hh.getVehicleIds().size();
//        String AUTOS =  scenario.getHouseholds().getHouseholdAttributes()
//                .getAttribute(hhid.toString(),"numberOfPrivateCars").toString().replace("\n", "").trim();
//        AUTOS = AUTOS.equals("3+") ? "3" : AUTOS; //remove the plus from autos
//        //need to convert income into 1-10 scale (which is defined in the thesis (page 85) as 1,000/month
//        int EINC = (int) hh.getIncome().getIncome() / 1000;
//
//        Person p = scenario.getPopulation().getPersons().get(hh.getMemberIds().get(0));
//        Coord homeLocation = ((Activity) p.getSelectedPlan().getPlanElements().get(0)).getCoord(); //assume persons start from home
//        Point homePoint = JTSFactoryFinder.getGeometryFactory().createPoint(new Coordinate(homeLocation.getX(), homeLocation.getY()));
//
//        homePoint.setSRID(2056); //the matsim CRS code is stupid
//        Zone<Map<String, Object>> gmde = gemeinde.getZone(homePoint);
//        String kt;
//        String gmde1;
//        if (gmde != null) {
//            kt = gmde.getAttribute().get("KT").toString();
//            gmde1 = gmde.getAttribute().get("GMDE").toString();
//        } else if (hh.getId().toString().equals("2228000199101")) {
//            gmde1 = "4545";
//        } else {
//            kt = "??";
//            gmde1 = "??";
//            System.out.println("Home kanton for person " + p.getId() + " not found");
//        }
//
//        String[] ret = new String[6];
//        ret[0] = hhid.toString();
//        ret[1] = Integer.toString(ZPERSONS);
//        ret[2] = "0";
//        ret[3] = Integer.toString(EINC);
//        ret[4] = AUTOS;
//        ret[5] = gmde1;
//        return ret;
//
//    }
//
//}
