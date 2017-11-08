package playground.ivt.proj_sccer.graphhopperMM;

import org.matsim.api.core.v01.network.Node;

/**
 * Created by molloyj on 07.11.2017.
 */

public class NodeTimingStruct {
    public double time;
    public final Node node;

    public NodeTimingStruct(Node node, double time) {
        this.node = node; this.time = time;
    }

    @Override
    public String toString() {
        return "{" +
                "node=" + node.getId() +
                ", time=" + time +
                '}';
    }
}


