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
                "personId", "timeBin", "count", "delay_caused", "delay_experienced", "congestion_caused", "congestion_experienced"
        });
    }

    private String formatItem(Id<Person> personId, CongestionPerTime congestion) {

        String s = "";

        if (congestion.getNumBins() > 0) {
            s = String.join(";", new String[] {
                    personId.toString(),
                    String.valueOf(0),
                    String.valueOf(congestion.getCountAtTimeBin(0)),
                    String.valueOf(congestion.getDelayCausedAtTimeBin(0)),
                    String.valueOf(congestion.getDelayExperiencedAtTimeBin(0)),
                    String.valueOf(congestion.getCongestionCausedAtTimeBin(0)),
                    String.valueOf(congestion.getCongestionExperiencedAtTimeBin(0)),
            });

            if (congestion.getNumBins() > 1) {

                for (int bin=1; bin<congestion.getNumBins(); bin++) {
                    String temp = String.join(";", new String[] {
                            personId.toString(),
                            String.valueOf(bin),
                            String.valueOf(congestion.getCountAtTimeBin(bin)),
                            String.valueOf(congestion.getDelayCausedAtTimeBin(bin)),
                            String.valueOf(congestion.getDelayExperiencedAtTimeBin(bin)),
                            String.valueOf(congestion.getCongestionCausedAtTimeBin(bin)),
                            String.valueOf(congestion.getCongestionExperiencedAtTimeBin(bin)),
                    });

                    s = String.join("\n", new String[] {s, temp});
                }
            }
        }

        return s;
    }
}
