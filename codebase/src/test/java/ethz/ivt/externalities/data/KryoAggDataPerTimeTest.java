package ethz.ivt.externalities.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import ethz.ivt.externalities.data.congestion.CongestionPerTime;
import ethz.ivt.externalities.data.congestion.io.IdSerializer;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;


public class KryoAggDataPerTimeTest {

    @Test
    public void testKryo() throws FileNotFoundException {

        ArrayList<String> att = new ArrayList<>();
        att.add("count");
        att.add("delay");

        Kryo kryo = new Kryo();
        kryo.register(Id.class, new IdSerializer(Link.class));
        kryo.register(Link.class);
        kryo.register(AggregateDataPerTimeImpl.class, new AggregateDataSerializer());

        AggregateDataPerTimeImpl<Link> cd1 = new AggregateDataPerTimeImpl<>(3600.0, att, Link.class);
        for (int i=0; i< 100; i++) {

            for (String a : cd1.attributes) {
                for (int j = 0; j < cd1.numBins; j++) {
                    Id<Link> linkId = Id.createLinkId(i);
                    double val = Math.random() * 100;
                    if (cd1.getValueInTimeBin(linkId, j, "count") > 0){
                        cd1.addValueToTimeBin(linkId, j, a, val);

                    }
                }
            }
        }
        cd1.writeDataToCsv("C:\\Projects\\SCCER_project\\output\\test.csv");

        Output output = new Output(1024, -1);
        kryo.writeObject(output, cd1);
        output.flush();
        output.close();

        Input input = new Input(output.getBuffer(), 0, output.position());
        AggregateDataPerTimeImpl<Link>  cd2 = kryo.readObject(input, AggregateDataPerTimeImpl.class);

        //check equals
        compareDatasets(cd1, cd2);
    }

    @Test
    public void testKryoWrite() throws FileNotFoundException {

        ArrayList<String> att = new ArrayList<>();
        att.add("count");
        att.add("delay");

        Kryo kryo = new Kryo();
        kryo.register(Id.class, new IdSerializer(Link.class));
        kryo.register(Link.class);
        kryo.register(AggregateDataPerTimeImpl.class, new AggregateDataSerializer());

        AggregateDataPerTimeImpl<Link> cd1 = new AggregateDataPerTimeImpl<>(3600.0, att, Link.class);
        for (int i=0; i< 100; i++) {

            for (String a : cd1.attributes) {
                for (int j = 0; j < cd1.numBins; j++) {
                    Id<Link> linkId = Id.createLinkId(i);
                    double val = Math.random() * 100;
                    if (cd1.getValueInTimeBin(linkId, j, "count") > 0){
                        cd1.addValueToTimeBin(linkId, j, a, val);

                    }
                }
            }
        }
        Path folder = Paths.get("C:\\Projects\\SCCER_project\\data\\new_swiss\\congestion");
        Output output = new Output(new FileOutputStream(folder.resolve("test.kryo").toFile()));

        kryo.writeObject(output, cd1);
        output.flush();
        output.close();

    }

    @Test
    public void testKryoRead() throws FileNotFoundException {

        ArrayList<String> att = new ArrayList<>();
        att.add("count");
        att.add("delay");

        Kryo kryo = new Kryo();
        kryo.register(Id.class, new IdSerializer(Link.class));
        kryo.register(Link.class);
        kryo.register(AggregateDataPerTimeImpl.class, new AggregateDataSerializer());

        Path folder = Paths.get("C:\\Projects\\SCCER_project\\data\\new_swiss\\congestion");
        Input input = new Input(new FileInputStream(folder.resolve("test.kryo").toFile()));

        AggregateDataPerTimeImpl<Link>  cd2 = kryo.readObject(input, AggregateDataPerTimeImpl.class);
    }

    private void compareDatasets(AggregateDataPerTimeImpl<Link> cd1, AggregateDataPerTimeImpl<Link> cd2) {
        for (int i=0; i< 100; i++) {
            for (String a : cd1.attributes) {
                for (int j = 0; j < cd1.numBins; j++) {
                    double v1 = cd1.getValueInTimeBin(Id.createLinkId(i), j, a);
                    double v2 = cd2.getValueInTimeBin(Id.createLinkId(i), j, a);
                    assertEquals ("Values should be equals", v1, v2, 0.01);
                }
            }

        }
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
        kryo.register(Id.class, new IdSerializer(Link.class));
        kryo.register(Link.class);
        kryo.register(AggregateDataPerTimeImpl.class, new AggregateDataSerializer());

        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();

        AggregateDataPerTimeImpl<Link>  cd1 = new AggregateDataPerTimeImpl<>(900, att, Link.class);
        Path folder = Paths.get("C:\\Projects\\SCCER_project\\data\\new_swiss\\congestion");
        System.out.print("loading from csv...");
        cd1.loadDataFromCsv(folder.resolve("aggregate_delay_per_link_per_time.csv").toString());

        endTime = System.currentTimeMillis();
        System.out.println(String.format("%.2f", ((float) (endTime-startTime))/1000));
        startTime = System.currentTimeMillis();

        System.out.print("write to csv...");
      //  cd1.writeDataToCsv(folder.resolve("aggregate_delay_per_link_per_time_integers.csv").toString());

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
        AggregateDataPerTimeImpl<Link> cd2 = kryo.readObject(input, AggregateDataPerTimeImpl.class);
        System.out.println("done");

        endTime = System.currentTimeMillis();
        System.out.println(String.format("%.2f", ((float) (endTime-startTime))/1000));
        startTime = System.currentTimeMillis();
        System.out.print("comparing datasets...");

        compareDatasets(cd1, cd2);

        endTime = System.currentTimeMillis();
        System.out.println(String.format("%.2f", ((float) (endTime-startTime))/1000));
        startTime = System.currentTimeMillis();

    }


    @Test
    public void readDataset() throws FileNotFoundException {


        Kryo kryo = new Kryo();
        kryo.register(Id.class, new IdSerializer(Link.class));
        kryo.register(Link.class);
        kryo.register(AggregateDataPerTimeImpl.class, new AggregateDataSerializer());

        Path folder = Paths.get("C:\\Projects\\SCCER_project\\data\\new_swiss\\congestion");
        Path kryo_file = folder.resolve("aggregate_delay_per_link_per_time.kryo");

        System.out.print("reading from kryo format...");
        Input input = new Input(new FileInputStream(kryo_file.toFile()));
        AggregateDataPerTimeImpl<Link> cd2 = kryo.readObject(input, AggregateDataPerTimeImpl.class);
        System.out.println("done");


    }
}
