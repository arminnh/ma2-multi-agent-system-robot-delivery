package mas.buildings;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import mas.pizza.DeliveryTask;
import mas.pizza.PizzaParcel;
import mas.robot.Robot;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of a Charging Station
 */
public class ChargingStation implements RoadUser, TickListener {

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

    private List<Robot> getWaitingRobots() {
        List<Robot> robots = new LinkedList<>(this.roadModel.get().getObjectsAt(this, Robot.class));

        return robots;
    }

    @Override
    public void tick(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        List<Robot> waitingRobots = getWaitingRobots();

        // At the moment we don't do anything concerning with the maximum amount of robots that can station here
        for(final Robot robot: waitingRobots){
            robot.chargeBattery();
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

}
