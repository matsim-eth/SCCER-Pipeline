package ethz.ivt.externalities.data.congestion.reader;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class CSVCongestionPerPersonPerTimeReader {

    private double binSize;
    private Map<Id<Person>, CongestionPerTime> map = new HashMap<>();

    public CSVCongestionPerPersonPerTimeReader(Collection<Id<Person>> personIds, double binSize) {
        this.binSize = binSize;
        for (Id<Person> personId : personIds) {
            map.putIfAbsent(personId, new CongestionPerTime(this.binSize));
        }
    }

    public Map<Id<Person>, CongestionPerTime> read(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Person> personId = Id.createPersonId(row.get(header.indexOf("personId")));
                double originalBinSize = Double.parseDouble(row.get(header.indexOf("binSize")));
                int timeBin = Integer.parseInt(row.get(header.indexOf("timeBin")));

                double time = timeBin * originalBinSize;

                double count = Double.parseDouble(row.get(header.indexOf("count")));
                double delayCaused = Double.parseDouble(row.get(header.indexOf("delay_caused")));
                double delayExperienced = Double.parseDouble(row.get(header.indexOf("delay_experienced")));
                double congestionCaused = Double.parseDouble(row.get(header.indexOf("congestion_caused")));
                double congestionExperienced = Double.parseDouble(row.get(header.indexOf("congestion_experienced")));

                map.get(personId).addCountAtTime(count, time);
                map.get(personId).addDelayCausedAtTime(delayCaused, time);
                map.get(personId).addDelayExperiencedAtTime(delayExperienced, time);
                map.get(personId).addCongestionCausedAtTime(congestionCaused, time);
                map.get(personId).addCongestionExperiencedAtTime(congestionExperienced, time);
            }
        }

        reader.close();
        return map;
    }
}
