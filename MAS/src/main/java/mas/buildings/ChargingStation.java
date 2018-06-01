package mas.buildings;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import mas.SimulatorSettings;
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
    private Queue<RobotAgent> chargingRobots;

    public ChargingStation(Point position, int capacity) {
        this.position = position;
        this.capacity = capacity;
        this.chargingRobots = new ConcurrentLinkedDeque<>();
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
        if(this.chargingRobots.size() > SimulatorSettings.CHARGING_STATION_CAPACITY){
            throw new IllegalStateException("More robots than allowed!");
        }

        for (RobotAgent robot : this.chargingRobots) {
            robot.chargeBattery();
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
