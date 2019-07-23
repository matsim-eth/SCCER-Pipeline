package ethz.ivt.externalities.counters;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Map;

public class TestECC {

    @Test
    public void testSimpleEmissions() {
        ExternalityCostCalculator ecc = new ExternalityCostCalculator("../data/NISTRA_reference_values.txt");

        LegValues emissions1 = new LegValues(LocalDateTime.now(), "car");
        emissions1.setDistance(10000);
        emissions1.put("NOx", 44.3723);
        emissions1.put("CO2(total)", 25340.7437);
        emissions1.put("N2O",0.4804);
        emissions1.put("CH4",0.1508);
        emissions1.put("delay_caused", 50 );

        Map<String, Double> costs = ecc.calculateCostsForLeg(emissions1);

        for (String key : emissions1.keys()) {
            System.out.println(key + ": " + emissions1.get(key));
        }

        System.out.println("\ncosts");
        for (String key : costs.keySet()) {
            System.out.println(key + ": " + costs.get(key));
        }

    }

    @Test
    public void testSimpleBusEmissions() {
        ExternalityCostCalculator ecc = new ExternalityCostCalculator("../data/NISTRA_reference_values.txt");

        LegValues emissions1 = new LegValues(LocalDateTime.now(), "bus");
        emissions1.setDistance(10000);

        Map<String, Double> costs = ecc.calculateCostsForLeg(emissions1);

        for (String key : emissions1.keys()) {
            System.out.println(key + ": " + emissions1.get(key));
        }

        System.out.println("\ncosts");
        for (String key : costs.keySet()) {
            System.out.println(key + ": " + costs.get(key));
        }

    }


    @Test
    public void testSimpleTrainEmissions() {
        ExternalityCostCalculator ecc = new ExternalityCostCalculator("../data/NISTRA_reference_values.txt");

        LegValues emissions1 = new LegValues(LocalDateTime.now(), "train");
        emissions1.setDistance(50000);

        Map<String, Double> costs = ecc.calculateCostsForLeg(emissions1);

        for (String key : emissions1.keys()) {
            System.out.println(key + ": " + emissions1.get(key));
        }

        System.out.println("\ncosts");
        for (String key : costs.keySet()) {
            System.out.println(key + ": " + costs.get(key));
        }

    }


}
