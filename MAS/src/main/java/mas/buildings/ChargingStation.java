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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

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
    private Queue<Robot> chargingRobots;

    public ChargingStation(Point position, int capacity) {
        this.position = position;
        this.capacity = capacity;
        this.chargingRobots = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = Optional.of(model);

        model.addObjectAt(this, position);
    }

    public Point getPosition() {
        return this.position;
    }

    @Override
    synchronized public void tick(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        // At the moment we don't do anything concerning with the maximum amount of totalVehicles that can station here
        for(Iterator<Robot> it = this.chargingRobots.iterator(); it.hasNext(); ){
            it.next().chargeBattery();
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {

    }

    synchronized public boolean addRobot(Robot r) {
        if(this.chargingRobots.contains(r)){
            return false;
        }
        return this.chargingRobots.add(r);
    }

    public void removeRobot(Robot r){
        this.chargingRobots.remove(r);
    }
}
