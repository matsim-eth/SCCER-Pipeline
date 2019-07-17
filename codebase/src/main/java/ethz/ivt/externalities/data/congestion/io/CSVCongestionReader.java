package ethz.ivt.externalities.data.congestion.io;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.*;

public class CSVCongestionReader<T> {

    private final Class<T> clazz;

    public static CSVCongestionReader<Link> forLink() {
        return new CSVCongestionReader<>(Link.class);
    }

    public static CSVCongestionReader<Person> forPerson() {
        return new CSVCongestionReader<>(Person.class);
    }

    private CSVCongestionReader(Class<T> clazz) {
        this.clazz = clazz;
    }

    public AggregateDataPerTimeImpl<T> read(String path, double binSize) {

        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("count");
        attributes.add("delay_caused");
        attributes.add("delay_experienced");
        attributes.add("congestion_caused");
        attributes.add("congestion_experienced");

        AggregateDataPerTimeImpl<T> aggData = new AggregateDataPerTimeImpl<T>(binSize, attributes, clazz);
        aggData.loadDataFromCsv(path);
        return aggData;
    }
}
