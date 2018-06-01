package mas.buildings;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import org.jetbrains.annotations.NotNull;


public class Pizzeria implements RoadUser {

    public final Point position;

    public Pizzeria(Point position) {
        this.position = position;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        model.addObjectAt(this, position);
    }

    public Point getPosition() {
        return this.position;
    }
}
