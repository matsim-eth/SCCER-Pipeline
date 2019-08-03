package ethz.ivt.externalities.counters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class ExtendedPersonDepartureEvent extends Event {

    private final double distance;
    private final String tripleg_id;
    private final PersonDepartureEvent personDepartureEvent;

    public ExtendedPersonDepartureEvent(final double time, final Id<Person> agentId,
                                        final Id<Link> linkId, final String legMode,
                                        final double distance, final String tripleg_id) {

        super(time);
        this.personDepartureEvent = new PersonDepartureEvent(time, agentId, linkId, legMode);
        this.distance = distance;
        this.tripleg_id = tripleg_id;
    }

    public double getDistance() {
        return distance;
    }

    public String getTripleg_id() {
        return tripleg_id;
    }

    @Override
    public String getEventType() {
        return "ExtendedPersonDepartureEvent";
    }

    public Id<Person> getPersonId() {
        return personDepartureEvent.getPersonId();
    }

    public PersonDepartureEvent getPersonDepartureEvent() {
        return personDepartureEvent;
    }
}
