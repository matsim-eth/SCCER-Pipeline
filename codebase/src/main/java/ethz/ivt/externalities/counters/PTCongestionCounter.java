package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.congestion.PtChargingZones;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.util.ZoneFinder;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.matsim.contrib.zone.util.NetworkWithZonesUtils.createLinkToZoneMap;

public class PTCongestionCounter implements ExtendedPersonDepartureEventEventHandler, PersonArrivalEventHandler {
    private static final Logger log = Logger.getLogger(CongestionCounter.class);
    private static final HashSet<String> PT_MODES
            = new HashSet<>(Arrays.asList(TransportMode.pt, TransportMode.train, "bus", "tram"));
    public static final String PT_CONGESTION_KEY = "pt_congestion_m";
    private final PtChargingZones ptChargingZones;

    private Scenario scenario;

    private Map<Id<Person>, Double> personPTdistance = new HashMap<>();
    private Map<Id<Person>, Double> personPTdepartureTime = new HashMap<>();
    private Map<Id<Person>, Id<Link>> personPTdepartureLink = new HashMap<>();

    private ExternalityCounter externalityCounterDelegate;


    public PTCongestionCounter(Scenario scenario, ExternalityCounter externalityCounterDelegate,
                               PtChargingZones ptChargingZones) {

        this.scenario = scenario;
        this.externalityCounterDelegate = externalityCounterDelegate;
        this.ptChargingZones = ptChargingZones;

        externalityCounterDelegate.addKey(PT_CONGESTION_KEY); //freespeed travel time

    }

    //don't need to look at the arrival event, as we know the length of the trip from the dep. event.
    @Override
    public void handleEvent(ExtendedPersonDepartureEvent event) {
        if (PT_MODES.contains(event.getPersonDepartureEvent().getLegMode())) {
            double distance = event.getDistance();
            double start_time = event.getTime();
            Id<Person> personId = event.getPersonId();

            personPTdistance.put(personId, distance);
            personPTdepartureTime.put(personId, start_time);
            personPTdepartureLink.put(personId, event.getPersonDepartureEvent().getLinkId());

        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (PT_MODES.contains(event.getLegMode())) {
            Id<Person> personId = event.getPersonId();
            double start_time = personPTdepartureTime.get(personId);
            double distance = personPTdistance.get(personId);

            boolean is_peak_connection = true;
            if (ptChargingZones != null) {
                Id<Link> start_link = personPTdepartureLink.get(personId);
                Id<Link> end_link = event.getLinkId();
                is_peak_connection = ptChargingZones.is_peak_connection(start_link, end_link);
            }

            if (is_peak_connection) {
                double end_time = event.getTime();
                double trip_duration = end_time - start_time;

                double time_in_morning_peak = getTimeInPeak(start_time, end_time, 7 * 3600, 9 * 3600);
                double time_in_evening_peak = getTimeInPeak(start_time, end_time, 17 * 3600, 19 * 3600);

                double total_time_in_peak = time_in_morning_peak + time_in_evening_peak;
                double percentage_in_peak = total_time_in_peak / trip_duration;
                double distance_in_peak = distance * percentage_in_peak;
                externalityCounterDelegate.incrementTempValueBy(personId, PT_CONGESTION_KEY, distance_in_peak);
            }

            personPTdepartureTime.remove(personId);
            personPTdistance.remove(personId);
            personPTdepartureLink.remove(personId);
        }
    }

    protected double getTimeInPeak(double start_time, double end_time, double peak_start, double peak_end) {
        return Math.max(0, Math.min(end_time, peak_end) - Math.max(peak_start, start_time));
    }

    @Override
    public void reset(int iteration) {

    }

}
