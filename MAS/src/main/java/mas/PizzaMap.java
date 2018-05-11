package mas;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PizzaMap {
    /*
     start: (0,1)
     final dest: (0,3)
     paths: { -> (0,2), 1
              -> (1,1), 3 (-> (1,2) -> (1,3) -> (0,3))
             }
      */
    private Map<Point, Map<Point, Map<Point, Double> > > worldView;

    public PizzaMap(){
        worldView = new ConcurrentHashMap<>();
    }

    private Point _getMinKey(Map<Point, Double> map) {
        Point minKey = null;
        Double minValue = Double.MAX_VALUE;
        for(Point key : map.keySet()) {
            Double value = map.get(key);
            if(value < minValue) {
                minValue = value;
                minKey = key;
            }
        }
        return minKey;
    }

    // Gets the next location we need to go to such that we can move from our current position to our destination
    public Point nextLocationFor(Point currentPosition, Point destination){
        Map<Point, Double> possiblePaths = worldView.get(currentPosition).get(destination);
        return _getMinKey(possiblePaths);
    }


}
