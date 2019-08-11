package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PTCongestionCounter implements ExtendedPersonDepartureEventEventHandler, PersonArrivalEventHandler {
    private static final Logger log = Logger.getLogger(CongestionCounter.class);
    private static final HashSet<String> PT_MODES
            = new HashSet<>(Arrays.asList(TransportMode.pt, TransportMode.train, "bus", "tram"));
    public static final String PT_CONGESTION_KEY = "pt_congestion_m";

    private Scenario scenario;

    private Map<Id<Person>, Double> personPTdistance = new HashMap<>();
    private Map<Id<Person>, Double> personPTdepartureTime = new HashMap<>();
    private Map<Id<Person>, Id<Link>> personPTdepartureLink = new HashMap<>();

    private ExternalityCounter externalityCounterDelegate;


    public PTCongestionCounter(Scenario scenario, ExternalityCounter externalityCounterDelegate) {

        this.scenario = scenario;
        this.externalityCounterDelegate = externalityCounterDelegate;
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
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (PT_MODES.contains(event.getLegMode())) {
            Id<Person> personId = event.getPersonId();
            double start_time = personPTdepartureTime.get(personId);
            double distance = personPTdistance.get(personId);

            double end_time = event.getTime();
            double trip_duration = end_time - start_time;

            double time_in_morning_peak = getTimeInPeak(start_time, end_time, 7*3600, 9*3600);
            double time_in_evening_peak = getTimeInPeak(start_time, end_time, 17*3600, 19*3600);

            double total_time_in_peak = time_in_morning_peak + time_in_evening_peak;
            double percentage_in_peak = total_time_in_peak / trip_duration;
            double distance_in_peak = distance * percentage_in_peak;
            externalityCounterDelegate.incrementTempValueBy(personId, PT_CONGESTION_KEY, distance_in_peak);

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
