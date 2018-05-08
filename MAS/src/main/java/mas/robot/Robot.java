package mas.robot;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import mas.buildings.ChargingStation;
import mas.pizza.DeliveryTask;
import mas.pizza.PizzaParcel;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Implementation of a very simple delivery robot.
 */
public class Robot extends Vehicle implements MovingRoadUser, TickListener, RandomUser, CommUser {

    private int id;
    private Battery battery;
    private double metersMoved = 0;
    private Point pizzeriaPos;
    private int currentCapacity;
    private Optional<Queue<Point>> currentPath;
    private Optional<PizzaParcel> currentParcel;
    public Optional<Long> timestamp_idle;

    private Optional<RandomGenerator> rnd;
    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;
    private Optional<CommDevice> comm;

    public Robot(VehicleDTO vdto, Battery battery, int id, Point pizzeriaPos) {
        super(vdto);

        this.id = id;
        this.battery = battery;
        this.pizzeriaPos = pizzeriaPos;
        this.currentPath = Optional.absent();
        this.currentParcel = Optional.absent();
        this.timestamp_idle = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.pdpModel = Optional.of(pPdpModel);
        this.roadModel = Optional.of(pRoadModel);
    }

    @Override
    public Optional<Point> getPosition() {
        if (roadModel.isPresent()) {
            return Optional.of(roadModel.get().getPosition(this));
        }
        return Optional.absent();
    }

    @Override
    public void setRandomGenerator(@NotNull RandomProvider provider) {
        rnd = Optional.of(provider.newInstance());
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(0.5);
        comm = Optional.of(builder.build());
    }

    private Set<ChargingStation> getChargeStations() {
        return this.roadModel.get().getObjectsOfType(ChargingStation.class);
    }

    private boolean isAtChargeStationRecharging(){
        // We are recharging when we're at a charging station and our current capacity is less than 100%
        for(final ChargingStation station: getChargeStations()){
            if(roadModel.get().equalPosition(this, station)){
                // Okay we are at a charging station
                if(!this.battery.isAtMaxCapacity()){
                    // Remove current path as we don't need it anymore
                    // Current path is the path to the charging station
                    if(this.currentPath.isPresent()){
                        this.currentPath = Optional.absent();
                    }
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft() || isAtChargeStationRecharging() || this.battery.getRemainingCapacity() == 0) {
            return;
        }

        // No parcel so move to pizzeria
        if (!currentParcel.isPresent()) {
            toPizzeriaOrChargingStation();
        } else {
            // We have a parcel
            toCustomerOrChargingStation();
        }

        if (this.currentPath.get().size() > 0) {
            MoveProgress progress = roadModel.get().followPath(this, this.currentPath.get(), time);
            //System.out.println(progress.distance().doubleValue(SI.METER));
            metersMoved += progress.distance().doubleValue(SI.METER);
            if (metersMoved > 1.0) {
                this.battery.decrementCapacity();
                metersMoved -= 1.0;
            }
        }

        RoadModel rModel = roadModel.get();
        PDPModel pModel = pdpModel.get();

        if (currentParcel.isPresent()) {
            PizzaParcel currParcel = this.currentParcel.get();
            DeliveryTask currentTask = currParcel.getDeliveryTask();

            if (rModel.equalPosition(this, currentTask)) {
                // Deliver the pizzas
                pModel.deliver(this, currParcel, time);
                currParcel.getDeliveryTask().deliverPizzas(
                    currParcel.getAmountPizzas(), time.getEndTime(), currParcel, this
                );

                // Unload pizzas
                this.currentCapacity -= currParcel.getAmountPizzas();

                if (currParcel.getDeliveryTask().receivedAllPizzas()) {
                    // All pizzas have been delivered, now we have to delete the task.
                    rModel.removeObject(currParcel.getDeliveryTask());
                }

                // Remove current task
                this.currentParcel = Optional.absent();

                // Remove current path
                this.currentPath = Optional.absent();
            }
        } else {
            if (rModel.getPosition(this) == this.pizzeriaPos) {
                this.currentPath = Optional.absent();

                if (!this.timestamp_idle.isPresent()) {
                    this.timestamp_idle = Optional.of(time.getEndTime());
                    System.out.println("SET TIMESTAMP IDLE: " + this.timestamp_idle);
                }
            }
        }
    }

    private void toCustomerOrChargingStation() {
        if (!this.currentPath.isPresent()) {
            Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.currentParcel.get().getDeliveryLocation()));
            this.currentPath = Optional.of(path);

            rechargeIfNecessary(path, this.currentParcel.get().getDeliveryLocation());
        }
    }

    private void toPizzeriaOrChargingStation() {
        // Logic to decide if we have to go to the pizzeria or recharge our battery in a charging station

        if (!this.currentPath.isPresent()) {
            Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.pizzeriaPos));
            this.currentPath = Optional.of(path);

            rechargeIfNecessary(path, this.pizzeriaPos);
        }
    }

    private void rechargeIfNecessary(Queue<Point> path, Point nextStartPos) {
        // If distance of our path is more than our battery can take, recharge!
        boolean canReachStation = false;
        Queue<Point> newPath = null;

        for (final ChargingStation station : getChargeStations()) {
            Queue<Point> tempPath = new LinkedList<>(roadModel.get().getShortestPathTo(this, station.getPosition()));
            if (newPath == null) {
                newPath = tempPath;
            }

            // Check the path between our destination and the charging station
            Queue<Point> fromDestToCharge = new LinkedList<>(roadModel.get().getShortestPathTo(nextStartPos, station.getPosition()));

            // What is the total distance of our current path + moving from our current destination to the charging station?
            double distToDestThenToChargeStation = Math.ceil(getDistanceOfPathInMeters(path) +  getDistanceOfPathInMeters(fromDestToCharge));

            // If our battery can handle this, then we can reach the station
            if (new Double(distToDestThenToChargeStation).intValue() < battery.getRemainingCapacity()) {
                canReachStation = true;
            }

            if (getDistanceOfPathInMeters(newPath) > getDistanceOfPathInMeters(tempPath)) {
                newPath = tempPath;
            }
        }

        // If we cannot reach any station by first going to our destination and then going to a station
        // We first need to pass through a station
        if(!canReachStation){
            this.currentPath = Optional.of(newPath);
        }

    }

    private double getDistanceOfPathInMeters(Queue<Point> newPath) {
        return roadModel.get().getDistanceOfPath(newPath).doubleValue(SI.METER);
    }

    public int getCurrentBatteryCapacity() {
        return this.battery.getRemainingCapacity();
    }

    public boolean hasTask() {
        return currentParcel.isPresent();
    }

    public int getCapacityLeft() {
        return new Double(this.getCapacity()).intValue() - this.currentCapacity;
    }

    public void setTask(PizzaParcel task) {
        this.currentParcel = Optional.of(task);
        this.currentCapacity += task.getAmountPizzas();
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    public void chargeBattery(){
        this.battery.incrementCapacity();
    }

}