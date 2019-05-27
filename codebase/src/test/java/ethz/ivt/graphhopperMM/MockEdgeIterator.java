package ethz.ivt.graphhopperMM;

import com.graphhopper.matching.GPXExtension;
import com.graphhopper.routing.profiles.BooleanEncodedValue;
import com.graphhopper.routing.profiles.DecimalEncodedValue;
import com.graphhopper.routing.profiles.EnumEncodedValue;
import com.graphhopper.routing.profiles.IntEncodedValue;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.IntsRef;
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
    public int getOrigEdgeFirst() {
        return 0;
    }

    @Override
    public int getOrigEdgeLast() {
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
    public IntsRef getFlags() {
        return null;
    }

    @Override
    public EdgeIteratorState setFlags(IntsRef edgeFlags) {
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
    public boolean get(BooleanEncodedValue property) {
        return false;
    }

    @Override
    public EdgeIteratorState set(BooleanEncodedValue property, boolean value) {
        return null;
    }

    @Override
    public boolean getReverse(BooleanEncodedValue property) {
        return false;
    }

    @Override
    public EdgeIteratorState setReverse(BooleanEncodedValue property, boolean value) {
        return null;
    }

    @Override
    public int get(IntEncodedValue property) {
        return 0;
    }

    @Override
    public EdgeIteratorState set(IntEncodedValue property, int value) {
        return null;
    }

    @Override
    public int getReverse(IntEncodedValue property) {
        return 0;
    }

    @Override
    public EdgeIteratorState setReverse(IntEncodedValue property, int value) {
        return null;
    }

    @Override
    public double get(DecimalEncodedValue property) {
        return 0;
    }

    @Override
    public EdgeIteratorState set(DecimalEncodedValue property, double value) {
        return null;
    }

    @Override
    public double getReverse(DecimalEncodedValue property) {
        return 0;
    }

    @Override
    public EdgeIteratorState setReverse(DecimalEncodedValue property, double value) {
        return null;
    }

    @Override
    public <T extends Enum> T get(EnumEncodedValue<T> property) {
        return null;
    }

    @Override
    public <T extends Enum> EdgeIteratorState set(EnumEncodedValue<T> property, T value) {
        return null;
    }

    @Override
    public <T extends Enum> T getReverse(EnumEncodedValue<T> property) {
        return null;
    }

    @Override
    public <T extends Enum> EdgeIteratorState setReverse(EnumEncodedValue<T> property, T value) {
        return null;
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
    public EdgeIteratorState copyPropertiesFrom(EdgeIteratorState e) {
        return null;
    }

}

