package ethz.ivt.externalities.aggregation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;

/**
 * Network:
 * <pre>
 *
 * (n) node
 *  l  link
 *
 *       0       1
 *  (0)-----(1)-----(2)
 *
 *
 * </pre>
 * Coordinates: Distance 1 between nodes on axis.
 *
 *
 * @author ctchervenkov
 */
/*package*/ class Fixture {

    /*package*/ final MutableScenario scenario;
    /*package*/ final Config config;
    /*package*/ final Network network;
    private final Node[] nodes = new Node[3];
    private final Link[] links = new Link[2];

    public Fixture() {
        this.config = ConfigUtils.createConfig();
        this.config.transit().setUseTransit(true);

        ScenarioBuilder scBuilder = new ScenarioBuilder(config) ;
        this.scenario = (MutableScenario) scBuilder.build() ;

        this.network = this.scenario.getNetwork();
    }

    protected void init() {
        buildNetwork();
    }

    protected void buildNetwork() {
        this.nodes[0]  = this.network.getFactory().createNode(Id.create("0", Node.class), new Coord((double) 0, (double) 0));
        this.nodes[1]  = this.network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 1, (double) 0));
        this.nodes[2]  = this.network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 2, (double) 0));
        for (int i = 0; i < nodes.length; i++) {
            this.network.addNode(this.nodes[i]);
        }

        this.links[0]  = this.network.getFactory().createLink(Id.create( "0", Link.class), this.nodes[ 0], this.nodes[ 1]);
        this.links[1]  = this.network.getFactory().createLink(Id.create( "1", Link.class), this.nodes[ 1], this.nodes[ 2]);
        for (int i = 0; i < links.length; i++) {
            this.links[i].setLength(1.0);
            this.links[i].setFreespeed(1.0);
            this.links[i].setCapacity(1.0);
            this.links[i].setNumberOfLanes(1.0);
            this.network.addLink(this.links[i]);
        }
    }
}