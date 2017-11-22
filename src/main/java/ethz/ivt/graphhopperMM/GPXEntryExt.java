package ethz.ivt.graphhopperMM;

import com.graphhopper.util.GPXEntry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.List;

/**
 * Created by molloyj on 20.11.2017.
 */
public class GPXEntryExt{

    final int leg;
    final String mode;
    final GPXEntry gpxEntry;

    public GPXEntryExt(int leg, String mode, double lat, double lon, long millis) {
        this.gpxEntry = new GPXEntry(lat, lon, millis);
        this.leg = leg;
        this.mode = mode;
    }

    public Integer getLeg() {
        return leg;
    }

    public GPXEntry getGpxEntry() {
        return gpxEntry;
    }

    public Tuple<Integer, String> getLegModeTuple() {
        return new Tuple<>(leg, mode);
    }
}
