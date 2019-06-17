package ethz.ivt;

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

public class JITvehicleCreator implements PersonDepartureEventHandler {
    Scenario scenario;
    List<Id<Vehicle>> addedVehicles = new LinkedList<>();

    public JITvehicleCreator(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Id<Person> pid = event.getPersonId();

        Id<VehicleType>  vt = Id.create("Benzin", VehicleType.class);
        Id<Vehicle> vid = Id.createVehicleId(pid);
        //easy option: add
        if (!scenario.getVehicles().getVehicles().containsKey(vid)) {
            VehicleType car = scenario.getVehicles().getVehicleTypes().get(vt);
            Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, car);
            scenario.getVehicles().addVehicle(v);

            addedVehicles.add(vid);
            //scenario.getHouseholds().popul  ().get(hid).getVehicleIds().add(vid);
        }
    }

    @Override
    public void reset(int iteration) {
    //    addedVehicles.forEach(vid ->scenario.getVehicles().removeVehicle(vid));
    //   addedVehicles.clear();
    }
}
