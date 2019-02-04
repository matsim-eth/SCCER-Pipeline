package ethz.ivt.externalities.data.congestion.io;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public Map<Id<T>, CongestionPerTime> read(String path, double binSize) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        Map<Id<T>, CongestionPerTime> map = new HashMap<>();

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<T> id = Id.create(row.get(header.indexOf("id")), clazz);
                map.putIfAbsent(id, new CongestionPerTime(binSize));

                double originalBinSize = Double.parseDouble(row.get(header.indexOf("binSize")));
                int timeBin = Integer.parseInt(row.get(header.indexOf("timeBin")));

                double time = timeBin * originalBinSize;

                double count = Double.parseDouble(row.get(header.indexOf("count")));
                double delayCaused = Double.parseDouble(row.get(header.indexOf("delay_caused")));
                double delayExperienced = Double.parseDouble(row.get(header.indexOf("delay_experienced")));
                double congestionCaused = Double.parseDouble(row.get(header.indexOf("congestion_caused")));
                double congestionExperienced = Double.parseDouble(row.get(header.indexOf("congestion_experienced")));

                map.get(id).addCountAtTime(count, time);
                map.get(id).addDelayCausedAtTime(delayCaused, time);
                map.get(id).addDelayExperiencedAtTime(delayExperienced, time);
                map.get(id).addCongestionCausedAtTime(congestionCaused, time);
                map.get(id).addCongestionExperiencedAtTime(congestionExperienced, time);
            }
        }

        reader.close();
        return map;
    }
}
