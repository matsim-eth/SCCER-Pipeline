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
    private double binSize;
    private Map<Id<Link>, CongestionPerTime> map = new HashMap<>();

    public CSVCongestionPerLinkPerTimeReader(Collection<Id<Link>> linkIds, double binSize) {
        this.binSize = binSize;
        for (Id<Link> linkId : linkIds) {
            map.putIfAbsent(linkId, new CongestionPerTime(this.binSize));
        }
    }

    public Map<Id<Link>, CongestionPerTime> read(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(";"));

            if (header == null) {
                header = row;
            } else {
                Id<Link> linkId = Id.createLinkId(row.get(header.indexOf("linkId")));
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
