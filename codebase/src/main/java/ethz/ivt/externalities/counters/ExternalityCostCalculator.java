package ethz.ivt.externalities.counters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExternalityCostCalculator {
    private static final Logger log = Logger.getLogger(ExternalityCostCalculator.class);

    private Map<String, Double> rv;

    public ExternalityCostCalculator(String filename) {
        Stream<String> lines = null;
        try {
            lines = Files.lines(Paths.get(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        rv = lines.filter(x -> !x.startsWith("x") && !x.isEmpty()) //ignore empty lines and comments
                .map(l -> l.split("="))
                .collect(Collectors.toMap(x -> x[0].trim(), x -> Double.parseDouble(x[1])));

        double years = rv.get("scenario.year") - rv.get("base.year");
        double C02_cost_increase = years * rv.get("CO2.costs.growth_rate") * rv.get("CO2.climate.costs.2010");
        rv.put("CO2.climate.costs.adj", rv.get("CO2.climate.costs.2010") + C02_cost_increase);
        log.info(String.format("Original CO2 cost was: %s in %.0f", rv.get("CO2.climate.costs.2010"), rv.get("base.year")));
        log.info(String.format("With growth rate of %f, adding %f", rv.get("CO2.costs.growth_rate"), C02_cost_increase));

        double norm_scale_years = rv.get("scenario.year") - rv.get("noise.base.year");
        double noise_cost_increase = norm_scale_years * rv.get("noise.costs.growth_rate") * rv.get("noise.average.cost");
        rv.put("noise.average.cost.adj", rv.get("noise.average.cost") + noise_cost_increase);


    }

    public static void main(String[] args) {
        ExternalityCostCalculator ecc = new ExternalityCostCalculator("data/NISTRA_reference_values.txt");
        ecc.rv.forEach((k,v) -> System.out.println(k + " -> " + v));
    }

    public void addCosts(ExternalityCounter ec) {
        Map<Id<Person>,List<LegValues>> emissions = ec.getPersonId2Leg();
        emissions.forEach((pid, list) -> {
            list.forEach(xs -> xs.putAll(calculateCostsForLeg(xs)));
        });
        ec.addKeys(getAllKeys(emissions));
    }

    private Set<String> getAllKeys(Map<Id<Person>, List<LegValues>> emissions) {
        return emissions.values().stream().flatMap(x->x.stream().flatMap(x1->x1.keys().stream())).collect(Collectors.toSet());
    }

    private Map<String,Double> calculateCostsForLeg(LegValues emissions) {
        Map<String, Double> costs = new HashMap<>();

        if ("Car".equals(emissions.getMode()) && emissions.get("MappedDistance") == 0.0) {
            emissions.setDistance(emissions.get("MappedDistance"));
        }
        addPTEmissions(emissions);

        //CO2.
        if (emissions.containsKey("CO2(total)")) {
            double CO2 = emissions.get("CO2(total)") * rv.get("CO2.climate.costs.adj") / 1e6;
            costs.put("CO2_costs", CO2);
        }
        //Non-exhaust PM
        if (emissions.containsKey("MappedDistance_urban") && emissions.containsKey("MappedDistance_rural")) {
            double PM_urban_non_exhaust = emissions.get("MappedDistance_urban") / 1000 * rv.get("PM10.non_exhaust.g_per_km_pv");
            double PM_rural_non_exhaust = emissions.get("MappedDistance_rural") / 1000 * rv.get("PM10.non_exhaust.g_per_km_pv");

            double PM_urban_total = emissions.get("PM_urban") + PM_urban_non_exhaust;
            double PM_rural_total = emissions.get("PM_rural") + PM_rural_non_exhaust;

            emissions.put("PM_urban", PM_urban_total);
            emissions.put("PM_rural", PM_rural_total);
        }
        //PM urban
        if (emissions.containsKey("PM_urban") && emissions.containsKey("PM_rural")) {
            double PM_urban_health = emissions.get("PM_urban") * rv.get("PM10.healthcare.urban") / 1e6;
            double PM_urban_buildings = emissions.get("PM_urban") * rv.get("PM10.building.urban") / 1e6;
            //PM rural
            double PM_rural_health = emissions.get("PM_rural") * rv.get("PM10.healthcare.rural") / 1e6;
            double PM_rural_buildings = emissions.get("PM_rural") * rv.get("PM10.building.rural") / 1e6;

            //PM regional
            double PM_regional_health = (emissions.get("PM_urban") + emissions.get("PM_rural")) * rv.get("PM10.healthcare.regional") / 1e6;
            double PM_regional_buildings = (emissions.get("PM_urban") + emissions.get("PM_rural")) * rv.get("PM10.building.regional") / 1e6;

            costs.put("PM_health_costs", PM_urban_health + PM_rural_health + PM_regional_health);
            costs.put("PM_building_damage_costs", PM_urban_buildings + PM_rural_buildings + PM_regional_buildings);
        }
        //NOX regional
        if (emissions.containsKey("NOx")) {
            double NOX_regional = emissions.get("NOx") * rv.get("NOX.regional") / 1e6;
            costs.put("NOx_costs", NOX_regional);
        }
        //Zinc
        if ("Car".equals(emissions.getMode())) {
            double zinc_regional = (emissions.get("MappedDistance")) / 1000 * rv.get("Zinc.g_per_km_pv") * rv.get("Zinc.soil_quality.regional") / 1e6;
            costs.put("Zinc_costs", zinc_regional);

            double noise_costs = (emissions.get("MappedDistance")) / 1000 * rv.get("noise.average.cost.adj");
            costs.put("Noise_costs", noise_costs);
        }
        else if ("Walk".equals(emissions.getMode())) {
            costs.put("Active_costs", emissions.getDistance() / 1000 * rv.get("walking.health.cost"));
        }
        else if ("Bicycle".equals(emissions.getMode())) {
            costs.put("Active_costs", emissions.getDistance() / 1000 * rv.get("cycling.health.cost"));

        }


        return costs;
    }

    private void addPTEmissions(LegValues emissions) {
        String mode = null;

        if ("Train".equals(emissions.getMode())) {
            mode = "sbb.regional_train.";
        } else if (TransportMode.pt.equals(emissions.getMode())) {
            mode = "sbb.bus.";
        }

        if (mode != null) {
            emissions.put("PM_urban", emissions.getDistance() / 1000  * (rv.get(mode + "PM10")) * 0.5);
            emissions.put("PM_rural", emissions.getDistance() / 1000 * (rv.get(mode + "PM10")) * 0.5);
            emissions.put("NOx", emissions.getDistance() / 1000 * rv.get(mode + "NOX"));

            emissions.put("CO2(total)", emissions.getDistance() / 1000 * rv.get(mode + "CO2"));
        }
    }

}
