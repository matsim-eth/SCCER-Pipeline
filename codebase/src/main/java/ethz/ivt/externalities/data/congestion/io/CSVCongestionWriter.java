package ethz.ivt.externalities.data.congestion.io;

import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

public class CSVCongestionWriter<T> {

    public static CSVCongestionWriter<Link> forLink() {
        return new CSVCongestionWriter<>();
    }

    public static CSVCongestionWriter<Person> forPerson() {
        return new CSVCongestionWriter<>();
    }

    private CSVCongestionWriter() {
    }

    public void write(Map<Id<T>, CongestionPerTime> map, String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader());
        writer.flush();

        for (Map.Entry<Id<T>, CongestionPerTime> entry : map.entrySet()) {
            writer.write(formatItem(entry.getKey(), entry.getValue()));
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(";", new String[] {
                "Id", "binSize", "timeBin", "count", "delay_caused", "delay_experienced", "congestion_caused", "congestion_experienced"
        });
    }

    private String formatItem(Id<T> id, CongestionPerTime congestion) {

        String s = "";

        boolean isFirstLine = true;

        for (int bin=0; bin<congestion.getNumBins(); bin++) {

            // only write lines where the counts are greater than zero to save space
            if (congestion.getCountAtTimeBin(bin) > 0.0) {
                s = String.join("\n", new String[] {s, formatSingleLine(id, congestion, bin)});
            }
        }

        return s;
    }

    private String formatSingleLine(Id<T> id, CongestionPerTime congestion, int bin) {
        return String.join(";", new String[] {
                id.toString(),
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
