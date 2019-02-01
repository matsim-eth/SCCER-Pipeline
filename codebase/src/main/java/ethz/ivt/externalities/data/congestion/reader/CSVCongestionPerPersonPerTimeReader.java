package ethz.ivt.externalities.data.congestion.reader;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVCongestionPerPersonPerTimeReader {
    public Map<Id<Person>, CongestionPerTime> read(String path) throws IOException {
        Map<Id<Person>, CongestionPerTime> map = new HashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Person> personId = Id.createPersonId(row.get(header.indexOf("personId")));
                int timeBin = Integer.parseInt(row.get(header.indexOf("timeBin")));
                double count = Double.parseDouble(row.get(header.indexOf("count")));
                double delayCaused = Double.parseDouble(row.get(header.indexOf("delay_caused")));
                double delayExperienced = Double.parseDouble(row.get(header.indexOf("delay_experienced")));
                double congestionCaused = Double.parseDouble(row.get(header.indexOf("congestion_caused")));
                double congestionExperienced = Double.parseDouble(row.get(header.indexOf("congestion_experienced")));

                map.putIfAbsent(personId, new CongestionPerTime(3600.));
                map.get(personId).setCountAtTimeBin(count, timeBin);
                map.get(personId).setDelayCausedAtTimeBin(delayCaused, timeBin);
                map.get(personId).setDelayExperiencedAtTimeBin(delayExperienced, timeBin);
                map.get(personId).setCongestionCausedAtTimeBin(congestionCaused, timeBin);
                map.get(personId).setCongestionExperiencedAtTimeBin(congestionExperienced, timeBin);
            }
        }

        reader.close();
        return map;
    }
}
