package ethz.ivt.externalities.data.congestion.reader;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class CSVCongestionPerLinkPerTimeReader {

    static public Map<Id<Link>, CongestionPerTime> read(String path, double binSize) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        Map<Id<Link>, CongestionPerTime> map = new HashMap<>();

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("linkId")));
                map.putIfAbsent(linkId, new CongestionPerTime(binSize));

                double originalBinSize = Double.parseDouble(row.get(header.indexOf("binSize")));
                int timeBin = Integer.parseInt(row.get(header.indexOf("timeBin")));

                double time = timeBin * originalBinSize;

                double count = Double.parseDouble(row.get(header.indexOf("count")));
                double delayCaused = Double.parseDouble(row.get(header.indexOf("delay_caused")));
                double delayExperienced = Double.parseDouble(row.get(header.indexOf("delay_experienced")));
                double congestionCaused = Double.parseDouble(row.get(header.indexOf("congestion_caused")));
                double congestionExperienced = Double.parseDouble(row.get(header.indexOf("congestion_experienced")));

                map.get(linkId).addCountAtTime(count, time);
                map.get(linkId).addDelayCausedAtTime(delayCaused, time);
                map.get(linkId).addDelayExperiencedAtTime(delayExperienced, time);
                map.get(linkId).addCongestionCausedAtTime(congestionCaused, time);
                map.get(linkId).addCongestionExperiencedAtTime(congestionExperienced, time);
            }
        }

        reader.close();
        return map;
    }
}
