package mas.buildings;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import mas.agents.RobotAgent;
import org.jetbrains.annotations.NotNull;

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
    private RoadModel roadModel;
    private Queue<RobotAgent> chargingRobots;

    public ChargingStation(Point position, int capacity) {
        this.position = position;
        this.capacity = capacity;
        this.chargingRobots = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = model;

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
        for (RobotAgent robot : this.chargingRobots) {
            robot.chargeBattery();
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {

    }

    synchronized public boolean addRobot(RobotAgent r) {
        if (this.chargingRobots.contains(r)) {
            return false;
        }

        return this.chargingRobots.add(r);
    }

    public void removeRobot(RobotAgent r) {
        this.chargingRobots.remove(r);
    }
}
