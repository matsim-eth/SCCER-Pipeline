package ethz.ivt.externalities.data;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class AggregateDataPerTimeTest {

    @Test
    public void testConstructor() {

        Fixture fixture = new Fixture();
        fixture.init();

        List<String> att = new LinkedList<>();
        att.add("count");
        att.add("delay");

        AggregateDataPerTimeImpl<Link> acd1 = new AggregateDataPerTimeImpl<Link>(3600.0, att, null, Link.class);
        assertEquals("Incorrect number of bins!",30, acd1.getNumBins());
        assertEquals("Incorrect bin size!",3600.0, acd1.getBinSize(), 0.0);

        AggregateDataPerTimeImpl<Link> acd2 = new AggregateDataPerTimeImpl<Link>(7200.0, att, null, Link.class);
        assertEquals("Incorrect number of bins!", 15, acd2.getNumBins());
        assertEquals("Incorrect bin size!", 7200.0, acd2.getBinSize(), 0.0);

    }

    @Test
    public void testSetAndGetValues() {
        Fixture fixture = new Fixture();
        fixture.init();

        List<String> att = new LinkedList<>();
        att.add("count");
        att.add("delay");

        AggregateDataPerTimeImpl<Link> acd = new AggregateDataPerTimeImpl<Link>(3600.0, att, null, Link.class);

        // link exists
        Id<Link> linkId1 = Id.create("0",Link.class);
        acd.setValue(linkId1, 1, "count", 50.0);
        acd.setValue(linkId1, 1, "delay", 100.0);
        assertEquals("Wrong count!", 50.0, acd.getValue(linkId1, 1, "count"), 0.0);
        assertEquals("Wrong delay!", 100.0, acd.getValue(linkId1, 1, "delay"), 0.0);

        // link does not exist
        Id<Link> linkId2 = Id.create("10",Link.class);
    //    acd.setValue(linkId2, 1, "count", 50.0);
    //    acd.setValue(linkId2, 1, "delay", 100.0);
        assertEquals("Wrong count!", 0.0, acd.getValue(linkId2, 1, "count"), 0.0);
        assertEquals("Wrong delay!", 0.0, acd.getValue(linkId2, 1, "delay"), 0.0);

        // attribute does not exist
        acd.setValue(linkId1, 1, "aaa", 50.0);
        acd.setValue(linkId1, 1, "bbb", 100.0);
        assertEquals("Wrong count!", 0.0, acd.getValue(linkId1, 1, "aaa"), 0.0);
        assertEquals("Wrong delay!", 0.0, acd.getValue(linkId1, 1, "bbb"), 0.0);

        // time bin does not exist
        acd.setValue(linkId1, acd.getNumBins(), "count", 50.0);
        acd.setValue(linkId1, acd.getNumBins(), "delay", 100.0);
        assertEquals("Wrong count!", 0.0, acd.getValue(linkId1, 100, "count"), 0.0);
        assertEquals("Wrong delay!", 0.0, acd.getValue(linkId1, 100, "delay"), 0.0);

        // time bin does not exist
        acd.setValue(linkId1, acd.getNumBins() + 100, "count", 50.0);
        acd.setValue(linkId1, acd.getNumBins() + 100, "delay", 100.0);
        assertEquals("Wrong count!", 0.0, acd.getValue(linkId1, 100, "count"), 0.0);
        assertEquals("Wrong delay!", 0.0, acd.getValue(linkId1, 100, "delay"), 0.0);
    }

    @Test
    public void testAddValues() {
        Fixture fixture = new Fixture();
        fixture.init();

        List<String> att = new LinkedList<>();
        att.add("count");
        att.add("delay");

        AggregateDataPerTimeImpl<Link> acd = new AggregateDataPerTimeImpl<Link>(3600.0, att, null, Link.class);

        // link exists
        Id<Link> linkId1 = Id.create("0",Link.class);
        acd.addValue(linkId1, 1, "count", 50.0);
        acd.addValue(linkId1, 1, "delay", 100.0);
        assertEquals("Wrong count!", 50.0, acd.getValue(linkId1, 1, "count"), 0.0);
        assertEquals("Wrong delay!", 100.0, acd.getValue(linkId1, 1, "delay"), 0.0);

        acd.addValue(linkId1, 1, "count", 50.0);
        acd.addValue(linkId1, 1, "delay", 100.0);
        assertEquals("Wrong count!", 100.0, acd.getValue(linkId1, 1, "count"), 0.0);
        assertEquals("Wrong delay!", 200.0, acd.getValue(linkId1, 1, "delay"), 0.0);
    }


    @Test
    public void testWriteAndReadCSV() {
        Fixture fixture = new Fixture();
        fixture.init();

        List<String> att = new LinkedList<>();
        att.add("count");
        att.add("delay");

        String outputFile = "aggregate_delay_per_link_per_time.csv";

        AggregateDataPerTimeImpl<Link> acdToWrite = new AggregateDataPerTimeImpl<Link>(3600.0, att, outputFile, Link.class);

        Random randomGenerator = new Random();
        for (Id<Link> lid : fixture.network.getLinks().keySet()) {
            for (int i = 0; i < 30; i++) {
                acdToWrite.setValue(lid, i,"count", randomGenerator.nextInt(100));
                acdToWrite.setValue(lid, i,"delay", randomGenerator.nextInt(100));
            }
        }

        acdToWrite.writeDataToCsv("");

        AggregateDataPerTimeImpl<Link> acdToRead = new AggregateDataPerTimeImpl<Link>(3600.0, att, null, Link.class);
        acdToRead.loadDataFromCsv(outputFile);

        for (Id<Link> linkId : acdToWrite.getData().keySet()) {
            for (int bin = 0; bin < 30; bin++) {
                double writtenCountValue = acdToWrite.getData().get(linkId).get("count")[bin];
                double writtenDelayValue = acdToWrite.getData().get(linkId).get("delay")[bin];
                double readCountValue = acdToRead.getData().get(linkId).get("count")[bin];
                double readDelayValue = acdToRead.getData().get(linkId).get("delay")[bin];
                assertEquals("Read count value does not match written count value!", writtenCountValue, readCountValue, 0.0);
                assertEquals("Read delay value does not match written delay value!", writtenDelayValue, readDelayValue, 0.0);
            }
        }
    }

}