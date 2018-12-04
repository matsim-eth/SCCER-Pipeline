package ethz.ivt.externalities.counters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.Optional;
import java.util.OptionalDouble;

public class CongestionMatchingInfo {

    private final Link link;
    private final Id<Person> gpsAgentId;
    private final double gpsEnterTime;
    private Optional<Double> gpsExitTime = Optional.empty();

    private Optional<Id<Person>> matsimAgentId = Optional.empty();
    private Optional<Double> matsimEnterTime = Optional.empty();
    private Optional<Double> matsimExitTime = Optional.empty();

    private Optional<Double> matsimCausedDelay = Optional.empty();

    private OptionalDouble gpsCausedDelay = OptionalDouble.empty();
    private OptionalDouble gpsExperiencedDelay = OptionalDouble.empty();

    private boolean tempValuesReadyToBeUpdated = false;
    private boolean tempValuesUpdated = false;


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

    public OptionalDouble getGpsCausedDelay() {
        return gpsCausedDelay;
    }

    public OptionalDouble getGpsExperiencedDelay() {
        return gpsExperiencedDelay;
    }

    public boolean isTempValuesReadyToBeUpdated() {
        return tempValuesReadyToBeUpdated;
    }

    public boolean isTempValuesUpdated() {
        return tempValuesUpdated;
    }

    public void setGpsExitTime(Optional<Double> gpsExitTime) {
        this.gpsExitTime = gpsExitTime;
        if (!this.gpsExperiencedDelay.isPresent()) {
            this.gpsExperiencedDelay = this.computeExperiencedDelay();
        }
        if (!this.gpsCausedDelay.isPresent()) {
            this.gpsCausedDelay = this.computeCausedDelay();
        }
        this.tempValuesReadyToBeUpdated = (this.gpsExitTime.isPresent()
                && this.gpsCausedDelay.isPresent()
                && this.gpsExperiencedDelay.isPresent()
                && this.matsimEnterTime.isPresent()
                && this.matsimExitTime.isPresent()
                && this.matsimCausedDelay.isPresent());
    }

    public void setMatsimAgentId(Optional<Id<Person>> matsimAgentId) {
        this.matsimAgentId = matsimAgentId;
        this.tempValuesReadyToBeUpdated = (this.gpsExitTime.isPresent()
                && this.gpsCausedDelay.isPresent()
                && this.gpsExperiencedDelay.isPresent()
                && this.matsimEnterTime.isPresent()
                && this.matsimExitTime.isPresent()
                && this.matsimCausedDelay.isPresent());
    }

    public void setMatsimEnterTime(Optional<Double> matsimEnterTime) {
        this.matsimEnterTime = matsimEnterTime;
        this.tempValuesReadyToBeUpdated = (this.gpsExitTime.isPresent()
                && this.gpsCausedDelay.isPresent()
                && this.gpsExperiencedDelay.isPresent()
                && this.matsimEnterTime.isPresent()
                && this.matsimExitTime.isPresent()
                && this.matsimCausedDelay.isPresent());
    }

    public void setMatsimExitTime(Optional<Double> matsimExitTime) {
        this.matsimExitTime = matsimExitTime;
        if (!this.gpsExperiencedDelay.isPresent()) {
            this.gpsExperiencedDelay = this.computeExperiencedDelay();
        }
        if (!this.gpsCausedDelay.isPresent()) {
            this.gpsCausedDelay = this.computeCausedDelay();
        }

        this.tempValuesReadyToBeUpdated = (this.gpsExitTime.isPresent()
                && this.gpsCausedDelay.isPresent()
                && this.gpsExperiencedDelay.isPresent()
                && this.matsimEnterTime.isPresent()
                && this.matsimExitTime.isPresent()
                && this.matsimCausedDelay.isPresent());
    }

    public void setMatsimCausedDelay(Optional<Double> matsimCausedDelay) {
        this.matsimCausedDelay = matsimCausedDelay;
        if (!this.gpsExperiencedDelay.isPresent()) {
            this.gpsExperiencedDelay = this.computeExperiencedDelay();
        }
        if (!this.gpsCausedDelay.isPresent()) {
            this.gpsCausedDelay = this.computeCausedDelay();
        }

        this.tempValuesReadyToBeUpdated = (this.gpsExitTime.isPresent()
                && this.gpsCausedDelay.isPresent()
                && this.gpsExperiencedDelay.isPresent()
                && this.matsimEnterTime.isPresent()
                && this.matsimExitTime.isPresent()
                && this.matsimCausedDelay.isPresent());
    }

    public void setGpsCausedDelay(OptionalDouble gpsCausedDelay) {
        this.gpsCausedDelay = gpsCausedDelay;
    }

    public void setGpsExperiencedDelay(OptionalDouble gpsExperiencedDelay) {
        this.gpsExperiencedDelay = gpsExperiencedDelay;
    }

    public void setTempValuesUpdated(boolean tempValuesUpdated) {
        this.tempValuesUpdated = tempValuesUpdated;
    }

    private OptionalDouble computeCausedDelay() {
        if (this.gpsExitTime.isPresent()
                && this.matsimEnterTime.isPresent()
                && this.matsimExitTime.isPresent()
                && this.matsimCausedDelay.isPresent()) {
            double gpsTravelTime = this.gpsExitTime.get() - this.gpsEnterTime;
            double matsimTravelTime = this.matsimExitTime.get() - this.matsimEnterTime.get();
            return OptionalDouble.of(this.matsimCausedDelay.get() * delayScalingFactor(this.link, gpsTravelTime, matsimTravelTime));
        }
        return OptionalDouble.empty();
    }

    private OptionalDouble computeExperiencedDelay() {
        if (this.gpsExitTime.isPresent()) {
            double gpsTravelTime = this.gpsExitTime.get() - this.gpsEnterTime;
            double freeflowTravelTime = Math.floor(this.link.getLength() / this.link.getFreespeed()) + 1.0;
            if (gpsTravelTime > freeflowTravelTime) {
                return OptionalDouble.of(gpsTravelTime - freeflowTravelTime);
            }
            return OptionalDouble.of(0.0);
        }
        return OptionalDouble.empty();
    }

    private static double delayScalingFactor(Link link, double gpsTravelTime, double scenarioTravelTime) {
        return (gpsTravelTime) / (scenarioTravelTime);
    }

}
