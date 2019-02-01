package ethz.ivt.externalities.counters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.handler.EventHandler;

public class PublicTransportExternalityCounter implements ActivityEndEventHandler, ActivityStartEventHandler, EventHandler {

	private static final Logger log = Logger.getLogger(PublicTransportExternalityCounter.class);
	private ExternalityCounter externalityCounterDelegate;
	private Scenario scenario;

	public PublicTransportExternalityCounter(Scenario scenario, ExternalityCounter externalityCounterDelegate) {
		this.scenario = scenario;
		this.externalityCounterDelegate = externalityCounterDelegate;
		//    initializeFields(); //JM'18 - fields are now added dynamically during operation.
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		"pt_interaction".equals(event.getEventType());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

	}
}
