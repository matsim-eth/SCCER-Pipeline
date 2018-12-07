package ethz.ivt.externalities.counters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
