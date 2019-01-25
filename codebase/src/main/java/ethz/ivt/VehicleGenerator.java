package ethz.ivt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class VehicleGenerator {

    private Scenario scenario;
    private List<Double> shares = new LinkedList<>();
    private List<Id<VehicleType>> vehicleTypeIds = new LinkedList<>();

    public VehicleGenerator(Scenario scenario) {
        this.scenario = scenario;
    }

    public void read(String path, int year) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        double cumulativeShare = 0.0;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                String vehicleCategory = row.get(header.indexOf("Vehicle category"));
                String fuelType = row.get(header.indexOf("Fuel type"));
                String emissionLevel = row.get(header.indexOf("Emission level"));
                double fraction = Double.valueOf(row.get(header.indexOf(Integer.toString(year))));

                cumulativeShare += fraction;

                String fuelTypeFull;
                if (fuelType.equals("P")) {
                    fuelTypeFull = "petrol (4S)";
                } else {
                    fuelTypeFull = "diesel";

                    if (emissionLevel.equals("Euro-5") || emissionLevel.equals("Euro-6")) {
                        emissionLevel += " DPF";
                    }
                }

                Id<VehicleType> vehicleId = Id.create("PASSENGER_CAR;"
                                + fuelTypeFull + ";"
                                + "1,4-<2L;"
                                + vehicleCategory + " " + fuelType + " " + emissionLevel,
                        VehicleType.class);

                VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehicleId);
                vehicleType.setMaximumVelocity(100.0 / 3.6);
                vehicleType.setPcuEquivalents(1.0);
                vehicleType.setDescription("BEGIN_EMISSIONS" + vehicleId.toString() + "END_EMISSIONS");
                scenario.getVehicles().addVehicleType(vehicleType);

                shares.add(cumulativeShare);
                vehicleTypeIds.add(vehicleId);
            }
        }

        reader.close();
    }

    public void setUpVehicles() {

        Random randomGenerator = new Random();

        for (Id<Person> pid : scenario.getPopulation().getPersons().keySet()) {
            Id<Vehicle> vid = Id.createVehicleId(pid);

            //add vehicles according to share
            double percent = randomGenerator.nextDouble();

            for (double share : shares) {
                if (percent <= share) {
                    VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(vehicleTypeIds.get(shares.indexOf(share)));
                    Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(vid, vehicleType);
                    scenario.getVehicles().addVehicle(vehicle);
                    break;
                }
            }

            if (!scenario.getVehicles().getVehicles().containsKey(vid)) {
                VehicleType vehicleType = scenario.getVehicles().getVehicleTypes().get(vehicleTypeIds.get(vehicleTypeIds.size() - 1));
                Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(vid, vehicleType);
                scenario.getVehicles().addVehicle(vehicle);
            }
        }
    }

}
