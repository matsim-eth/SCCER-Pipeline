package ethz.ivt.externalities.data;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class AggregateCongestionDataPerPersonPerTimeTest {

    @Test
    public void testConstructor() {

        Fixture fixture = new Fixture();
        fixture.init();

        AggregateCongestionDataPerPersonPerTime acd1 = new AggregateCongestionDataPerPersonPerTime(fixture.scenario, 3600.0);
        assertEquals("Incorrect number of bins!",30, acd1.getNumBins());
        assertEquals("Incorrect bin size!",3600.0, acd1.getBinSize(), 0.0);

        AggregateCongestionDataPerPersonPerTime acd2 = new AggregateCongestionDataPerPersonPerTime(fixture.scenario, 7200.0);
        assertEquals("Incorrect number of bins!", 15, acd2.getNumBins());
        assertEquals("Incorrect bin size!", 7200.0, acd2.getBinSize(), 0.0);

        for (Id<Person> pid : fixture.population.getPersons().keySet()) {
            assertEquals("Delay experienced array for person " + pid + " in data map has incorrect number of elements!",30, acd1.getData().get(pid).get("delay_experienced").length);
            assertEquals("Delay caused array for person " + pid + " in data map has incorrect number of elements!",30, acd1.getData().get(pid).get("delay_caused").length);
        }
    }

    @Test
    public void testSetAndGetValues() {
        Fixture fixture = new Fixture();
        fixture.init();
        AggregateCongestionDataPerPersonPerTime acd = new AggregateCongestionDataPerPersonPerTime(fixture.scenario, 3600.0);

        // person exists
        Id<Person> personId1 = Id.createPersonId("0");
        acd.setValue(personId1, 1, "delay_experienced", 50.0);
        acd.setValue(personId1, 1, "delay_caused", 100.0);
        assertEquals("Wrong delay experienced!", 50.0, acd.getValue(personId1, 1, "delay_experienced"), 0.0);
        assertEquals("Wrong delay caused!", 100.0, acd.getValue(personId1, 1, "delay_caused"), 0.0);

        // link does not exist
        Id<Person> personId2 = Id.createPersonId("10");;
        acd.setValue(personId2, 1, "delay_experienced", 50.0);
        acd.setValue(personId2, 1, "delay_caused", 100.0);
        assertEquals("Wrong delay experienced!", 0.0, acd.getValue(personId2, 1, "delay_experienced"), 0.0);
        assertEquals("Wrong delay caused!", 0.0, acd.getValue(personId2, 1, "delay_caused"), 0.0);

        // attribute does not exist
        acd.setValue(personId1, 1, "aaa", 50.0);
        acd.setValue(personId1, 1, "bbb", 100.0);
        assertEquals("Wrong delay experienced!", 0.0, acd.getValue(personId1, 1, "aaa"), 0.0);
        assertEquals("Wrong delay caused!", 0.0, acd.getValue(personId1, 1, "bbb"), 0.0);

        // time bin does not exist
        acd.setValue(personId1, acd.getNumBins(), "delay_experienced", 50.0);
        acd.setValue(personId1, acd.getNumBins(), "delay_caused", 100.0);
        assertEquals("Wrong delay experienced!", 0.0, acd.getValue(personId1, 100, "delay_experienced"), 0.0);
        assertEquals("Wrong delay caused!", 0.0, acd.getValue(personId1, 100, "delay_caused"), 0.0);

        // time bin does not exist
        acd.setValue(personId1, acd.getNumBins() + 100, "delay_experienced", 50.0);
        acd.setValue(personId1, acd.getNumBins() + 100, "delay_caused", 100.0);
        assertEquals("Wrong delay experienced!", 0.0, acd.getValue(personId1, 100, "delay_experienced"), 0.0);
        assertEquals("Wrong delay caused!", 0.0, acd.getValue(personId1, 100, "delay_caused"), 0.0);
    }

    @Test
    public void testAddValues() {
        Fixture fixture = new Fixture();
        fixture.init();
        AggregateCongestionDataPerPersonPerTime acd = new AggregateCongestionDataPerPersonPerTime(fixture.scenario, 3600.0);

        // link exists
        Id<Person> personId1 = Id.createPersonId("0");
        acd.addValue(personId1, 1, "delay_experienced", 50.0);
        acd.addValue(personId1, 1, "delay_caused", 100.0);
        assertEquals("Wrong delay experienced!", 50.0, acd.getValue(personId1, 1, "delay_experienced"), 0.0);
        assertEquals("Wrong delay caused!", 100.0, acd.getValue(personId1, 1, "delay_caused"), 0.0);

        acd.addValue(personId1, 1, "delay_experienced", 50.0);
        acd.addValue(personId1, 1, "delay_caused", 100.0);
        assertEquals("Wrong delay experienced!", 100.0, acd.getValue(personId1, 1, "delay_experienced"), 0.0);
        assertEquals("Wrong delay caused!", 200.0, acd.getValue(personId1, 1, "delay_caused"), 0.0);
    }


    @Test
    public void testWriteAndReadCSV() {
        Fixture fixture = new Fixture();
        fixture.init();
        AggregateCongestionDataPerPersonPerTime acdToWrite = new AggregateCongestionDataPerPersonPerTime(fixture.scenario, 3600.0);

        Random randomGenerator = new Random();
        for (Id<Person> pid : fixture.population.getPersons().keySet()) {
            for (int i = 0; i < 30; i++) {
                acdToWrite.getData().get(pid).get("delay_experienced")[i] = randomGenerator.nextDouble();
                acdToWrite.getData().get(pid).get("delay_caused")[i] = randomGenerator.nextDouble();
            }
        }

        acdToWrite.writeDataToCsv("");

        AggregateCongestionDataPerPersonPerTime acdToRead = new AggregateCongestionDataPerPersonPerTime(fixture.scenario, 3600.0);
        acdToRead.loadDataFromCsv("aggregate_delay_per_person_per_time.csv");

        for (Id<Person> personId : acdToWrite.getData().keySet()) {
            for (int bin = 0; bin < 30; bin++) {
                double writtenCountValue = acdToWrite.getData().get(personId).get("delay_experienced")[bin];
                double writtenDelayValue = acdToWrite.getData().get(personId).get("delay_caused")[bin];
                double readCountValue = acdToRead.getData().get(personId).get("delay_experienced")[bin];
                double readDelayValue = acdToRead.getData().get(personId).get("delay_caused")[bin];
                assertEquals("Read delay experienced value does not match written value!", writtenCountValue, readCountValue, 0.0);
                assertEquals("Read delay caused value does not match written value!", writtenDelayValue, readDelayValue, 0.0);
            }
        }
    }

}