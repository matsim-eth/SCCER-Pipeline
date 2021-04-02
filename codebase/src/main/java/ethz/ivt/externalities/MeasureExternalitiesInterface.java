package ethz.ivt.externalities;

import ethz.ivt.externalities.counters.ExternalityCounter;
import org.matsim.api.core.v01.events.Event;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public interface MeasureExternalitiesInterface {
    void reset();

    ExternalityCounter process(List<Event> events, LocalDateTime date);

    ExternalityCounter process(String events, LocalDateTime date);

    void write(Path outputFolder);
}
