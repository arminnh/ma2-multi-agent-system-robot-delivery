package mas.maps;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of a Charging Station
 */
public class ChargingStation implements RoadUser {

    /**
     * For isRegistered implementation, see PDPObjectImpl
     */

    private int capacity;
    private Point position;
    private Optional<RoadModel> roadModel;

    public ChargingStation(Point position, int capacity) {
        this.position = position;
        this.capacity = capacity;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = Optional.of(model);

        model.addObjectAt(this, position);
    }

    public Point getPosition() {
        return this.position;
    }
}
