package ethz.ivt.externalities.counters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.Optional;

public class CongestionMatchingInfo {

    private final Link link;
    private final Id<Person> gpsAgentId;
    private final double gpsEnterTime;
    private Optional<Double> gpsExitTime = Optional.empty();

    private Optional<Id<Person>> matsimAgentId = Optional.empty();
    private Optional<Double> matsimEnterTime = Optional.empty();
    private Optional<Double> matsimExitTime = Optional.empty();

    private Optional<Double> matsimCausedDelay = Optional.empty();


    public CongestionMatchingInfo(Link link, Id<Person> gpsAgentId, double gpsEnterTime) {
        this.link = link;
        this.gpsAgentId = gpsAgentId;
        this.gpsEnterTime = gpsEnterTime;
    }

    public Link getLink() {
        return link;
    }

    public Id<Person> getGpsAgentId() {
        return gpsAgentId;
    }

    public double getGpsEnterTime() {
        return gpsEnterTime;
    }

    public Optional<Double> getGpsExitTime() {
        return gpsExitTime;
    }

    public Optional<Id<Person>> getMatsimAgentId() {
        return matsimAgentId;
    }

    public Optional<Double> getMatsimEnterTime() {
        return matsimEnterTime;
    }

    public Optional<Double> getMatsimExitTime() {
        return matsimExitTime;
    }

    public Optional<Double> getMatsimCausedDelay() {
        return matsimCausedDelay;
    }

    public void setGpsExitTime(Optional<Double> gpsExitTime) {
        this.gpsExitTime = gpsExitTime;
    }

    public void setMatsimAgentId(Optional<Id<Person>> matsimAgentId) {
        this.matsimAgentId = matsimAgentId;
    }

    public void setMatsimEnterTime(Optional<Double> matsimEnterTime) {
        this.matsimEnterTime = matsimEnterTime;
    }

    public void setMatsimExitTime(Optional<Double> matsimExitTime) {
        this.matsimExitTime = matsimExitTime;
    }

    public void setMatsimCausedDelay(Optional<Double> matsimCausedDelay) {
        this.matsimCausedDelay = matsimCausedDelay;
    }

    public double computeCausedDelay() {
        if (hasAllValuesSet()) {
            double gpsTravelTime = this.gpsExitTime.get() - this.gpsEnterTime;
            double matsimTravelTime = this.matsimExitTime.get() - this.matsimEnterTime.get();
            return this.matsimCausedDelay.get() * delayScalingFactor(this.link, gpsTravelTime, matsimTravelTime);
        }
        return 0.0;
    }

    public double computeExperiencedDelay() {
        if (hasAllValuesSet()) {
            double gpsTravelTime = this.gpsExitTime.get() - this.gpsEnterTime;
            double freeflowTravelTime = Math.floor(this.link.getLength() / this.link.getFreespeed()) + 1.0;
            if (gpsTravelTime > freeflowTravelTime) {
                return gpsTravelTime - freeflowTravelTime;
            }
            return 0.0;
        }
        return 0.0;
    }

    private static double delayScalingFactor(Link link, double gpsTravelTime, double scenarioTravelTime) {
        return (gpsTravelTime) / (scenarioTravelTime);
    }

    public boolean hasAllValuesSet() {
        return (this.matsimAgentId.isPresent()
                && this.gpsExitTime.isPresent()
                && this.matsimExitTime.isPresent()
                && this.matsimCausedDelay.isPresent());
    }
}
