package ethz.ivt.externalities.data.congestion.writer;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import ethz.ivt.externalities.data.congestion.reader.CSVCongestionPerLinkPerTimeReader;
import ethz.ivt.externalities.data.congestion.reader.CSVCongestionPerPersonPerTimeReader;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.io.IOException;
import java.util.*;


public class CongestionWriterTest {

    @Test
    public void WritePerLink() throws IOException {
        double binSize = 3600.;

        Map<Id<Link>, CongestionPerTime> expectedMap = new HashMap<>();

        Id<Link> linkId = Id.createLinkId("link");
        Collection<Id<Link>> collection = new HashSet<>();
        collection.add(linkId);

        expectedMap.put(linkId, new CongestionPerTime(binSize));

        Random random = new Random(0);

        for (int bin=0; bin<expectedMap.get(linkId).getNumBins(); bin++) {
            expectedMap.get(linkId).setCountAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setDelayCausedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setDelayExperiencedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setCongestionCausedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setCongestionExperiencedAtTimeBin(random.nextDouble(), bin);
        }

        new CSVCongestionPerLinkPerTimeWriter(expectedMap).write("./src/test/java/ethz/ivt/externalities/data/congestion.csv");


        Map<Id<Link>, CongestionPerTime> actualMap = CSVCongestionPerLinkPerTimeReader.read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", binSize);

        for (int bin=0; bin<expectedMap.get(linkId).getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", expectedMap.get(linkId).getCountAtTimeBin(bin), actualMap.get(linkId).getCountAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay caused does not match", expectedMap.get(linkId).getDelayCausedAtTimeBin(bin), actualMap.get(linkId).getDelayCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay experienced does not match",expectedMap.get(linkId).getDelayExperiencedAtTimeBin(bin), actualMap.get(linkId).getDelayExperiencedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion caused does not match",expectedMap.get(linkId).getCongestionCausedAtTimeBin(bin), actualMap.get(linkId).getCongestionCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion experienced does not match",expectedMap.get(linkId).getCongestionExperiencedAtTimeBin(bin), actualMap.get(linkId).getCongestionExperiencedAtTimeBin(bin), 0.0);
        }
    }

    @Test
    public void WritePerPerson() throws IOException {
        double binSize = 3600.;

        Map<Id<Person>, CongestionPerTime> expectedMap = new HashMap<>();

        Id<Person> person = Id.createPersonId("person");
        Collection<Id<Person>> collection = new HashSet<>();
        collection.add(person);

        expectedMap.put(person, new CongestionPerTime(binSize));

        Random random = new Random(1);

        for (int bin=0; bin<expectedMap.get(person).getNumBins(); bin++) {
            expectedMap.get(person).setCountAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(person).setDelayCausedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(person).setDelayExperiencedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(person).setCongestionCausedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(person).setCongestionExperiencedAtTimeBin(random.nextDouble(), bin);
        }

        new CSVCongestionPerPersonPerTimeWriter(expectedMap).write("./src/test/java/ethz/ivt/externalities/data/congestion.csv");


        Map<Id<Person>, CongestionPerTime> actualMap = new CSVCongestionPerPersonPerTimeReader(collection, binSize).read("./src/test/java/ethz/ivt/externalities/data/congestion.csv");

        for (int bin=0; bin<expectedMap.get(person).getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", expectedMap.get(person).getCountAtTimeBin(bin), actualMap.get(person).getCountAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay caused does not match", expectedMap.get(person).getDelayCausedAtTimeBin(bin), actualMap.get(person).getDelayCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay experienced does not match",expectedMap.get(person).getDelayExperiencedAtTimeBin(bin), actualMap.get(person).getDelayExperiencedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion caused does not match",expectedMap.get(person).getCongestionCausedAtTimeBin(bin), actualMap.get(person).getCongestionCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion experienced does not match",expectedMap.get(person).getCongestionExperiencedAtTimeBin(bin), actualMap.get(person).getCongestionExperiencedAtTimeBin(bin), 0.0);
        }
    }

    @Test
    public void ReadPerLinkDifferentBinSize() throws IOException {
        double originalBinSize = 60.;
        double aggregationFactor = 60.;
        double value = 1.0;
        double newBinSize = aggregationFactor * originalBinSize;

        Map<Id<Link>, CongestionPerTime> inputMap = new HashMap<>();

        Id<Link> linkId = Id.createLinkId("link");
        Collection<Id<Link>> collection = new HashSet<>();
        collection.add(linkId);

        inputMap.put(linkId, new CongestionPerTime(originalBinSize));

        for (int bin=0; bin<inputMap.get(linkId).getNumBins(); bin++) {
            inputMap.get(linkId).setCountAtTimeBin(value, bin);
            inputMap.get(linkId).setDelayCausedAtTimeBin(value, bin);
            inputMap.get(linkId).setDelayExperiencedAtTimeBin(value, bin);
            inputMap.get(linkId).setCongestionCausedAtTimeBin(value, bin);
            inputMap.get(linkId).setCongestionExperiencedAtTimeBin(value, bin);
        }

        new CSVCongestionPerLinkPerTimeWriter(inputMap).write("./src/test/java/ethz/ivt/externalities/data/congestion.csv");


        Map<Id<Link>, CongestionPerTime> outputMap = CSVCongestionPerLinkPerTimeReader.read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", newBinSize);

        for (int bin=0; bin<outputMap.get(linkId).getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", value * aggregationFactor, outputMap.get(linkId).getCountAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay caused does not match", value * aggregationFactor, outputMap.get(linkId).getDelayCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay experienced does not match", value * aggregationFactor, outputMap.get(linkId).getDelayExperiencedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion caused does not match", value * aggregationFactor, outputMap.get(linkId).getCongestionCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion experienced does not match", value * aggregationFactor, outputMap.get(linkId).getCongestionExperiencedAtTimeBin(bin), 0.0);
        }
    }

    @Test
    public void WritePerLinkSparse() throws IOException {
        double binSize = 3600.;

        Map<Id<Link>, CongestionPerTime> expectedMap = new HashMap<>();

        Id<Link> linkId = Id.createLinkId("link");
        Collection<Id<Link>> collection = new HashSet<>();
        collection.add(linkId);

        expectedMap.put(linkId, new CongestionPerTime(binSize));

        int numBins = expectedMap.get(linkId).getNumBins();

        Random random = new Random(0);

        for (int bin=0; bin<expectedMap.get(linkId).getNumBins(); bin++) {
            expectedMap.get(linkId).setCountAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setDelayCausedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setDelayExperiencedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setCongestionCausedAtTimeBin(random.nextDouble(), bin);
            expectedMap.get(linkId).setCongestionExperiencedAtTimeBin(random.nextDouble(), bin);
        }


        // set half the entries to zero at random
        int n = (int) numBins / 2;
        for (int i=0; i<n; i++) {

            int bin = random.nextInt(numBins - 1);

            expectedMap.get(linkId).setCountAtTimeBin(0.0, bin);
            expectedMap.get(linkId).setDelayCausedAtTimeBin(0.0, bin);
            expectedMap.get(linkId).setDelayExperiencedAtTimeBin(0.0, bin);
            expectedMap.get(linkId).setCongestionCausedAtTimeBin(0.0, bin);
            expectedMap.get(linkId).setCongestionExperiencedAtTimeBin(0.0, bin);
        }

        new CSVCongestionPerLinkPerTimeWriter(expectedMap).write("./src/test/java/ethz/ivt/externalities/data/congestion.csv");


        Map<Id<Link>, CongestionPerTime> actualMap = CSVCongestionPerLinkPerTimeReader.read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", binSize);

        for (int bin=0; bin<expectedMap.get(linkId).getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", expectedMap.get(linkId).getCountAtTimeBin(bin), actualMap.get(linkId).getCountAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay caused does not match", expectedMap.get(linkId).getDelayCausedAtTimeBin(bin), actualMap.get(linkId).getDelayCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Delay experienced does not match",expectedMap.get(linkId).getDelayExperiencedAtTimeBin(bin), actualMap.get(linkId).getDelayExperiencedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion caused does not match",expectedMap.get(linkId).getCongestionCausedAtTimeBin(bin), actualMap.get(linkId).getCongestionCausedAtTimeBin(bin), 0.0);
            Assert.assertEquals("Congestion experienced does not match",expectedMap.get(linkId).getCongestionExperiencedAtTimeBin(bin), actualMap.get(linkId).getCongestionExperiencedAtTimeBin(bin), 0.0);
        }
    }

    @Test
    public void WriteAndReadMultipleLinks() throws IOException {
        double binSize = 3600.;

        Map<Id<Link>, CongestionPerTime> expectedMap = new HashMap<>();
        Collection<Id<Link>> idCollection = new HashSet<>();

        Random randomValueGenerator = new Random(0);
        Random randomZeroGenerator = new Random(1);

        for (int id = 0; id<4; id++) {
            Id<Link> linkId = Id.createLinkId("link_" + id);
            idCollection.add(linkId);
            expectedMap.put(linkId, new CongestionPerTime(binSize));

            for (int bin=0; bin<expectedMap.get(linkId).getNumBins(); bin++) {

                double probability = randomZeroGenerator.nextDouble();

                if (probability < 0.5) {
                    expectedMap.get(linkId).setCountAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setDelayCausedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setDelayExperiencedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setCongestionCausedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setCongestionExperiencedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                }
            }
        }

        // add one link somewhere with all zeros
        {
            Id<Link> linkId = Id.createLinkId("link_5");
            idCollection.add(linkId);
            expectedMap.put(linkId, new CongestionPerTime(binSize));
        }

        // then some more links
        for (int id = 6; id<10; id++) {
            Id<Link> linkId = Id.createLinkId("link_" + id);
            idCollection.add(linkId);
            expectedMap.put(linkId, new CongestionPerTime(binSize));

            for (int bin=0; bin<expectedMap.get(linkId).getNumBins(); bin++) {

                double probability = randomZeroGenerator.nextDouble();

                if (probability < 0.5) {
                    expectedMap.get(linkId).setCountAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setDelayCausedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setDelayExperiencedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setCongestionCausedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                    expectedMap.get(linkId).setCongestionExperiencedAtTimeBin(randomValueGenerator.nextDouble(), bin);
                }
            }
        }

        new CSVCongestionPerLinkPerTimeWriter(expectedMap).write("./src/test/java/ethz/ivt/externalities/data/congestion.csv");

        Map<Id<Link>, CongestionPerTime> actualMap = CSVCongestionPerLinkPerTimeReader.read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", binSize);

        for (Id<Link> linkId : idCollection) {
            for (int bin=0; bin<expectedMap.get(linkId).getNumBins(); bin++) {
                Assert.assertEquals("LinkId: " + linkId.toString() + ", Counts do not match", expectedMap.get(linkId).getCountAtTimeBin(bin), actualMap.get(linkId).getCountAtTimeBin(bin), 0.0);
                Assert.assertEquals("LinkId: " + linkId.toString() + ", Delay caused does not match", expectedMap.get(linkId).getDelayCausedAtTimeBin(bin), actualMap.get(linkId).getDelayCausedAtTimeBin(bin), 0.0);
                Assert.assertEquals("LinkId: " + linkId.toString() + ", Delay experienced does not match",expectedMap.get(linkId).getDelayExperiencedAtTimeBin(bin), actualMap.get(linkId).getDelayExperiencedAtTimeBin(bin), 0.0);
                Assert.assertEquals("LinkId: " + linkId.toString() + ", Congestion caused does not match",expectedMap.get(linkId).getCongestionCausedAtTimeBin(bin), actualMap.get(linkId).getCongestionCausedAtTimeBin(bin), 0.0);
                Assert.assertEquals("LinkId: " + linkId.toString() + ", Congestion experienced does not match",expectedMap.get(linkId).getCongestionExperiencedAtTimeBin(bin), actualMap.get(linkId).getCongestionExperiencedAtTimeBin(bin), 0.0);
            }
        }
    }
}