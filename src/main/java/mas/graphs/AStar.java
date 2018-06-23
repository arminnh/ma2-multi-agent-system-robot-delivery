package mas.graphs;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AStar {

    public AStar() {
    }

    private static double _roundToClosestEven(double d){
        return Math.round(d / 2) *2 ;
    }

    private static Point roundStartPointToEven(Point start){
        double new_x = _roundToClosestEven(start.x);
        double new_y = _roundToClosestEven(start.y);

        return new Point(new_x, new_y);
    }

    static public List<Point> getShortestPath(Graph<? extends ConnectionData> graph, Table<Point, Point, Double> weights,
                                              Point start, List<Point> dest) {
        // Implementation of A* based on https://en.wikipedia.org/wiki/A*_search_algorithm#Pseudocode
        // Points already evaluated
        List<Point> closedSet = new LinkedList<>();

        // The set of currently discovered nodes that are not evaluated yet.
        // Initially, only the start node is known.
        List<Point> openSet = new LinkedList<>();
        start = roundStartPointToEven(start);

        openSet.add(start);


        HashMap<Point, Point> cameFrom = new HashMap<>();

        // For each node, the cost of getting from the start node to that node.
        HashMap<Point, Double> gScore = getGScore(graph, start);

        HashMap<Point, Double> fScore = getFScore(graph, start, dest.get(0), weights);

        while (!openSet.isEmpty()) {
            //current := the node in openSet having the lowest fScore[] value
            Point current = getLowestFScore(openSet, fScore);

            if (current.equals(dest.get(0))) {
                start = dest.remove(0);

                if (dest.size() > 0) {
                    List<Point> concat = new LinkedList<>();
                    List<Point> l1 = reconstruct_path(cameFrom, current);
                    List<Point> l2 = getShortestPath(graph, weights, start, dest);

                    concat.addAll(l1);
                    concat.addAll(l2.subList(1, l2.size()));
                    return concat;
                } else {
                    return reconstruct_path(cameFrom, current);
                }
            }

            openSet.remove(current);
            closedSet.add(current);

            for (Point neighbor : graph.getOutgoingConnections(current)) {
                if (closedSet.contains(neighbor)) {
                    continue; // Ignore the neighbor which is already evaluated.
                }

                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor); // Discover a new node
                }

                // The estimatedTime from current to a neighbor
                //the "dist_between" function may vary as per the solution requirements.
                double tentative_gScore = gScore.get(current) + heuristic_cost_estimate(current, neighbor, weights);
                if (tentative_gScore >= gScore.get(neighbor)) {
                    continue;        // This is not a better path.
                }

                // This path is the best until now. Record it!
                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentative_gScore);

                fScore.put(neighbor, gScore.get(neighbor) + heuristic_cost_estimate(neighbor, dest.get(0), weights));
            }
        }

        return null;
    }

    private static List<Point> reconstruct_path(HashMap<Point, Point> cameFrom, Point current) {
        /*
         total_path := {current}
         while current in cameFrom.Keys:
            current := cameFrom[current]
            total_path.append(current)
         return total_path
         */
        List<Point> total_path = new LinkedList<>();
        total_path.add(current);
        while (cameFrom.keySet().contains(current)) {
            current = cameFrom.get(current);
            total_path.add(current);
        }

        return new LinkedList<>(Lists.reverse(total_path));
    }

    private static Point getLowestFScore(List<Point> points, HashMap<Point, Double> fScore) {
        Point minPoint = null;
        double minVal = Double.MAX_VALUE;
        for (Point p : points) {
            double fVal = fScore.get(p);
            if (fVal < minVal) {
                minVal = fVal;
                minPoint = p;
            }
        }

        return minPoint;
    }

    private static HashMap<Point, Double> getGScore(Graph<? extends ConnectionData> graph, Point start) {
        // For each node, the cost of getting from the start node to that node.
        HashMap<Point, Double> gScore = new HashMap<>();
        for (Point p : graph.getNodes()) {
            if (p.equals(start)) {
                // The cost of going from start to start is zero.
                gScore.put(start, 0.0);
            } else {
                gScore.put(p, Double.MAX_VALUE);
            }
        }
        return gScore;
    }

    private static HashMap<Point, Double> getFScore(Graph<? extends ConnectionData> graph, Point start, Point dest, Table<Point, Point,
            Double> weights) {
        // For each node, the total cost of getting from the start node to the goal
        // by passing by that node. That value is partly known, partly heuristic.
        HashMap<Point, Double> gScore = new HashMap<>();
        for (Point p : graph.getNodes()) {
            if (p.equals(start)) {
                // For the first node, that value is completely heuristic.
                gScore.put(start, heuristic_cost_estimate(start, dest, weights));
            } else {
                gScore.put(p, Double.MAX_VALUE);
            }
        }
        return gScore;
    }

    private static Double heuristic_cost_estimate(Point start, Point dest, Table<Point, Point,
            Double> weights) {
        if (weights.contains(start, dest)) {
            return dist_between(start, dest) + weights.get(start, dest);
        }
        return dist_between(start, dest);
    }

    private static Double dist_between(Point current, Point neighbor) {
        return Math.abs(current.x - neighbor.x) + Math.abs(current.y - neighbor.y);
    }
}
