package ethz.ivt.travelTimes;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;


public class CarTravelTimes {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String configFile = args[0];
        String eventsFile = args[1];
        String outputPath = args[2];

        double[] departureTimes = new double[30];
        for(int timebin = 0; timebin < departureTimes.length; timebin++) {
            departureTimes[timebin] = timebin * 3600.0;
        }
        Coord startCoord = new Coord(2682602.7151458208, 1248697.5133739929);
        Coord endCoord = new Coord(2682774.8418166735, 1249038.4213025665);

        CarTravelTimes loadedNetworkRouter = new CarTravelTimes();
        loadedNetworkRouter.run(configFile, eventsFile, departureTimes, startCoord, endCoord, outputPath);
    }

    public void run(String configFile, String eventsFile, double[] departureTimes, Coord startCoord, Coord endCoord, String outputPath) throws IOException {
        Config config = ConfigUtils.loadConfig(configFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Network network = scenario.getNetwork();

        // add algorithm to estimate travel cost
        // and which performs routing based on that
        TravelTimeCalculator travelTimeCalculator = Events2TTCalculator.getTravelTimeCalculator(scenario, eventsFile);
        TravelDisutilityFactory travelCostCalculatorFactory = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() );
        TravelDisutility travelCostCalculator = travelCostCalculatorFactory.createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());

        // create router
        LeastCostPathCalculator router = new DijkstraFactory()
                .createPathCalculator(network, travelCostCalculator, travelTimeCalculator.getLinkTravelTimes());

        // route and write out route and travel time
        Collection<RouteItem> routeItems = new LinkedList<>();
        Node startNode = NetworkUtils.getNearestNode(network, startCoord);
        Node endNode = NetworkUtils.getNearestNode(network, endCoord);

        for(double departureTime : departureTimes) {
            LeastCostPathCalculator.Path path = router.calcLeastCostPath(startNode, endNode, departureTime, null, null);

            System.out.println(path.links.size() + " links along path!" );
            routeItems.add(new RouteItem(departureTime, startCoord, endCoord, path.travelTime, path.links));
        }

        CSVRouteWriter writer = new CSVRouteWriter(routeItems);
        writer.write(outputPath);
    }

}