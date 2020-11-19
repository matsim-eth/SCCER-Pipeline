package ethz.ivt.externalities.data;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.Map;

public class AggregateDataSerializer extends Serializer<AggregateDataPerTimeImpl> {


    @Override
    public void write(Kryo kryo, Output output, AggregateDataPerTimeImpl object) {

        kryo.writeClass(output, object.clazz);
        output.writeDouble(object.binSize);
        output.writeInt(object.numBins);
        output.writeInt(object.attributes.size());
        output.writeInt(object.aggregateDataPerLinkPerTime.size());
        ArrayList<String> attrs = object.getAttributes();
        for (String at : attrs) {
            output.writeString(at);
        }

        //Map<Id<Link>, Map<String, double[]>> aggregateDataPerLinkPerTime = new HashMap<>();
        for (Object id : object.aggregateDataPerLinkPerTime.keySet()) {
            output.writeString(id.toString());
            Map<Id<?>, Map<String, double[]>> data = (Map<Id<?>, Map<String, double[]>>) object.aggregateDataPerLinkPerTime;

            long numRows = 0;

            for (int bin = 0; bin < object.numBins; bin++) {
                if (data.get(id).get("count")[bin] > 0) {
                    numRows += 1;
                }
            }

            output.writeLong(numRows, true);
            for (int bin = 0; bin < object.numBins; bin++) {
                if (data.get(id).get("count")[bin] > 0) {
                    output.writeInt(bin, true);
                    for (Object at : object.attributes) {
                        double val = data.get(id).get(at)[bin];
                        output.writeDouble(val);

                    }
                }
            }
        }

    }

    @Override
    public AggregateDataPerTimeImpl read(Kryo kryo, Input input, Class<? extends AggregateDataPerTimeImpl> type) {
        Registration r = kryo.readClass(input);
        //    Class valueClass = r.getType();
        Class valueClass = Link.class;

        double binSize = input.readDouble();
        int numBins = input.readInt();
        int numAttr = input.readInt();
        int numLinks = input.readInt();
        ArrayList<String> attributes = new ArrayList<>();


        for (int i = 0; i < numAttr; i++) {
            attributes.add(input.readString());
        }

        AggregateDataPerTimeImpl object = new AggregateDataPerTimeImpl(binSize, attributes, valueClass);

        for (int i = 0; i < numLinks; i++) {
            Id linkId = Id.create(input.readString(), valueClass);
            long numRows = input.readLong(true);

            for (int bin = 0; bin < numRows; bin++) {
                int bin_num = input.readInt(true);
                for (String at : attributes) {
                    double val = input.readDouble();
                    object.setValueForTimeBin(linkId, bin_num, at, val);
                }

            }
        }
        kryo.getGenerics().popGenericType();
        return object;

    }
}