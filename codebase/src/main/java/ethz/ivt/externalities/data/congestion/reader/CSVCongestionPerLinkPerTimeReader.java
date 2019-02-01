package ethz.ivt.externalities.data.congestion.reader;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVCongestionPerLinkPerTimeReader {
    public Map<Id<Link>, CongestionPerTime> read(String path) throws IOException {
        Map<Id<Link>, CongestionPerTime> map = new HashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("linkId")));
                int timeBin = Integer.parseInt(row.get(header.indexOf("timeBin")));
                double count = Double.parseDouble(row.get(header.indexOf("count")));
                double delayCaused = Double.parseDouble(row.get(header.indexOf("delay_caused")));
                double delayExperienced = Double.parseDouble(row.get(header.indexOf("delay_experienced")));
                double congestionCaused = Double.parseDouble(row.get(header.indexOf("congestion_caused")));
                double congestionExperienced = Double.parseDouble(row.get(header.indexOf("congestion_experienced")));

                map.putIfAbsent(linkId, new CongestionPerTime(3600.));
                map.get(linkId).setCountAtTimeBin(count, timeBin);
                map.get(linkId).setDelayCausedAtTimeBin(delayCaused, timeBin);
                map.get(linkId).setDelayExperiencedAtTimeBin(delayExperienced, timeBin);
                map.get(linkId).setCongestionCausedAtTimeBin(congestionCaused, timeBin);
                map.get(linkId).setCongestionExperiencedAtTimeBin(congestionExperienced, timeBin);
            }
        }

        reader.close();
        return map;
    }
}
