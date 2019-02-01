package ethz.ivt.externalities.data.congestion.writer;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

public class CSVCongestionPerPersonPerTimeWriter {
    final private Map<Id<Person>, CongestionPerTime> map;

    public CSVCongestionPerPersonPerTimeWriter(Map<Id<Person>, CongestionPerTime> map) {
        this.map = map;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Map.Entry<Id<Person>, CongestionPerTime> entry : map.entrySet()) {
            writer.write(formatItem(entry.getKey(), entry.getValue()) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(";", new String[] {
                "personId", "binSize", "timeBin", "count", "delay_caused", "delay_experienced", "congestion_caused", "congestion_experienced"
        });
    }

    private String formatItem(Id<Person> personId, CongestionPerTime congestion) {

        String s = "";

        boolean isFirstLine = true;

        for (int bin=0; bin<congestion.getNumBins(); bin++) {

            // only write lines where the counts are greater than zero to save space
            if (congestion.getCountAtTimeBin(bin) > 0.0) {
                if (isFirstLine) {
                    s = formatSingleLine(personId, congestion, bin);
                    isFirstLine = false;
                } else {
                    s = String.join("\n", new String[] {s, formatSingleLine(personId, congestion, bin)});
                }
            }
        }

        return s;
    }

    private String formatSingleLine(Id<Person> personId, CongestionPerTime congestion, int bin) {
        return String.join(";", new String[] {
                personId.toString(),
                String.valueOf(congestion.getBinSize()),
                String.valueOf(bin),
                String.valueOf(congestion.getCountAtTimeBin(bin)),
                String.valueOf(congestion.getDelayCausedAtTimeBin(bin)),
                String.valueOf(congestion.getDelayExperiencedAtTimeBin(bin)),
                String.valueOf(congestion.getCongestionCausedAtTimeBin(bin)),
                String.valueOf(congestion.getCongestionExperiencedAtTimeBin(bin)),
        });
    }
}
