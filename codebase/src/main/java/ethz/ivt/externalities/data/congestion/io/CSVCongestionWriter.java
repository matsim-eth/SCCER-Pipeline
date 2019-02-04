package ethz.ivt.externalities.data.congestion.io;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
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

    public void write(AggregateDataPerTimeImpl<T> aggData, String outputPath)  {
        aggData.writeDataToCsv(outputPath);
    }

}
