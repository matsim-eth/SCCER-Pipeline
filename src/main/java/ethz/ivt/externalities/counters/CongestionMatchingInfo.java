package ethz.ivt.externalities.counters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

public class CongestionMatchingInfo {

    private final Link link;
    private final Id<Person> gpsAgent;
    private final Id<Person> matsimAgent;
    private final double gpsEnterTime;
    private final double matsimEnterTime;
    private double gpsExitTime;
    private double matsimExitTime;
    private double matsimCausedDelay;
    private boolean isGpsExitTimeSet = false;
    private boolean isMatsimExitTime = false;
    private boolean isMatsimCausedDelaySet = false;

    public CongestionMatchingInfo(Link link, Id<Person> gpsAgent, Id<Person> matsimAgent, double gpsEnterTime, double matsimEnterTime) {
        this.link = link;
        this.gpsAgent = gpsAgent;
        this.matsimAgent = matsimAgent;
        this.gpsEnterTime = gpsEnterTime;
        this.matsimEnterTime = matsimEnterTime;
    }

    public Link getLink() {
        return link;
    }

    public Id<Person> getGpsAgent() {
        return gpsAgent;
    }

    public Id<Person> getMatsimAgent() {
        return matsimAgent;
    }

    public void setGpsExitTime(double gpsExitTime) {
        this.gpsExitTime = gpsExitTime;
        this.isGpsExitTimeSet = true;
    }

    public void setMatsimExitTime(double matsimExitTime) {
        this.matsimExitTime = matsimExitTime;
        this.isMatsimExitTime = true;
    }

    public double getGpsEnterTime() {
        return gpsEnterTime;
    }

    public double getGpsExitTime() {
        return gpsExitTime;
    }

    public double getMatsimEnterTime() {
        return matsimEnterTime;
    }

    public double getMatsimExitTime() {
        return matsimExitTime;
    }

    public void setMatsimCausedDelay(double matsimCausedDelay) {
        this.matsimCausedDelay = matsimCausedDelay;
        this.isMatsimCausedDelaySet = true;
    }

    public double computeScaledGPSDelay() {
        double gpsTravelTime = this.gpsExitTime - this.gpsEnterTime;
        double matsimTravelTime = this.matsimExitTime - this.matsimEnterTime;
        return this.matsimCausedDelay * delayScalingFactor(this.link, gpsTravelTime, matsimTravelTime);
    }

    private static double delayScalingFactor(Link link, double gpsTravelTime, double scenarioTravelTime) {
        return (gpsTravelTime) / (scenarioTravelTime);
    }

    public boolean hasAllValuesSet() {
        return (isGpsExitTimeSet && isMatsimExitTime && isMatsimCausedDelaySet);
    }
}
