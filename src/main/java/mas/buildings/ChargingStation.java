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

    public final int capacity;
    public final Point position;
    public final double rechargeCapacity;
    private Queue<RobotAgent> chargingRobots;

    public ChargingStation(Point position, int stationCapacity, double rechargeCapacity) {
        this.position = position;
        this.capacity = stationCapacity;
        this.chargingRobots = new ConcurrentLinkedDeque<>();
        this.rechargeCapacity = rechargeCapacity;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        model.addObjectAt(this, position);
    }

    @Override
    synchronized public void tick(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        if (this.chargingRobots.size() > this.capacity) {
            throw new IllegalStateException("More robots than allowed!");
        }

        for (RobotAgent robot : this.chargingRobots) {
            robot.chargeBattery(this.rechargeCapacity, time);
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {

    }

    synchronized public boolean addRobot(RobotAgent r) {
        return !this.chargingRobots.contains(r) && this.chargingRobots.add(r);

    }

    public void removeRobot(RobotAgent r) {
        this.chargingRobots.remove(r);
    }
}
