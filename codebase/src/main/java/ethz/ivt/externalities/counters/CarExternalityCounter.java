package ethz.ivt.externalities.counters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;

import java.util.Map;

/**
 * Created by molloyj on 10.10.2017.
 */
public class CarExternalityCounter implements WarmEmissionEventHandler, ColdEmissionEventHandler, LinkEnterEventHandler {

    private static final double ECAR = 1;
    private static final double CAR = 0;

    private ExternalityCounter externalityCounterDelegate;
    private Scenario scenario;

    public CarExternalityCounter(Scenario scenario, ExternalityCounter externalityCounterDelegate) {
        this.scenario = scenario;
    	this.externalityCounterDelegate = externalityCounterDelegate;
    //    initializeFields(); //JM'18 - fields are now added dynamically during operation.
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(event.getVehicleId());

        double linkLength = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
        externalityCounterDelegate.incrementTempValueBy(personId, "Distance", linkLength);

        // distance in urban or rural setting
        String distanceLandUseKey = addLandUseToEmissionKey("Distance", event.getLinkId());
        externalityCounterDelegate.incrementTempValueBy(personId, distanceLandUseKey, linkLength);
    }


    @Override
    public void handleEvent(ColdEmissionEvent e) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(e.getVehicleId());

        // add emissions
        Map<String, Double> pollutants = e.getColdEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();

            if (pollutant.equals("PM")) {
                pollutant = addLandUseToEmissionKey(pollutant, e.getLinkId());
            }

            externalityCounterDelegate.incrementTempValueBy(personId,pollutant, p.getValue());
        }
    }


    @Override
    public void handleEvent(WarmEmissionEvent e) {
        Id<Person> personId = externalityCounterDelegate.getDriverOfVehicle(e.getVehicleId());
        if (personId == null) { //TODO fix this, so that the person id is retrieved properly
            personId = Id.createPersonId(e.getVehicleId().toString());
        }

        // add emissions
        Map<String, Double> pollutants = e.getWarmEmissions();
        for (Map.Entry<String, Double> p : pollutants.entrySet()) {
            String pollutant = p.getKey();

            if (pollutant.equals("PM")) {
                pollutant = addLandUseToEmissionKey(pollutant, e.getLinkId());
            }

            externalityCounterDelegate.incrementTempValueBy(personId,pollutant, p.getValue());
        }
    }

    private String addLandUseToEmissionKey(String pollutant, Id<Link> linkId) {
        Link link = scenario.getNetwork().getLinks().get(linkId);
        String urbanity = (String) link.getAttributes().getAttribute("CH_BEZ_D");
        boolean isRurual = "Ungebaut".equalsIgnoreCase(urbanity);
        return pollutant + (isRurual ? "_rural" : "_urban");
    }


}
