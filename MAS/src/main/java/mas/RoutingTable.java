package mas;

import com.github.rinde.rinsim.geom.Point;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class RoutingTable {

    // Destination, next hop, distance to destination
    private Map<Point, Map<Point, RoutingTableValue> > rTable;
    private Integer maxLifeTime;
    public RoutingTable(int maxLifeTime){
        rTable = new HashMap<>();
        this.maxLifeTime = maxLifeTime;
    }

    private Pair<Point, Double> _getMinKey(Map<Point, RoutingTableValue> map) {
        Point minKey = null;
        Double minValue = Double.MAX_VALUE;
        for(Point key : map.keySet()) {
            RoutingTableValue value = map.get(key);
            if(value.getDistance() < minValue) {
                minValue = value.getDistance();
                minKey = key;
            }
        }
        return new Pair(minKey, minValue);
    }

    public void decreaseLifeTime(){
        Map<Point, Map<Point, RoutingTableValue> > newTable = new HashMap<>();
        for(Point dest: rTable.keySet()){
            for(Point nextHop: rTable.get(dest).keySet()){
                RoutingTableValue value = rTable.get(dest).get(nextHop);
                value.decreaseTime();
                if(!value.timeUp()){
                    if(!newTable.containsKey(dest)){
                        newTable.put(dest, new HashMap<>());
                    }
                    newTable.get(dest).put(nextHop, value);
                }
            }
        }

        this.rTable = newTable;
    }

    public void addHop(Point destination, Point nextHop, double distance){
        if(!rTable.containsKey(destination)){
            rTable.put(destination, new HashMap<>());
        }

        rTable.get(destination).put(nextHop, new RoutingTableValue(maxLifeTime, distance));
    }

    public boolean hasHopForThrough(Point destination, Point hop) {
        return canReach(destination) && rTable.get(destination).containsKey(hop);
    }

    public double getDistanceFor(Point destination){
        return _getMinKey(rTable.get(destination)).getValue();
    }

    public boolean canReach(Point destination){
        return rTable.containsKey(destination);
    }

    public Point getNextHop(Point destination){
        Map<Point, RoutingTableValue> possiblePaths = rTable.get(destination);
        return _getMinKey(possiblePaths).getKey();
    }
}
