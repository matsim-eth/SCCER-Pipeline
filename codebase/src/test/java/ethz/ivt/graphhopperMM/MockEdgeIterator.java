package ethz.ivt.graphhopperMM;

import com.graphhopper.matching.GPXExtension;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.List;

/**
 * Created by molloyj on 14.11.2017.
 */

class MockEdgeIterator implements EdgeIteratorState {

    private String edgeId;

    public MockEdgeIterator(String edgeId) {
        this.edgeId = edgeId;
    }

    public MockEdgeIterator(Id<Link> id) {
        this.edgeId = id.toString();
    }

    @Override
    public int getEdge() {
        return 0;
    }

    @Override
    public int getBaseNode() {
        return 0;
    }

    @Override
    public int getAdjNode() {
        return 0;
    }

    @Override
    public PointList fetchWayGeometry(int mode) {
        return null;
    }

    @Override
    public EdgeIteratorState setWayGeometry(PointList list) {
        return null;
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public EdgeIteratorState setDistance(double dist) {
        return null;
    }

    @Override
    public long getFlags() {
        return 0;
    }

    @Override
    public EdgeIteratorState setFlags(long flags) {
        return null;
    }

    @Override
    public int getAdditionalField() {
        return 0;
    }

    @Override
    public EdgeIteratorState setAdditionalField(int value) {
        return null;
    }

    @Override
    public boolean isForward(FlagEncoder encoder) {
        return false;
    }

    @Override
    public boolean isBackward(FlagEncoder encoder) {
        return false;
    }

    @Override
    public boolean getBool(int key, boolean _default) {
        return false;
    }

    @Override
    public String getName() {
        return edgeId;
    }

    @Override
    public EdgeIteratorState setName(String name) {
        return null;
    }

    @Override
    public EdgeIteratorState detach(boolean reverse) {
        return null;
    }

    @Override
    public EdgeIteratorState copyPropertiesTo(EdgeIteratorState e) {
        return null;
    }
}

