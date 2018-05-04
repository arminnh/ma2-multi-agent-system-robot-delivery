package mas.buildings;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Implementation of a mas.buildings.Pizzeria
 */
public class Pizzeria implements RoadUser {

    /**
     * For isRegistered implementation, see PDPObjectImpl
     */

    private Point position;
    private Optional<RoadModel> roadModel;

    public Pizzeria(Point position) {
        this.position = position;
    }

    @Override
    public void initRoadUser(RoadModel model) {
        this.roadModel = Optional.of(model);

        model.addObjectAt(this, position);
    }

    public Point getPosition() {
        return this.position;
    }
}
