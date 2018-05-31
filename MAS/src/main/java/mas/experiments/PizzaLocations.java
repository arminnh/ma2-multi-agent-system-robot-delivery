package mas.experiments;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.generator.Locations;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class PizzaLocations implements Locations.LocationGenerator {
    private List<Point> points;
    PizzaLocations(List<Point> p ){
        points = p;
    }
    @Override
    public ImmutableList<Point> generate(long l, int i) {
        com.google.common.collect.ImmutableList.Builder<Point> locs = ImmutableList.builder();
        locs.addAll(points);
        return locs.build();
    }

    @Override
    public Point getCenter() {
        return null;
    }

    @Override
    public Point getMin() {
        return new Point(0,0);
    }

    @Override
    public Point getMax() {
        return null;
    }
}
