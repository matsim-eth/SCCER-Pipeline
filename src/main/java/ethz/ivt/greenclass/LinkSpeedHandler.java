package ethz.ivt.greenclass;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkSpeedHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {

    private Map<Id<Link>, List<LinkTimingStruct>> link_timings = new HashMap<>();
    private Map<Id<Link>, Double> current_link_entrytime = new HashMap<>();

    public class LinkTimingStruct {
        public final Id<Link> link_id;
        public final double entry_time;
        public final double exit_time;

        public LinkTimingStruct(Id<Link> l, double entryTime, double exitTime) {
            this.link_id = l;
            this.entry_time = entryTime;
            this.exit_time = exitTime;
        }

        public double getTravelTime() {
            return exit_time - entry_time;
        }

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Link>  link_id = event.getLinkId();

        if (current_link_entrytime.containsKey(link_id)) {
            double entryTime = current_link_entrytime.get(link_id); //do I need to handle person departure and arrival events
            current_link_entrytime.remove(link_id);
            double exitTime = event.getTime();
            link_timings.putIfAbsent(link_id, new ArrayList<>());

            LinkTimingStruct lts = new LinkTimingStruct(event.getLinkId(), entryTime, exitTime);

            link_timings.get(link_id).add(lts);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Link> link_id = event.getLinkId();
        Double entry_time = event.getTime();
        current_link_entrytime.put(link_id, entry_time);

    }

    @Override
    public void reset(int iteration) {
        current_link_entrytime.clear();
    }

    public Map<Id<Link>, List<LinkTimingStruct>> getLinkTimings() {
        return link_timings;
    }
}
