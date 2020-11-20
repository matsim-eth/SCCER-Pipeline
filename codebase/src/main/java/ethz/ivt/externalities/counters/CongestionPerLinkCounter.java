package ethz.ivt.externalities.counters;

import ethz.ivt.externalities.data.AggregateDataPerTimeImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.trafficmonitoring.TimeBinUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class CongestionPerLinkCounter implements LinkEnterEventHandler {

    private static final String OSM_HIGHWAY_TAG = "osm:way:highway";
    private static final Logger log = Logger.getLogger(CongestionPerLinkCounter.class);

    private final Scenario scenario;
    private final int binSize;
    private final int numBins;

    private AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime;
    private Map<Id<Link>, double[]> congestionPerLinkPerTimeBin = new HashMap<>();
    private Map<Id<Link>, Boolean> isMotorwayMap = new HashMap<>();

    public CongestionPerLinkCounter(Scenario scenario, AggregateDataPerTimeImpl<Link> aggregateCongestionDataPerLinkPerTime, int binSize) {
        this.scenario = scenario;
        this.aggregateCongestionDataPerLinkPerTime = aggregateCongestionDataPerLinkPerTime;
        this.binSize = binSize;
        this.numBins = 30 * 3600 / binSize;
    }

    @Override
    public void reset(int iteration) {
        this.isMotorwayMap.clear();
        this.congestionPerLinkPerTimeBin.clear();
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        double time = event.getTime();
        Id<Link> linkId = event.getLinkId();

        // get mean congestion on that link at that time
        double congestion = this.aggregateCongestionDataPerLinkPerTime.getValueAtTime(linkId, time, "congestion");

        // get time bin
        int timeBin = TimeBinUtils.getTimeBinIndex(time, this.binSize, this.numBins);

        // add value for that time bin for that link
        this.congestionPerLinkPerTimeBin.putIfAbsent(linkId, new double[numBins]);
        double oldValue = this.congestionPerLinkPerTimeBin.get(linkId)[timeBin];
        this.congestionPerLinkPerTimeBin.get(linkId)[timeBin] = oldValue + congestion;

        // store if road type is motorway
        String roadType = null;
        if (this.scenario.getNetwork().getLinks().containsKey(linkId)) {
            roadType = (String) this.scenario.getNetwork().getLinks().get(linkId).getAttributes().getAttribute(OSM_HIGHWAY_TAG);
        }
        if (roadType == null) {
            roadType = "none";
        }
        this.isMotorwayMap.put(linkId, roadType.contains("motorway"));
    }

    public void write(String outputPath) throws IOException{
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Id<Link> linkId : this.congestionPerLinkPerTimeBin.keySet()) {

            boolean isMotorway = this.isMotorwayMap.get(linkId);

            for (int timeBin = 0; timeBin < this.congestionPerLinkPerTimeBin.get(linkId).length; timeBin++) {
                double congestion = this.congestionPerLinkPerTimeBin.get(linkId)[timeBin];

                writer.write(formatEntry(linkId, timeBin, isMotorway, congestion) + "\n");
                writer.flush();
            }
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(";", new String[] { //
                "link_id",
                "bin_size",
                "time_bin",
                "is_motorway",
                "congestion"
        });
    }

    private String formatEntry(Id<Link> linkId, int timeBin, boolean isMotorway, double congestion) {

        return String.join(";", new String[] { //
                String.valueOf(linkId.toString()), //
                String.valueOf(this.binSize), //
                String.valueOf(timeBin), //
                String.valueOf(isMotorway), //
                String.valueOf(congestion), //
        });
    }

}
