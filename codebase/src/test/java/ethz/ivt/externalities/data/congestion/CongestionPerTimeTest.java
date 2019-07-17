package ethz.ivt.externalities.data.congestion;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import ethz.ivt.externalities.data.congestion.io.IdSerializer;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class CongestionPerTimeTest {

    @Test
    public void testKryo() throws FileNotFoundException {

        ArrayList<String> att = new ArrayList<>();
        att.add("count");
        att.add("delay");

        Kryo kryo = new Kryo();
        kryo.register(Id.class, new IdSerializer(Link.class));
        kryo.register(CongestionPerTime.class);

        CongestionPerTime cd1 = new CongestionPerTime(3600.0, att);
        for (int i=0; i< 100; i++) {

            for (String a : cd1.attributes) {
                for (int j = 0; j < cd1.numBins; j++) {
                    Id<Link> linkId = Id.createLinkId(i);
                    cd1.addValue(linkId, j, a, (int) (Math.random() * 100));
                }
            }
        }
        cd1.writeDataToCsv("C:\\Projects\\SCCER_project\\output\\test.csv");

        Output output = new Output(1024, -1);
        kryo.writeObject(output, cd1);
        output.flush();
        output.close();

        Input input = new Input(output.getBuffer(), 0, output.position());
        CongestionPerTime cd2 = kryo.readObject(input, CongestionPerTime.class);
        assert(cd1.equals(cd2));


    }


    @Test
    public void testFullDataset() throws FileNotFoundException {

        ArrayList<String> att = new ArrayList<>();
        att.add("count");
        att.add("delay_caused");
        att.add("delay_experienced");
        att.add("congestion_caused");
        att.add("congestion_experienced");

        Kryo kryo = new Kryo();
        kryo.register(CongestionPerTime.class);

        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();

        CongestionPerTime cd1 = new CongestionPerTime(900, att);
        Path folder = Paths.get("C:\\Projects\\SCCER_project\\data\\new_swiss\\congestion");
        System.out.print("loading from csv...");
        cd1.loadDataFromCsv(folder.resolve("aggregate_delay_per_link_per_time.csv").toString());

        endTime = System.currentTimeMillis();
        System.out.println(String.format("%.2f", ((float) (endTime-startTime))/1000));
        startTime = System.currentTimeMillis();

        System.out.print("write to csv...");
        cd1.writeDataToCsv(folder.resolve("aggregate_delay_per_link_per_time_integers.csv").toString());

        endTime = System.currentTimeMillis();
        System.out.println(String.format("%.2f", ((float) (endTime-startTime))/1000));
        startTime = System.currentTimeMillis();

        Path kryo_file = folder.resolve("aggregate_delay_per_link_per_time.kryo");

        System.out.print("saving to kryo format...");
        Output output = new Output(new FileOutputStream(kryo_file.toFile()));
        kryo.writeObject(output, cd1);
        output.flush();
        output.close();

        endTime = System.currentTimeMillis();
        System.out.println(String.format("%.2f", ((float) (endTime-startTime))/1000));
        startTime = System.currentTimeMillis();

        System.out.print("reading from kryo format...");
        Input input = new Input(new FileInputStream(kryo_file.toFile()));
        CongestionPerTime cd2 = kryo.readObject(input, CongestionPerTime.class);
        System.out.println("done");

        endTime = System.currentTimeMillis();
        System.out.println(String.format("%.2f", ((float) (endTime-startTime))/1000));
        startTime = System.currentTimeMillis();

    //    assert(cd1.equals(cd2));


    }
}
