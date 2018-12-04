package ethz.ivt.roadpricing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.TollFactor;
import org.matsim.vehicles.Vehicle;

public class PeakTollFactor implements TollFactor {

    int timeBins;
    double[] tollMask;

    public static TollFactor basicPeak (double factor) {
        int timeBins = 24;
        double [] tollMask = new double[24];
        tollMask[8-1] = factor;
        tollMask[9-1] = factor;
        tollMask[17-1] = factor;
        tollMask[18-1] = factor;
        return new PeakTollFactor(timeBins, tollMask);
    }

    private PeakTollFactor(int timeBins, double[] tollMask) {
        this.timeBins = timeBins;
        this.tollMask = tollMask;
    }

    @Override
    public double getTollFactor(Id<Person> personId, Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
        int bin = (int) ((time % Time.MIDNIGHT) / Time.MIDNIGHT) * timeBins ;

        return tollMask[bin];
    }
}
