package ethz.ivt.externalities;

import ethz.ivt.externalities.counters.ExternalityCounter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.EventsManagerImpl;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class MockMeasureExternalities implements MeasureExternalitiesInterface {

    private final Scenario scenario;

    public MockMeasureExternalities(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public void reset() {

    }

    @Override
    public ExternalityCounter process(List<Event> events, LocalDateTime date) {
        return null;
    }

    @Override
    public ExternalityCounter process(String events, LocalDateTime date) {
        return new ExternalityCounter(scenario, new EventsManagerImpl());
    }

    @Override
    public void write(Path outputFolder) {

    }
}
