package ethz.ivt.aggregation;

import java.net.URI;

public class ExternalityUtils {
    private ExternalityUtils(){}

    public static int getTimeBin(double time, double binSize_s) {

        double timeAfterSimStart = time;

		/*
		 * Agents who end their first activity before the simulation has started
		 * will depart in the first time step.
		 */
        if (timeAfterSimStart <= 0.0) return 0;

		/*
		 * Calculate the bin for the given time. If the result
		 * of the modulo operation is 0, it is the last time value
		 * which is part of the previous bin.
		 */
        int bin = (int) (timeAfterSimStart / binSize_s);
        if (timeAfterSimStart % binSize_s == 0.0) bin--;

        return bin;
    }

    public static String getDate(String eventFileName) {
        // Assume event file name format "date_events.xml[.gz]"
        return eventFileName.split("_")[0];
    }
}
