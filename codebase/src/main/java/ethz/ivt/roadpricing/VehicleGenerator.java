package ethz.ivt.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.*;
import org.matsim.vehicles.EngineInformation.FuelType;

import java.io.*;

public class VehicleGenerator {
    private static Logger logger = Logger.getLogger(VehicleGenerator.class);

    public static void main(String[] args) {

        if (args.length != 2) {
            throw new RuntimeException("Please provide the input and output file names as arguments");
        }

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();

        vehicles.addVehicleType(createType("D_00", "Micro", FuelType.diesel, 0));
        vehicles.addVehicleType(createType("D_01", "Subcompact", FuelType.diesel, 0));
        vehicles.addVehicleType(createType("D_02", "Compact", FuelType.diesel, 0));
        vehicles.addVehicleType(createType("D_03", "MiniMVP", FuelType.diesel, 0));
        vehicles.addVehicleType(createType("D_04", "MidSized", FuelType.diesel, 0));
        vehicles.addVehicleType(createType("D_05", "FullSized", FuelType.diesel, 0));
        vehicles.addVehicleType(createType("D_06", "Luxus", FuelType.diesel, 0));

        vehicles.addVehicleType(createType("B_00", "Micro", FuelType.gasoline, 0));
        vehicles.addVehicleType(createType("B_01", "Subcompact", FuelType.gasoline, 0));
        vehicles.addVehicleType(createType("B_02", "Compact", FuelType.gasoline, 0));
        vehicles.addVehicleType(createType("B_03", "MiniMVP", FuelType.gasoline, 0));
        vehicles.addVehicleType(createType("B_04", "MidSized", FuelType.gasoline, 0));
        vehicles.addVehicleType(createType("B_05", "FullSized", FuelType.gasoline, 0));
        vehicles.addVehicleType(createType("B_06", "Luxus", FuelType.gasoline, 0));
        vehicles.addVehicleType(createType("B_07", "Sport", FuelType.gasoline, 0));

        vehicles.addVehicleType(createType("U_00", "AlternativeFuels", FuelType.electricity, 0));

        File file = new File(args[0]);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            br.lines().skip(1).map(l -> {
                String[] columns = l.split(",");
                String pid = columns[1];
                String vtid = columns[2];
                VehicleType vt = vehicles.getVehicleTypes().get(Id.create(vtid, VehicleType.class));
                if (vt == null) {
                    logger.error("Vehicle type [" + vtid + "] was not found");
                }
                Vehicle v = vehicles.getFactory().createVehicle(Id.createVehicleId(pid), vt);
                return v;
            }).forEach(vehicles::addVehicle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        new VehicleWriterV1(vehicles).writeFile(args[1]);
    }
//   "D0,D1,D2,D3,D4,D5,D6,B0,B1,B2,B3,B4,B5,B6,B7,ALT"

    public static VehicleType createType(String id, String name, FuelType fuelType, double averageLitersPerMeter) {
        VehicleType vt = new VehicleTypeImpl(Id.create(id, VehicleType.class));
        vt.setEngineInformation(new EngineInformationImpl(fuelType, averageLitersPerMeter));
        return vt;

    }


}
