package playground.ivt.proj_sccer.aggregation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by molloyj on 06.10.2017.
 */
public abstract class EventAggregator implements LinkEnterEventHandler, PersonDepartureEventHandler {
    private static final Logger log = Logger.getLogger(EventAggregator.class);

    protected final double binSize_s; //30 hours, how do we split them
    protected int num_bins; //30 hours, how do we split them
    protected Map<Id<Link>, Map<String, double[]>> linkId2timeBin2values = new HashMap<>();
    protected Map<Id<Link>, double[]> linkId2timeBin2enteringAndDepartingAgents = new HashMap<Id<Link>, double[]>();
    protected Map<Id<Link>, Map<Integer, List<Id<Person>>>> linkId2timeBin2personIdCausingDelay = new HashMap<>();
    protected Map<Id<Link>, double[]> linkId2timeBin2numberCausingDelay = new HashMap<>();
    protected List<PersonDepartureEvent> personDepartureEvents = new ArrayList<PersonDepartureEvent>();
    private List<LinkEnterEvent> linkEnterEvents = new ArrayList<LinkEnterEvent>();
    private boolean setMethodsExecuted = false;

    public EventAggregator(Scenario scenario, double binSize_s) {
        this.num_bins = (int) (30 * 3600 / binSize_s);
        this.binSize_s = binSize_s;

        setUpBinsForLinks(scenario);
        log.info("Number of congestion bins: " + num_bins);
    }

    protected void setUpBinsForLinks(Scenario scenario) {
        scenario.getNetwork().getLinks().keySet().forEach(l -> {
            linkId2timeBin2values.put(l, new HashMap<>());
            linkId2timeBin2values.get(l).putIfAbsent("delay", new double[num_bins]); //no nulls
            linkId2timeBin2enteringAndDepartingAgents.put(l, new double[num_bins]);
            linkId2timeBin2personIdCausingDelay.put(l, new HashMap<>());
            for(int bin = 0; bin<this.num_bins; bin++) {
            	linkId2timeBin2personIdCausingDelay.get(l).put(bin, new ArrayList<>());
            }
            linkId2timeBin2numberCausingDelay.put(l, new double[num_bins]);
        });
    }

    @Override
    public void reset(int iteration) {
        this.linkId2timeBin2values.clear();
        this.linkId2timeBin2enteringAndDepartingAgents.clear();
        
        this.linkId2timeBin2personIdCausingDelay.clear();
        this.linkId2timeBin2numberCausingDelay.clear();

        this.linkEnterEvents.clear();
        this.personDepartureEvents.clear();

        this.setMethodsExecuted = false;
    }

    /*package*/ int getTimeBin(double time) {

        double timeAfterSimStart = time;

		/*
		 * Agents who end their first activity before the simulation has started
		 * will depart in the first time step.
		 */
        if (timeAfterSimStart <= 0.0) return 0;

		/*
		 * Calculate the bin for the given time. Increase it by one if the result
		 * of the modulo operation is > 0. If it is 0, it is the last time value
		 * which is part of the previous bin.
		 */  
        int bin = (int) (timeAfterSimStart / binSize_s);
        if (timeAfterSimStart % binSize_s == 0.0) bin--;

        return bin;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        int bin = getTimeBin(event.getTime());

        this.linkEnterEvents.add(event);
        //this.linkId2timeBin2enteringAndDepartingAgents.putIfAbsent(event.getLinkId(), new double[num_bins]);
        this.linkId2timeBin2enteringAndDepartingAgents.get(event.getLinkId())[bin]++; //TODO: do we have to consider PCU;s here?
    }


    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(TransportMode.car.toString())) {
            this.personDepartureEvents.add(event);

            int bin = getTimeBin(event.getTime());
            //this.linkId2timeBin2enteringAndDepartingAgents.putIfAbsent(event.getLinkId(), new double[num_bins]);
            this.linkId2timeBin2enteringAndDepartingAgents.get(event.getLinkId())[bin]++; //TODO: do we have to consider PCU;s here?

        } else {
            // other simulated modes are not accounted for
        }
    }
}
