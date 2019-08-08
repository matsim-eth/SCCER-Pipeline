package ethz.ivt;

import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;

public class CSVVehicleWriter {
    final private Collection<Vehicle> vehicles;

    public CSVVehicleWriter(Collection<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public void write(Path outputFileName) throws IOException {

        File file = outputFileName.toFile();
        file.getParentFile().mkdirs();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Vehicle vehicle : vehicles) {
            writer.write(formatItem(vehicle) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(";", new String[] {
                "vehicleId", "vehicleCategory", "fuelType", "emissionLevel", "engineSize"
        });
    }

    private String formatItem(Vehicle vehicle) {
        String vehicleId = vehicle.getId().toString();
        String[] description = vehicle.getType()
                .getDescription()
                .split("END_EMISSIONS")[0]
                .split("BEGIN_EMISSIONS")[1]
                .split(";");

        String engineSize = description[2];

        String[] subsegment = description[3].split(" ");
        String vehicleCategory = subsegment[0];
        String fuelType = subsegment[1];
        String emissionLevel = subsegment[2];

        return String.join(";", new String[] {
                vehicleId,
                vehicleCategory,
                fuelType,
                emissionLevel,
                engineSize
        });
    }
}
