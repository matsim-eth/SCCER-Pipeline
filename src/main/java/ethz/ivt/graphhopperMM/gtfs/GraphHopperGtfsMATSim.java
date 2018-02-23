/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ethz.ivt.graphhopperMM.gtfs;

        import com.graphhopper.*;
        import com.graphhopper.routing.QueryGraph;
        import com.graphhopper.routing.VirtualEdgeIteratorState;
        import com.graphhopper.routing.util.EncodingManager;
        import com.graphhopper.storage.*;
        import com.graphhopper.storage.index.LocationIndex;
        import com.graphhopper.storage.index.LocationIndexTree;
        import com.graphhopper.storage.index.QueryResult;
        import com.graphhopper.util.*;
        import com.graphhopper.util.exceptions.PointNotFoundException;
        import com.graphhopper.util.shapes.GHPoint;
        import ethz.ivt.graphhopperMM.GraphHopperMATSim;
        import ethz.ivt.graphhopperMM.MATSimNetwork2graphhopper;
        import org.apache.log4j.Logger;
        import org.matsim.api.core.v01.network.Network;
        import org.matsim.core.utils.geometry.CoordinateTransformation;

        import java.io.IOException;
        import java.time.Instant;
        import java.time.format.DateTimeParseException;
        import java.util.*;
        import java.util.stream.Collectors;
        import java.util.stream.Stream;
        import java.util.zip.ZipFile;

        import static com.graphhopper.util.Parameters.PT.PROFILE_QUERY;
/**
 * Created by molloyj on 28.11.2017.
 * A 'decoration' over the normal graphHopper that adds the PT functionality from the gtfsReader.
 * The merging of the two allow different stages of a trip to be routed using the same graphhopper.
 * PT trips are processed using the GTFS extension, and the walk trips are done on the matsim network.
 * unfortunately, due to the structure of the gtfs module in the original respository, a fair bit of code
 * had to be copied over, as certain functionality hadn't been made public.
 */
public class GraphHopperGtfsMATSim extends GraphHopper  {
        private static final Logger logger = Logger.getLogger(GraphHopperGtfsMATSim.class);
        private final PtFlagEncoder flagEncoder;
        private final GtfsStorage gtfsStorage;
        private final RealtimeFeed realtimeFeed;
        private final TripFromLabel tripFromLabel;

        //TODO: fix the PT request handler to give the response that makes sense.
        //have a separate handler for normal requests with waypoints
        //for bike, car, use a different encoder
        private class PtRequestHandler { //TODO: update this so that multiple points can be included
            private final int maxVisitedNodesForRequest;
            private final int limitSolutions;
            private final Instant initialTime;
            private final boolean profileQuery;
            private final boolean separateWalkQuery = false;
            private final boolean arriveBy;
            private final boolean ignoreTransfers;
            private final double walkSpeedKmH;
            private final double maxWalkDistancePerLeg;
            private final double maxTransferDistancePerLeg;
            private final PtTravelTimeWeighting weighting;
            private final GHPoint enter;
            private final GHPoint exit;
            private final List<GHPoint> waypoints;
            private final Translation translation;
            private final List<VirtualEdgeIteratorState> extraEdges = new ArrayList<>(realtimeFeed.getAdditionalEdges());
            private final PointList extraNodes = new PointList();
            private final Map<Integer, PathWrapper> walkPaths = new HashMap<>();

            private final GHResponse response = new GHResponse();
            private final Graph graphWithExtraEdges = new WrapperGraph(getGraphHopperStorage(), extraEdges);
            private QueryGraph queryGraph = new QueryGraph(graphWithExtraEdges);
            private GraphExplorer graphExplorer;

            PtRequestHandler(GHRequest request) {
                maxVisitedNodesForRequest = request.getHints().getInt(Parameters.Routing.MAX_VISITED_NODES, 1_000_000);
                profileQuery = request.getHints().getBool(PROFILE_QUERY, false);
                ignoreTransfers = request.getHints().getBool(Parameters.PT.IGNORE_TRANSFERS, profileQuery);
                limitSolutions = request.getHints().getInt(Parameters.PT.LIMIT_SOLUTIONS, profileQuery ? 5 : ignoreTransfers ? 1 : Integer.MAX_VALUE);
                final String departureTimeString = request.getHints().get(Parameters.PT.EARLIEST_DEPARTURE_TIME, "");
                try {
                    initialTime = Instant.parse(departureTimeString);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(String.format("Illegal value for required parameter %s: [%s]", Parameters.PT.EARLIEST_DEPARTURE_TIME, departureTimeString));
                }
                arriveBy = request.getHints().getBool(Parameters.PT.ARRIVE_BY, false);
                walkSpeedKmH = request.getHints().getDouble(Parameters.PT.WALK_SPEED, 5.0);
                maxWalkDistancePerLeg = request.getHints().getDouble(Parameters.PT.MAX_WALK_DISTANCE_PER_LEG, 1000.0);
                maxTransferDistancePerLeg = request.getHints().getDouble(Parameters.PT.MAX_TRANSFER_DISTANCE_PER_LEG, separateWalkQuery ? -1 : Double.MAX_VALUE);
                weighting = createPtTravelTimeWeighting(flagEncoder, arriveBy, walkSpeedKmH);
                translation = getTranslationMap().getWithFallBack(request.getLocale());
                if (request.getPoints().size() != 2) {
                    throw new IllegalArgumentException("Exactly 2 points have to be specified, but was:" + request.getPoints().size());
                }
                enter = request.getPoints().get(0);
                exit = request.getPoints().get(1);
                waypoints = Collections.EMPTY_LIST;
            }

            GHResponse route() {
                StopWatch stopWatch = new StopWatch().start();

                ArrayList<QueryResult> allQueryResults = new ArrayList<>();

                QueryResult source = findClosest(enter, 0);
                QueryResult dest = findClosest(exit, 1);
                allQueryResults.add(source);
                allQueryResults.add(dest);
                queryGraph.lookup(Arrays.asList(source, dest)); // modifies queryGraph, source and dest!

                PointList startAndEndpoint = pointListFrom(Arrays.asList(source, dest));
                response.addDebugInfo("idLookup:" + stopWatch.stop().getSeconds() + "s");

                if (separateWalkQuery) {
                    substitutePointWithVirtualNode(0, false, enter, allQueryResults);
                    substitutePointWithVirtualNode(1, true, exit, allQueryResults);
                }

                int startNode;
                int destNode;
                if (arriveBy) {
                    startNode = allQueryResults.get(1).getClosestNode();
                    destNode = allQueryResults.get(0).getClosestNode();
                } else {
                    startNode = allQueryResults.get(0).getClosestNode();
                    destNode = allQueryResults.get(1).getClosestNode();
                }
                List<Label> solutions = findPaths(startNode, destNode);
                parseSolutionsAndAddToResponse(solutions, startAndEndpoint);
                return response;
            }

            private void substitutePointWithVirtualNode(int index, boolean reverse, GHPoint ghPoint, ArrayList<QueryResult> allQueryResults) {
                final GraphExplorer graphExplorer = new GraphExplorer(queryGraph, weighting, flagEncoder, gtfsStorage, realtimeFeed, reverse, new PointList(), extraEdges, true);

                extraNodes.add(ghPoint);

                int nextNodeId = getGraphHopperStorage().getNodes() + 10000 + index; // FIXME: A number bigger than the number of nodes QueryGraph adds
                int nextEdgeId = graphWithExtraEdges.getAllEdges().getMaxId() + 100; // FIXME: A number bigger than the number of edges QueryGraph adds

                final List<Label> stationNodes = findStationNodes(graphExplorer, allQueryResults.get(index).getClosestNode(), reverse);
                for (Label stationNode : stationNodes) {
                    final PathWrapper pathWrapper = stationNode.parent.parent != null ?
                            tripFromLabel.parseSolutionIntoPath(reverse, flagEncoder, translation, graphExplorer, weighting, stationNode.parent, new PointList()) :
                            new PathWrapper();
                    final VirtualEdgeIteratorState newEdge = new VirtualEdgeIteratorState(stationNode.edge,
                            nextEdgeId++, reverse ? stationNode.adjNode : nextNodeId, reverse ? nextNodeId : stationNode.adjNode, pathWrapper.getDistance(), 0, "", pathWrapper.getPoints());
                    final VirtualEdgeIteratorState reverseNewEdge = new VirtualEdgeIteratorState(stationNode.edge,
                            nextEdgeId++, reverse ? nextNodeId : stationNode.adjNode, reverse ? stationNode.adjNode : nextNodeId, pathWrapper.getDistance(), 0, "", pathWrapper.getPoints());
                    newEdge.setFlags(((PtFlagEncoder) weighting.getFlagEncoder()).setEdgeType(newEdge.getFlags(), reverse ? GtfsStorage.EdgeType.EXIT_PT : GtfsStorage.EdgeType.ENTER_PT));
                    final long time = pathWrapper.getTime() / 1000;
                    newEdge.setFlags(((PtFlagEncoder) weighting.getFlagEncoder()).setTime(newEdge.getFlags(), time));
                    reverseNewEdge.setFlags(newEdge.getFlags());
                    newEdge.setReverseEdge(reverseNewEdge);
                    reverseNewEdge.setReverseEdge(newEdge);
                    newEdge.setDistance(pathWrapper.getDistance());
                    extraEdges.add(newEdge);
                    extraEdges.add(reverseNewEdge);
                    walkPaths.put(stationNode.adjNode, pathWrapper);
                }

                final QueryResult virtualNode = new QueryResult(ghPoint.getLat(), ghPoint.getLon());
                virtualNode.setClosestNode(nextNodeId);
                allQueryResults.set(index, virtualNode);
            }

            private List<Label> findStationNodes(GraphExplorer graphExplorer, int node, boolean reverse) {
                GtfsStorage.EdgeType edgeType = reverse ? GtfsStorage.EdgeType.EXIT_PT : GtfsStorage.EdgeType.ENTER_PT;
                MultiCriteriaLabelSetting router = new MultiCriteriaLabelSetting(graphExplorer, weighting, reverse, maxWalkDistancePerLeg, maxTransferDistancePerLeg, false, false, maxVisitedNodesForRequest);
                final Stream<Label> labels = router.calcLabels(node, -1, initialTime);
                return labels
                        .filter(current -> current.edge != -1 && flagEncoder.getEdgeType(graphExplorer.getEdgeIteratorState(current.edge, current.adjNode).getFlags()) == edgeType)
                        .collect(Collectors.toList());
            }

            private QueryResult findClosest(GHPoint point, int indexForErrorMessage) {
                QueryResult source = getLocationIndex().findClosest(point.lat, point.lon, new EverythingButPt(flagEncoder));
                if (!source.isValid()) {
                    throw new PointNotFoundException("Cannot find point: " + point, indexForErrorMessage);
                }
                return source;
            }

            private void parseSolutionsAndAddToResponse(List<Label> solutions, PointList waypoints) {
                for (Label solution : solutions) {
                    final List<Trip.Leg> legs = tripFromLabel.getTrip(arriveBy, flagEncoder, translation, graphExplorer, weighting, solution);
                    if (separateWalkQuery) {
                        legs.addAll(0, walkPaths.get(accessNode(solution)).getLegs());
                        legs.addAll(walkPaths.get(egressNode(solution)).getLegs());
                    }
                    final PathWrapper pathWrapper = tripFromLabel.createPathWrapper(translation, waypoints, legs);
                    // TODO: remove
                    pathWrapper.setTime((solution.currentTime - initialTime.toEpochMilli()) * (arriveBy ? -1 : 1));
                    response.add(pathWrapper);
                }
                response.getAll().sort(Comparator.comparingDouble(PathWrapper::getTime));
            }

            private int accessNode(Label solution) {
                if (!arriveBy) {
                    while (solution.parent.parent != null) {
                        solution = solution.parent;
                    }
                    return solution.adjNode;
                } else {
                    return solution.parent.adjNode;
                }
            }

            private int egressNode(Label solution) {
                if (!arriveBy) {
                    return solution.parent.adjNode;
                } else {
                    while(solution.parent.parent != null) {
                        solution = solution.parent;
                    }
                    return solution.adjNode;
                }
            }

            private List<Label> findPaths(int startNode, int destNode) {
                StopWatch stopWatch = new StopWatch().start();
                graphExplorer = new GraphExplorer(queryGraph, weighting, flagEncoder, gtfsStorage, realtimeFeed, arriveBy, extraNodes, extraEdges, false);
                MultiCriteriaLabelSetting router = new MultiCriteriaLabelSetting(graphExplorer, weighting, arriveBy, maxWalkDistancePerLeg, maxTransferDistancePerLeg, !ignoreTransfers, profileQuery, maxVisitedNodesForRequest);
                final Stream<Label> labels = router.calcLabels(startNode, destNode, initialTime);
                List<Label> solutions = labels
                        .filter(current -> destNode == current.adjNode)
                        .limit(limitSolutions)
                        .collect(Collectors.toList());
                response.addDebugInfo("routing:" + stopWatch.stop().getSeconds() + "s");
                if (solutions.isEmpty() && router.getVisitedNodes() >= maxVisitedNodesForRequest) {
                    throw new IllegalArgumentException("No path found - maximum number of nodes exceeded: " + maxVisitedNodesForRequest);
                }
                response.getHints().put("visited_nodes.sum", router.getVisitedNodes());
                response.getHints().put("visited_nodes.average", router.getVisitedNodes());
                if (solutions.isEmpty()) {
                    response.addError(new RuntimeException("No route found"));
                }
                return solutions;
            }
        }

        public GraphHopperGtfsMATSim(PtFlagEncoder flagEncoder,
                                     GraphHopperStorage graphHopperStorage, LocationIndex locationIndex,
                                     GtfsStorage gtfsStorage) {
            this.flagEncoder = flagEncoder;
            this.setGraphHopperStorage(graphHopperStorage);
            this.setLocationIndex(locationIndex);
            this.gtfsStorage = gtfsStorage;
            this.realtimeFeed = RealtimeFeed.empty();
            this.tripFromLabel = new TripFromLabel(this.gtfsStorage);

        }

        public static GtfsStorage createEmptyGtfsStorage() {
            return new GtfsStorage();
        }

        public static GHDirectory createGHDirectory(String graphHopperFolder) {
            return new GHDirectory(graphHopperFolder, DAType.RAM_STORE);
        }

        public static GraphHopperStorage createOrLoad(GHDirectory directory, EncodingManager encodingManager,
                                                      PtFlagEncoder ptFlagEncoder, GtfsStorage gtfsStorage, Collection<String> gtfsFiles,
                                                      String networkFilename, CoordinateTransformation matsim2wgs) {
            boolean createWalkNetwork = false;
            GraphHopperStorage graphHopperStorage = new GraphHopperStorage(directory, encodingManager, false, gtfsStorage);
            if (graphHopperStorage.loadExisting()) {
                return graphHopperStorage;
            } else {
                GraphHopperMATSim graphHopperMATSim = new GraphHopperMATSim(networkFilename, matsim2wgs);
                try {
                    graphHopperMATSim.createReader(graphHopperStorage).readGraph();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


                //new PrepareRoutingSubnetworks(graphHopperStorage, Collections.singletonList(ptFlagEncoder)).doWork();

                int id = 0;
                for (String gtfsFile : gtfsFiles) {
                    try {
                        ((GtfsStorage) graphHopperStorage.getExtension()).loadGtfsFromFile("gtfs_" + id++, new ZipFile(gtfsFile));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
      //          if (createWalkNetwork) {
      //              FakeWalkNetworkBuilder.buildWalkNetwork(((GtfsStorage) graphHopperStorage.getExtension()).getGtfsFeeds().values(), graphHopperStorage, ptFlagEncoder, Helper.DIST_EARTH);
      //          }
      //          LocationIndex walkNetworkIndex;
      //          if (graphHopperStorage.getNodes() > 0) {
      //              walkNetworkIndex = new LocationIndexTree(graphHopperStorage, new RAMDirectory()).prepareIndex();
      //          } else {
      //              walkNetworkIndex = new EmptyLocationIndex();
      //          }
                LocationIndex streetNetworkIndex = new LocationIndexTree(graphHopperStorage, new RAMDirectory()).prepareIndex();
                logger.info("reading graph from gtfs");
                for (int i = 0; i < id; i++) {
                    new GtfsReader("gtfs_" + i, graphHopperStorage, ptFlagEncoder, streetNetworkIndex).readGraph();
                }
                graphHopperStorage.flush();
                return graphHopperStorage;
            }
        }


        public static LocationIndex createOrLoadIndex(GHDirectory directory, GraphHopperStorage graphHopperStorage, PtFlagEncoder flagEncoder) {
            final EverythingButPt everythingButPt = new EverythingButPt(flagEncoder);
            Graph walkNetwork = GraphSupport.filteredView(graphHopperStorage, everythingButPt);
            LocationIndex locationIndex = new LocationIndexTree(walkNetwork, directory);
            if (!locationIndex.loadExisting()) {
                locationIndex.prepareIndex();
            }
            return locationIndex;
        }

        public boolean load(String graphHopperFolder) {
            throw new IllegalStateException("We are always loaded, or we wouldn't exist.");
        }

        @Override
        public GHResponse route(GHRequest request) {
            if(Objects.equals(request.getVehicle(), "pt")) {
                return new PtRequestHandler(request).route();
            }
            else {
                return super.route(request); //FOR other modes, call the normal router.
            }
        }

        private static PtTravelTimeWeighting createPtTravelTimeWeighting(PtFlagEncoder encoder, boolean arriveBy, double walkSpeedKmH) {
            PtTravelTimeWeighting weighting = new PtTravelTimeWeighting(encoder, walkSpeedKmH);
            if (arriveBy) {
                weighting = weighting.reverse();
            }
            return weighting;
        }

        private PointList pointListFrom(List<QueryResult> queryResults) {
            PointList waypoints = new PointList(queryResults.size(), true);
            for (QueryResult qr : queryResults) {
                waypoints.add(qr.getSnappedPoint());
            }
            return waypoints;
        }

    }

