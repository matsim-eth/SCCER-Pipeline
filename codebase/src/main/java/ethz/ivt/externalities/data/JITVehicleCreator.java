package ethz.ivt.externalities.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.LinkedList;
import java.util.List;

public class JITVehicleCreator implements PersonDepartureEventHandler {
    Scenario scenario;

    public JITVehicleCreator(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Id<Person> pid = event.getPersonId();

        Id<Vehicle> vid = Id.createVehicleId(pid);
        //easy option: add
        if (!scenario.getVehicles().getVehicles().containsKey(vid)) {
            Id<VehicleType>  vt = Id.create("PASSENGER_CAR", VehicleType.class);

            if(!scenario.getVehicles().getVehicleTypes().containsKey(vt)) {
                createDefaultVehicle(scenario);
            }

            VehicleType car = scenario.getVehicles().getVehicleTypes().get(vt);
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);
            scenario.getVehicles().addVehicle(v);
        }
    }

    private Id<VehicleType> createDefaultVehicle(Scenario scenario) {

        Id<VehicleType> vehicleId = Id.create("PASSENGER_CAR", VehicleType.class);

        VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(vehicleId);
        vehicleType.setMaximumVelocity(100.0 / 3.6);
        vehicleType.setPcuEquivalents(1.0);
        vehicleType.setDescription("BEGIN_EMISSIONS" + vehicleId.toString() + "END_EMISSIONS");
        scenario.getVehicles().addVehicleType(vehicleType);

        return vehicleId;
    }

    @Override
    public void reset(int iteration) {
        //    addedVehicles.forEach(vid ->scenario.getVehicles().removeVehicle(vid));
        //   addedVehicles.clear();
    }
}
