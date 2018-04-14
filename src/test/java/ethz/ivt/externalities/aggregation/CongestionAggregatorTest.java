package ethz.ivt.externalities.aggregation;

import ethz.ivt.vsp.CongestionEvent;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;

import static org.junit.Assert.*;

public class CongestionAggregatorTest {

    @Test
    public void handleCongestionEvent() {
        Fixture fixture = new Fixture();
        fixture.init();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        CongestionAggregator ca = new CongestionAggregator(fixture.scenario,v2deh);

        // create events on single link
        Id<Link> linkId = Id.create("0",Link.class);
        Id<Person> causingId = Id.createPersonId("0");
        Id<Person> affectedId = Id.createPersonId("1");
        CongestionEvent ce1 = new CongestionEvent(0.0, null,
                causingId, affectedId,1.0, linkId, 1.0);
        CongestionEvent ce2 = new CongestionEvent(0.0, null,
                causingId, affectedId,1.0, linkId, 1.0);
        CongestionEvent ce3 = new CongestionEvent(7220.0, null,
                causingId, affectedId,1.0, linkId, 7220.0);

        // handle events
        ca.handleEvent(ce1);
        assertEquals("Incorrect link delay value!",1.0,ca.aggregateCongestionDataPerLinkPerTime.getData().get(linkId).get("delay")[0],0.0);
        assertEquals("Incorrect delay experienced value!",1.0,ca.aggregateCongestionDataPerPersonPerTime.getData().get(affectedId).get("delay_experienced")[0],0.0);
        assertEquals("Incorrect delay caused value!",1.0,ca.aggregateCongestionDataPerPersonPerTime.getData().get(causingId).get("delay_caused")[0],0.0);

        ca.handleEvent(ce2);
        assertEquals("Delay values not added correctly!",2.0,ca.aggregateCongestionDataPerLinkPerTime.getData().get(linkId).get("delay")[0],0.0);
        assertEquals("Delay experienced value not added correctly!",2.0,ca.aggregateCongestionDataPerPersonPerTime.getData().get(affectedId).get("delay_experienced")[0],0.0);
        assertEquals("Delay caused value not added correctly!",2.0,ca.aggregateCongestionDataPerPersonPerTime.getData().get(causingId).get("delay_caused")[0],0.0);

        ca.handleEvent(ce3);
        assertEquals("Delay value not entered in correct bin!", 1.0,ca.aggregateCongestionDataPerLinkPerTime.getData().get(linkId).get("delay")[2],0.0);
        assertEquals("Delay experienced value not entered in correct bin!",1.0,ca.aggregateCongestionDataPerPersonPerTime.getData().get(affectedId).get("delay_experienced")[2],0.0);
        assertEquals("Delay caused value not entered in correct bin!",1.0,ca.aggregateCongestionDataPerPersonPerTime.getData().get(causingId).get("delay_caused")[2],0.0);
    }

    @Test
    public void testTwoPeopleEnterAndLeaveSameLink() {
        Fixture fixture = new Fixture();
        fixture.init();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        CongestionAggregator ca = new CongestionAggregator(fixture.scenario,v2deh);

        // create events on single link
        Id<Link> linkId0 = Id.create("0",Link.class);
        Id<Person> pid1 = Id.createPersonId("0");
        Id<Person> pid2 = Id.createPersonId("1");
        Id<Vehicle> vid1 = Id.createVehicleId(pid1);
        Id<Vehicle> vid2 = Id.createVehicleId(pid2);

        // two people enter and leave same link

        // vehicle entering link within time span, i.e count + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(0.0, pid1, linkId0, vid1, "car", 0.0));
        LinkEnterEvent lee1 = new LinkEnterEvent(  0.0, vid1, linkId0);
        ca.handleEvent(lee1);
        assertEquals("First person enters link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId0,0,"count"),0.0);

        // same vehicle exiting link within time span, i.e count does not change
        LinkLeaveEvent lle1 = new LinkLeaveEvent(100.0, vid1, linkId0);
        ca.handleEvent(lle1);
        assertEquals("First person leaves link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId0,0,"count"),0.0);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(100.0, pid1, linkId0, vid1, "car", 0.0));

        // new vehicle on link within time span, i.e. count + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(100.0, pid2, linkId0, vid2, "car", 0.0));
        LinkEnterEvent lee2 = new LinkEnterEvent(100.0, vid2, linkId0);
        ca.handleEvent(lee2);
        assertEquals("Second person enters link: Incorrect count value!", 2.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId0,0,"count"),0.0);

        // same vehicle on link within time span, i.e count does not change
        LinkLeaveEvent lle2 = new LinkLeaveEvent(200.0, vid2, linkId0);
        ca.handleEvent(lle2);
        assertEquals("Second person leaves link: Incorrect count value!", 2.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId0,0,"count"),0.0);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(200.0, pid2, linkId0, vid2, "car", 0.0));

    }

    @Test
    public void testLeaveFirstLinkAndEnterSecondLink() {
        Fixture fixture = new Fixture();
        fixture.init();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        CongestionAggregator ca = new CongestionAggregator(fixture.scenario,v2deh);

        // create events on single link
        Id<Link> linkId0 = Id.create("0",Link.class);
        Id<Link> linkId1 = Id.create("1",Link.class);
        Id<Person> pid1 = Id.createPersonId("0");
        Id<Vehicle> vid1 = Id.createVehicleId(pid1);

        // person enter and exits two links

        // vehicle enters link in previous time bin, i.e count[bin-1] + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(0.0, pid1, linkId0, vid1, "car", 0.0));
        LinkEnterEvent lee1 = new LinkEnterEvent(  0.0, vid1, linkId0);
        ca.handleEvent(lee1);
        assertEquals("Person enters first link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId0,0,"count"),0.0);

        // vehicle leaves link in current time bin, i.e. count[bin] + 1
        LinkLeaveEvent lle1 = new LinkLeaveEvent(3650.0, vid1, linkId0);
        ca.handleEvent(lle1);
        assertEquals("Person leaves first link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId0,1,"count"),0.0);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(3650.0, pid1, linkId0, vid1, "car", 0.0));

        // vehicle enters new link within same time bin, i.e. count[bin] + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(3650.0, pid1, linkId1, vid1, "car", 0.0));
        LinkEnterEvent lee2 = new LinkEnterEvent(3650.0, vid1, linkId1);
        ca.handleEvent(lee2);
        assertEquals("Person enters second link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId1,1,"count"),0.0);

        // same vehicle leaves link within same time bin, i.e count does not change
        LinkLeaveEvent lle2 = new LinkLeaveEvent(4000.0, vid1, linkId1);
        ca.handleEvent(lle2);
        assertEquals("Person leaves second link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId0,1,"count"),0.0);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(4000.0, pid1, linkId0, vid1, "car", 0.0));

    }

    @Test
    public void testVehicleReentersLinkLaterInSameTimeBin() {
        Fixture fixture = new Fixture();
        fixture.init();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        CongestionAggregator ca = new CongestionAggregator(fixture.scenario,v2deh);

        // create events on single link
        Id<Link> linkId = Id.create("0",Link.class);
        Id<Person> pid = Id.createPersonId("0");
        Id<Vehicle> vid = Id.createVehicleId(pid);

        // person enters, exits, reenters and reexits link within same time bin

        // vehicle enters link, i.e. count[bin] + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(0.0, pid, linkId, vid, "car", 0.0));
        LinkEnterEvent lee1 = new LinkEnterEvent(  0.0, vid, linkId);
        ca.handleEvent(lee1);
        assertEquals("Person enters link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId,0,"count"),0.0);

        // vehicle leaves link, i.e. count does not change
        LinkLeaveEvent lle1 = new LinkLeaveEvent(100.0, vid, linkId);
        ca.handleEvent(lle1);
        assertEquals("Person exits link: Incorrect count value!", 1.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId,0,"count"),0.0);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(100.0, pid, linkId, vid, "car", 0.0));

        // vehicle enters link, i.e. count[bin] + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(1000.0, pid, linkId, vid, "car", 0.0));
        LinkEnterEvent lee2 = new LinkEnterEvent(  1000.0, vid, linkId);
        ca.handleEvent(lee2);
        assertEquals("Person re-enters link: Incorrect count value!", 2.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId,0,"count"),0.0);

        // vehicle leaves link, i.e. count does not change
        LinkLeaveEvent lle2 = new LinkLeaveEvent(1100.0, vid, linkId);
        ca.handleEvent(lle2);
        assertEquals("Person re-exits link: Incorrect count value!", 2.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId,0,"count"),0.0);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(1100.0, pid, linkId, vid, "car", 0.0));
    }

    @Test
    public void CausedAndExperiencedDelayEqualsTotalDelay() {
        Fixture fixture = new Fixture();
        fixture.init();
        Vehicle2DriverEventHandler v2deh = new Vehicle2DriverEventHandler();
        CongestionAggregator ca = new CongestionAggregator(fixture.scenario,v2deh);

        // create events on single link
        Id<Link> linkId = Id.create("0",Link.class);
        Id<Person> pid1 = Id.createPersonId("0");
        Id<Vehicle> vid1 = Id.createVehicleId(pid1);
        Id<Person> pid2 = Id.createPersonId("1");
        Id<Vehicle> vid2 = Id.createVehicleId(pid2);

        // person 1 enters, person 2 enters, congestion event, person 1 exits, person 2 exits

        // vehicle 1 enters link, i.e. count[bin] + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(0.0, pid1, linkId, vid1, "car", 0.0));
        LinkEnterEvent lee1 = new LinkEnterEvent(  0.0, vid1, linkId);
        ca.handleEvent(lee1);

        // vehicle 2 enters link, i.e. count[bin] + 1
        v2deh.handleEvent(new VehicleEntersTrafficEvent(10.0, pid2, linkId, vid2, "car", 0.0));
        LinkEnterEvent lee2 = new LinkEnterEvent(  10.0, vid2, linkId);
        ca.handleEvent(lee2);

        // congestion event
        CongestionEvent ce = new CongestionEvent(50.0, null,
                pid1, pid2, 10.0, linkId, 1.0);
        ca.handleEvent(ce);

        // vehicle 1 leaves link, i.e. count does not change
        LinkLeaveEvent lle1 = new LinkLeaveEvent(100.0, vid1, linkId);
        ca.handleEvent(lle1);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(100.0, pid1, linkId, vid1, "car", 0.0));

        // vehicle 2 leaves link, i.e. count does not change
        LinkLeaveEvent lle2 = new LinkLeaveEvent(110.0, vid2, linkId);
        ca.handleEvent(lle2);
        v2deh.handleEvent(new VehicleLeavesTrafficEvent(110.0, pid2, linkId, vid2, "car", 0.0));

        assertEquals("Incorrect count value!", 2.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId,0,"count"),0.0);
        assertEquals("Incorrect congestion value!", 10.0, ca.aggregateCongestionDataPerLinkPerTime.getValue(linkId, 0, "delay"), 0.0);
        double totalDelay = 0.0;
        double totalCausedDelay = 0.0;
        double totalExperiencedDelay = 0.0;
        for (Id<Link> lid : ca.aggregateCongestionDataPerLinkPerTime.getData().keySet()) {
            for (int bin = 0; bin < 30; bin++) {
                totalDelay += ca.aggregateCongestionDataPerLinkPerTime.getValue(lid, bin, "delay");
            }
        }
        for (Id<Person> pid : ca.aggregateCongestionDataPerPersonPerTime.getData().keySet()) {
            for (int bin = 0; bin < 30; bin++) {
                totalCausedDelay += ca.aggregateCongestionDataPerPersonPerTime.getValue(pid, bin, "delay_caused");
                totalExperiencedDelay += ca.aggregateCongestionDataPerPersonPerTime.getValue(pid, bin, "delay_experienced");
            }
        }
        assertEquals("Total delay != total caused delay!", 0.0, totalDelay-totalCausedDelay, 0.0);
        assertEquals("Total delay != total experienced delay!", 0.0, totalDelay-totalExperiencedDelay, 0.0);
    }
}