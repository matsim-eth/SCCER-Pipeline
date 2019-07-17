package ethz.ivt.externalities.data.congestion.io;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.matsim.api.core.v01.Id;

public class IdSerializer extends Serializer<Id> {

    public IdSerializer(Class idClass) {
        this.idClass = idClass;
    }

    Class idClass;

    public void write (Kryo kryo, Output output, Id id) {
        output.writeString(id.toString());
    }

    @Override
    public Id read(Kryo kryo, Input input, Class<? extends Id> type) {
         return Id.create(input.readString(), getIdClass());
    }

    public Class getIdClass() {
        return idClass;
    }

    public void setIdType(Class idClass) {
        this.idClass = idClass;
    }

}
