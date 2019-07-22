package ethz.ivt.externalities.data.congestion.writer;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionReader;
import ethz.ivt.externalities.data.congestion.io.CSVCongestionWriter;
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

        Id<Link> linkId = Id.createLinkId("link");
        Collection<Id<Link>> collection = new HashSet<>();
        collection.add(linkId);


        AggregateDataPerTimeImpl<Link> aggData = (AggregateDataPerTimeImpl<Link>) AggregateDataPerTimeImpl.congestion(binSize, Link.class);

        Random random = new Random(0);

        for (int bin=0; bin<aggData.getNumBins(); bin++) {
            aggData.setValueForTimeBin(linkId, bin, "count", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "delay_caused", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "delay_experienced", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "congestion_caused", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "congestion_experienced", random.nextDouble());
        }

        CSVCongestionWriter.forLink().write(aggData,"./src/test/java/ethz/ivt/externalities/data/congestion.csv");


        AggregateDataPerTimeImpl<Link> actualMap = CSVCongestionReader.forLink().read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", binSize);

        for (int bin=0; bin<aggData.getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", aggData.getValueInTimeBin(linkId, bin, "count"), actualMap.getValueInTimeBin(linkId, bin, "count"), 0.0);
            Assert.assertEquals("Delay caused does not match", aggData.getValueInTimeBin(linkId, bin, "delay_caused"), actualMap.getValueInTimeBin(linkId, bin, "delay_caused"), 0.0);
            Assert.assertEquals("Delay experienced does not match", aggData.getValueInTimeBin(linkId, bin, "delay_experienced"), actualMap.getValueInTimeBin(linkId, bin, "delay_experienced"), 0.0);
            Assert.assertEquals("Congestion caused does not match", aggData.getValueInTimeBin(linkId, bin, "congestion_caused"), actualMap.getValueInTimeBin(linkId, bin, "congestion_caused"), 0.0);
            Assert.assertEquals("Congestion experienced does not match", aggData.getValueInTimeBin(linkId, bin, "congestion_experienced"), actualMap.getValueInTimeBin(linkId, bin, "congestion_experienced"), 0.0);
        }
    }

    @Test
    public void WritePerPerson() throws IOException {
        double binSize = 3600.;

        AggregateDataPerTimeImpl<Person> aggData = AggregateDataPerTimeImpl.congestionPerson(binSize);

        Id<Person> person = Id.createPersonId("person");
        Collection<Id<Person>> collection = new HashSet<>();
        collection.add(person);

        Random random = new Random(1);


        for (int bin=0; bin<aggData.getNumBins(); bin++) {
            aggData.setValueForTimeBin(person, bin, "count", random.nextDouble());
            aggData.setValueForTimeBin(person, bin, "delay_caused", random.nextDouble());
            aggData.setValueForTimeBin(person, bin, "delay_experienced", random.nextDouble());
            aggData.setValueForTimeBin(person, bin, "congestion_caused", random.nextDouble());
            aggData.setValueForTimeBin(person, bin, "congestion_experienced", random.nextDouble());
        }

        CSVCongestionWriter.forPerson().write(aggData,"./src/test/java/ethz/ivt/externalities/data/congestion.csv");


        AggregateDataPerTimeImpl<Person> actualMap = CSVCongestionReader.forPerson().read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", binSize);

        for (int bin=0; bin<aggData.getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", aggData.getValueInTimeBin(person, bin, "count"), actualMap.getValueInTimeBin(person, bin, "count"), 0.0);
            Assert.assertEquals("Delay caused does not match",  aggData.getValueInTimeBin(person, bin, "delay_caused"), actualMap.getValueInTimeBin(person, bin, "delay_caused"), 0.0);
            Assert.assertEquals("Delay experienced does not match",aggData.getValueInTimeBin(person, bin, "delay_experienced"), actualMap.getValueInTimeBin(person, bin, "delay_experienced"), 0.0);
            Assert.assertEquals("Congestion caused does not match",aggData.getValueInTimeBin(person, bin, "congestion_caused"), actualMap.getValueInTimeBin(person, bin, "congestion_caused"), 0.0);
            Assert.assertEquals("Congestion experienced does not match",aggData.getValueInTimeBin(person, bin, "congestion_experienced"), actualMap.getValueInTimeBin(person, bin, "congestion_experienced"), 0.0);
        }
    }

    @Test
    public void ReadPerLinkDifferentBinSize() throws IOException {
        double originalBinSize = 60.;
        double aggregationFactor = 60.;
        double value = 1.0;
        double newBinSize = aggregationFactor * originalBinSize;

        AggregateDataPerTimeImpl<Link> inputMap = AggregateDataPerTimeImpl.congestionLink(originalBinSize);

        Id<Link> linkId = Id.createLinkId("link");
        Collection<Id<Link>> collection = new HashSet<>();
        collection.add(linkId);

        for (int bin=0; bin<inputMap.getNumBins(); bin++) {
            inputMap.setValueForTimeBin(linkId, bin, "count", value);
            inputMap.setValueForTimeBin(linkId, bin, "delay_caused", value);
            inputMap.setValueForTimeBin(linkId, bin, "delay_experienced", value);
            inputMap.setValueForTimeBin(linkId, bin, "congestion_caused", value);
            inputMap.setValueForTimeBin(linkId, bin, "congestion_experienced", value);
        }

        CSVCongestionWriter.forLink().write(inputMap,"./src/test/java/ethz/ivt/externalities/data/congestion.csv");

        AggregateDataPerTimeImpl<Link> actualMap = CSVCongestionReader.forLink().read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", newBinSize);

        for (int bin=0; bin<actualMap.getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", value * aggregationFactor, actualMap.getValueInTimeBin(linkId, bin, "count"), 0.0);
            Assert.assertEquals("Delay caused does not match", value * aggregationFactor, actualMap.getValueInTimeBin(linkId, bin, "delay_caused"), 0.0);
            Assert.assertEquals("Delay experienced does not match", value * aggregationFactor, actualMap.getValueInTimeBin(linkId, bin, "delay_experienced"), 0.0);
            Assert.assertEquals("Congestion caused does not match", value * aggregationFactor, actualMap.getValueInTimeBin(linkId, bin, "congestion_caused"), 0.0);
            Assert.assertEquals("Congestion experienced does not match", value * aggregationFactor, actualMap.getValueInTimeBin(linkId, bin, "congestion_experienced"), 0.0);
        }
    }

    @Test
    public void WritePerLinkSparse() throws IOException {
        double binSize = 3600.;

        AggregateDataPerTimeImpl<Link> aggData = AggregateDataPerTimeImpl.congestionLink(binSize);


        Id<Link> linkId = Id.createLinkId("link");
        Collection<Id<Link>> collection = new HashSet<>();
        collection.add(linkId);

        int numBins = aggData.getNumBins();

        Random random = new Random(0);

        for (int bin=0; bin<aggData.getNumBins(); bin++) {
            aggData.setValueForTimeBin(linkId, bin, "count", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "delay_caused", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "delay_experienced", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "congestion_caused", random.nextDouble());
            aggData.setValueForTimeBin(linkId, bin, "congestion_experienced", random.nextDouble());
        }

        // set half the entries to zero at random
        int n = (int) numBins / 2;
        for (int i=0; i<n; i++) {

            int bin = random.nextInt(numBins - 1);

            aggData.setValueForTimeBin(linkId, bin, "count", 0.0);
            aggData.setValueForTimeBin(linkId, bin, "delay_caused", 0.0);
            aggData.setValueForTimeBin(linkId, bin, "delay_experienced", 0.0);
            aggData.setValueForTimeBin(linkId, bin, "congestion_caused", 0.0);
            aggData.setValueForTimeBin(linkId, bin, "congestion_experienced", 0.0);
        }

        CSVCongestionWriter.forLink().write(aggData,"./src/test/java/ethz/ivt/externalities/data/congestion.csv");


        AggregateDataPerTimeImpl<Link> actualMap = CSVCongestionReader.forLink().read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", binSize);

        for (int bin=0; bin<aggData.getNumBins(); bin++) {
            Assert.assertEquals("Counts do not match", aggData.getValueInTimeBin(linkId, bin, "count"), actualMap.getValueInTimeBin(linkId, bin, "count"), 0.0);
            Assert.assertEquals("Delay caused does not match", aggData.getValueInTimeBin(linkId, bin, "delay_caused"), actualMap.getValueInTimeBin(linkId, bin, "delay_caused"), 0.0);
            Assert.assertEquals("Delay experienced does not match", aggData.getValueInTimeBin(linkId, bin, "delay_experienced"), actualMap.getValueInTimeBin(linkId, bin, "delay_experienced"), 0.0);
            Assert.assertEquals("Congestion caused does not match", aggData.getValueInTimeBin(linkId, bin, "congestion_caused"), actualMap.getValueInTimeBin(linkId, bin, "congestion_caused"), 0.0);
            Assert.assertEquals("Congestion experienced does not match", aggData.getValueInTimeBin(linkId, bin, "congestion_experienced"), actualMap.getValueInTimeBin(linkId, bin, "congestion_experienced"), 0.0);
        }

    }

    @Test
    public void WriteAndReadMultipleLinks() throws IOException {
        double binSize = 3600.;

        AggregateDataPerTimeImpl<Link> aggData = AggregateDataPerTimeImpl.congestionLink(binSize);
        Collection<Id<Link>> idCollection = new HashSet<>();

        Random randomValueGenerator = new Random(0);
        Random randomZeroGenerator = new Random(1);

        for (int id = 0; id<4; id++) {
            Id<Link> linkId = Id.createLinkId("link_" + id);
            idCollection.add(linkId);

            for (int bin=0; bin<aggData.getNumBins(); bin++) {

                double probability = randomZeroGenerator.nextDouble();
                if (probability < 0.5) {

                    aggData.setValueForTimeBin(linkId, bin, "count", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "delay_caused", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "delay_experienced", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "congestion_caused", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "congestion_experienced", randomValueGenerator.nextDouble());
                }
            }

        }

        // add one link somewhere with all zeros

        Id<Link> link5Id = Id.createLinkId("link_5");
        idCollection.add(link5Id);

        // then some more links
        for (int id = 6; id<10; id++) {
            Id<Link> linkId = Id.createLinkId("link_" + id);
            idCollection.add(linkId);

            for (int bin=0; bin<aggData.getNumBins(); bin++) {

                double probability = randomZeroGenerator.nextDouble();

                if (probability < 0.5) {
                    aggData.setValueForTimeBin(linkId, bin, "count", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "delay_caused", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "delay_experienced", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "congestion_caused", randomValueGenerator.nextDouble());
                    aggData.setValueForTimeBin(linkId, bin, "congestion_experienced", randomValueGenerator.nextDouble());
                }
            }
        }

        CSVCongestionWriter.forLink().write(aggData, "./src/test/java/ethz/ivt/externalities/data/congestion.csv");

        AggregateDataPerTimeImpl<Link> actualMap = CSVCongestionReader.forLink().read("./src/test/java/ethz/ivt/externalities/data/congestion.csv", binSize);

        for (Id<Link> linkId : idCollection) {
            for (int bin=0; bin<aggData.getNumBins(); bin++) {
                if (!linkId.equals(link5Id)) {
                        Assert.assertEquals("Counts do not match", aggData.getValueInTimeBin(linkId, bin, "count"), actualMap.getValueInTimeBin(linkId, bin, "count"), 0.0);
                        Assert.assertEquals("Delay caused does not match", aggData.getValueInTimeBin(linkId, bin, "delay_caused"), actualMap.getValueInTimeBin(linkId, bin, "delay_caused"), 0.0);
                        Assert.assertEquals("Delay experienced does not match", aggData.getValueInTimeBin(linkId, bin, "delay_experienced"), actualMap.getValueInTimeBin(linkId, bin, "delay_experienced"), 0.0);
                        Assert.assertEquals("Congestion caused does not match", aggData.getValueInTimeBin(linkId, bin, "congestion_caused"), actualMap.getValueInTimeBin(linkId, bin, "congestion_caused"), 0.0);
                        Assert.assertEquals("Congestion experienced does not match", aggData.getValueInTimeBin(linkId, bin, "congestion_experienced"), actualMap.getValueInTimeBin(linkId, bin, "congestion_experienced"), 0.0);
                    }
                else {
                    Assert.assertNull("lines from Link_5 should not be in actual map", actualMap.getData().get(link5Id));
                }
            }
        }
    }




}