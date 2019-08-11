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

        double years = rv.get("scenario.year") - rv.get("CO2.climate.costs.base.year");
        double C02_cost_increase = years * rv.get("CO2.costs.growth_rate") * rv.get("CO2.climate.costs.base");
        rv.put("CO2.climate.costs.adj", rv.get("CO2.climate.costs.base") + C02_cost_increase);
        log.info(String.format("Original CO2 cost was: %s in %.0f", rv.get("CO2.climate.costs.base"), rv.get("CO2.climate.costs.base.year")));
        log.info(String.format("With growth rate of %f, adding %f", rv.get("CO2.costs.growth_rate"), C02_cost_increase));

        double norm_scale_years = rv.get("scenario.year") - rv.get("noise.base.year");
        double noise_cost_increase = 1 + norm_scale_years * rv.get("noise.costs.growth_rate");
        rv.put("noise.average.cost.adjustment", noise_cost_increase);


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

    protected Map<String,Double> calculateCostsForLeg(LegValues emissions) {
        Map<String, Double> costs = new HashMap<>();


        if (emissions.getMode().equalsIgnoreCase("train")
                ||emissions.getMode().equalsIgnoreCase("pt")
                ||emissions.getMode().equalsIgnoreCase("tram")
                ||emissions.getMode().equalsIgnoreCase("bus")) {
            addPTEmissions(emissions, costs);
        }
        if ("car".equalsIgnoreCase(emissions.getMode())) {
            addCarEmissions(emissions, costs);
        }
        if ("car".equalsIgnoreCase(emissions.getMode())) {
            addCarEmissions(emissions, costs);
        }

        //NOX regional
        double NOX_regional = emissions.get("NOx") * rv.get("NOX.regional.CHF_t") / 1e6;
        costs.put("NOx_costs", NOX_regional);

        //Zinc
        String zincKey = "Zinc.soil_quality." + emissions.getMode();
        double zinc_g = rv.getOrDefault(zincKey, 0.0) / 1e6;
        double zinc_cost = (emissions.getDistance()) / 1000 * rv.get("Zinc.regional") * zinc_g;
        costs.put("Zinc_costs", zinc_cost);

        String noiseKey = "noise.average.cost." + emissions.getMode();
        double noise_base_cost = rv.getOrDefault(noiseKey, 0.0);
        double noise_costs = (emissions.getDistance())  / 1000 * noise_base_cost * rv.get("noise.average.cost.adjustment");
        costs.put("Noise_costs", noise_costs);

        String activeHealthKey = "health.cost." + emissions.getMode();

        costs.put("Active_costs", emissions.getDistance() / 1000 * rv.getOrDefault(activeHealthKey, 0.0));

        //congestion
        double congestion_costs = emissions.get("delay_caused") * rv.get("VTTS") / 3600;
        costs.put("Congestion_costs", congestion_costs);

        return costs;
    }

    private void addCarEmissions(LegValues emissions, Map<String, Double> costs ) {

        double CO2_equivelent_g = 0;
        //CO2 equivelents
        CO2_equivelent_g += emissions.get("CO2(total)");
        CO2_equivelent_g += emissions.get("N2O") * rv.get("N2O.CO2.equivalents");
        CO2_equivelent_g += emissions.get("CH4") * rv.get("CH4.CO2.equivalents");
        emissions.put("CO2_equivalent_emissions", CO2_equivelent_g);

        //co2 costs
        double CO2_equivilent_costs =  emissions.get("CO2_equivalent_emissions") * rv.get("CO2.climate.costs.adj") / 1e6;
        costs.put("CO2_costs", CO2_equivilent_costs);

        if (!emissions.containsKey("MappedDistance_urban") || emissions.containsKey("MappedDistance_rural")) {
            emissions.put("MappedDistance_urban", emissions.getDistance()/2);
            emissions.put("MappedDistance_rural", emissions.getDistance()/2);
        }

        //Non-exhaust PM
        double PM_urban_non_exhaust = emissions.get("MappedDistance_urban") / 1000 * rv.get("PM10.non_exhaust.innerorts.g_per_km_pv");
        double PM_rural_non_exhaust = emissions.get("MappedDistance_rural") / 1000 * rv.get("PM10.non_exhaust.ausserorts.g_per_km_pv");

        double PM_urban_total = emissions.get("PM_urban") + PM_urban_non_exhaust;
        double PM_rural_total = emissions.get("PM_rural") + PM_rural_non_exhaust;

        emissions.put("PM_urban", PM_urban_total);
        emissions.put("PM_rural", PM_rural_total);

        double PM_urban_health = emissions.get("PM_urban") * rv.get("PM10.healthcare.car.urban") / 1e6;
        double PM_urban_buildings = emissions.get("PM_urban") * rv.get("PM10.building.car.urban") / 1e6;
        //PM rural
        double PM_rural_health = emissions.get("PM_rural") * rv.get("PM10.healthcare.car.rural") / 1e6;
        double PM_rural_buildings = emissions.get("PM_rural") * rv.get("PM10.building.car.rural") / 1e6;

        //PM regional
        double PM_regional_health = (emissions.get("PM_urban") + emissions.get("PM_rural")) * rv.get("PM10.healthcare.car.regional") / 1e6;
        double PM_regional_buildings = (emissions.get("PM_urban") + emissions.get("PM_rural")) * rv.get("PM10.building.car.regional") / 1e6;

        costs.put("PM_health_costs", PM_urban_health + PM_rural_health + PM_regional_health);
        costs.put("PM_building_damage_costs", PM_urban_buildings + PM_rural_buildings + PM_regional_buildings);
    }

    private void addPTEmissions(LegValues emissions, Map<String, Double> costs ) {

        String mode = emissions.getMode().toLowerCase();
        double distance = emissions.getDistance()/1000;
        double co2_costs_p_km = rv.getOrDefault("CO2."+mode+".CHF_pkm", 0.0);
        costs.put("CO2_costs", co2_costs_p_km*distance);

        double pm10_health = rv.getOrDefault("PM10.healthcare."+mode+".CHF_pkm", 0.0);
        double pm10_building = rv.getOrDefault("PM10.building."+mode+".CHF_pkm", 0.0);

        double nox = rv.getOrDefault("NOX."+mode+".urban.g_per_pkm", 0.0) * distance;
        emissions.put("NOx", nox);


        costs.put("PM_health_costs", pm10_health);
        costs.put("PM_building_damage_costs", pm10_building);

        //congestion
        double peak_surcharge_per_km = rv.getOrDefault("peak.surcharge.pt.CHF_km", 0.0);
        double congestion_costs = emissions.get("pt_congestion_m") / 1000 * peak_surcharge_per_km;
        costs.put("Congestion_costs", congestion_costs);
    }

}
