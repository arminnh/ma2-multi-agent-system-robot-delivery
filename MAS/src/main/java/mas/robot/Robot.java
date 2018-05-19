package mas.robot;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import mas.AStar;
import mas.ants.ExplorationAnt;
import mas.buildings.ChargingStation;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.pizza.DeliveryTask;
import mas.pizza.PizzaParcel;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;
import java.util.*;

/**
 * Implementation of a very simple delivery robot.
 */
public class Robot extends Vehicle implements MovingRoadUser, TickListener, RandomUser, CommUser, PizzeriaUser {

    public Optional<Long> timestamp_idle;
    private int id;
    private int antId = 0;
    private Battery battery;
    private double metersMoved = 0;
    private Point pizzeriaPos;
    private int currentCapacity;
    private Optional<Queue<Point>> currentPath;
    private Optional<PizzaParcel> currentParcel;
    private Optional<RandomGenerator> rnd;
    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;
    private Optional<CommDevice> comm;
    private Optional<PizzeriaModel> dtModel;
    private boolean goingToCharge;
    private boolean isCharging;
    private ChargingStation isAtChargingStation;
    private boolean isMovingToDestination = false;
    private GraphRoadModel graphModel;

    public Robot(VehicleDTO vdto, Battery battery, int id, Point pizzeriaPos, GraphRoadModel graphModel) {
        super(vdto);

        this.id = id;
        this.battery = battery;
        this.pizzeriaPos = pizzeriaPos;
        this.currentPath = Optional.absent();
        this.currentParcel = Optional.absent();
        this.timestamp_idle = Optional.absent();
        this.isAtChargingStation = null;
        this.graphModel = graphModel;
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
        builder.setMaxRange(1);
        comm = Optional.of(builder.build());
    }

    private Set<ChargingStation> getChargeStations() {
        return this.roadModel.get().getObjectsOfType(ChargingStation.class);
    }

    private List<List<Point>> getAlternativePaths(int numberOfPaths, Point start, List<Point> dest){
        List<List<Point>> S = new LinkedList<>();

        int numFails = 0;
        int maxFails = 3;
        double penalty = 2.0;
        double alpha = rnd.get().nextDouble();
        Table<Point, Point, Double> weights = HashBasedTable.create();

        while(S.size() < numberOfPaths && numFails < maxFails){
            System.out.print("numFails " + numFails + " alpha " + alpha);

            List<Point> p = AStar.getShortestPath(this.graphModel, weights , start, new LinkedList<>(dest));

            if(S.contains(p)){
                numFails += 1;
            }else{
                S.add(p);
                numFails = 0;
            }

            for(Point v: p){
                double beta = rnd.get().nextDouble();
                if(beta < alpha){

                    // Careful: there is also an getOutgoingConnections
                    for(Point v2: this.graphModel.getGraph().getIncomingConnections(v)){
                        if(weights.contains(v, v2)){
                            weights.put(v, v2, weights.get(v, v2) + penalty);
                        }else{
                            weights.put(v, v2, penalty);
                        }

                        if(weights.contains(v2, v)) {
                            weights.put(v2, v, weights.get(v2, v) + penalty);
                        }else{
                            weights.put(v2, v, penalty);

                        }
                    }
                }
            }
        }

        return S;
    }

    private boolean isAtChargeStationRecharging() {
        // We are recharging when we're at a charging station and our current capacity is less than 100%
        if (this.goingToCharge) {
            // Check if we're at charging station
            for (final ChargingStation station : getChargeStations()) {
                if (roadModel.get().equalPosition(this, station)) {
                    dtModel.get().robotAtChargingStation(this, station);
                    this.goingToCharge = false;
                    this.isCharging = true;
                    this.isAtChargingStation = station;

                    // Remove current path as we don't need it anymore
                    // Current path is the path to the charging station
                    if (this.currentPath.isPresent()) {
                        this.currentPath = Optional.absent();
                    }
                    return true;
                }
            }
        }

        if (this.isCharging) {
            // Okay we are at a charging station
            return !this.battery.isAtMaxCapacity();
        }

        return false;
    }

    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft() || isAtChargeStationRecharging() || this.battery.getRemainingCapacity() == 0) {
            return;
        }

        if(!this.currentPath.isPresent()){
            for(Message m: this.comm.get().getUnreadMessages()){
                if(m.getContents().getClass() == ExplorationAnt.class){
                    ExplorationAnt ant = (ExplorationAnt) m.getContents();

                    if(ant.hasReachedDestination() && ant.getRobot_id() == this.id){
                        System.out.println("GOT PATH!!!!");
                        // We have found a path for our current parcel
                        //System.out.println(ant.getPath());
                        //this.currentPath = Optional.of(new LinkedList<Point>(ant.getPath()));
                        //System.out.println(this.getPosition().get());
                        //this.isMovingToDestination = true;
                    }
                }
            }
        }

        // No parcel so move to pizzeria
        if (!currentParcel.isPresent()) {
            toPizzeriaOrChargingStation();
        } else {
            // We have a parcel
            toCustomerOrChargingStation();
        }

        if (this.isMovingToDestination) {
            MoveProgress progress;
            //System.out.println(this.currentPath.get());
            //System.out.println(this.getPosition().get());
            if(this.getPosition().get() != this.currentPath.get().peek()){
                progress = roadModel.get().moveTo(this, this.currentPath.get().peek(), time);
            }else{
                progress = roadModel.get().moveTo(this, this.currentPath.get().remove(), time);
            }
            //MoveProgress
            //progress = roadModel.get().followPath(this, this.currentPath.get(), time);
            //System.out.println(progress.distance().doubleValue(SI.METER));
            metersMoved += progress.distance().doubleValue(SI.METER);
            if (metersMoved > 1.0) {
                this.battery.decrementCapacity();
                metersMoved -= 1.0;
            }
        }

        RoadModel rModel = roadModel.get();
        PDPModel pModel = pdpModel.get();
        PizzeriaModel dtModel = this.dtModel.get();

        if (currentParcel.isPresent()) {
            PizzaParcel currParcel = this.currentParcel.get();
            DeliveryTask currentTask = currParcel.getDeliveryTask();

            if (rModel.equalPosition(this, currentTask)) {
                // Deliver the pizzas
                pModel.deliver(this, currParcel, time);
                dtModel.deliverPizzas(this, currParcel, time.getEndTime());

                // Unload pizzas
                this.currentCapacity -= currParcel.getAmountPizzas();

                if (currParcel.getDeliveryTask().isFinished()) {
                    // All pizzas have been delivered, now we have to delete the task.
                    rModel.removeObject(currParcel.getDeliveryTask());
                }

                // Remove current task
                this.currentParcel = Optional.absent();

                // Remove current path
                this.currentPath = Optional.absent();

                this.isMovingToDestination = false;
                sendExplorationAnt(this.pizzeriaPos);

            }
        } else {
            if (rModel.getPosition(this) == this.pizzeriaPos) {
                this.currentPath = Optional.absent();
                this.isMovingToDestination = false;

                if (!this.timestamp_idle.isPresent()) {
                    this.timestamp_idle = Optional.of(time.getEndTime());
                    System.out.println("SET TIMESTAMP IDLE: " + this.timestamp_idle);
                }
            }
        }
    }

    private void toCustomerOrChargingStation() {
        if (this.currentPath.isPresent()) {
            //Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.currentParcel.get().getDeliveryLocation()));
            //this.currentPath = Optional.of(path);

            //rechargeIfNecessary(this.currentPath.get(), this.currentParcel.get().getDeliveryLocation());
        }
    }

    private void toPizzeriaOrChargingStation() {
        // Logic to decide if we have to go to the pizzeria or recharge our battery in a charging station

        if (this.currentPath.isPresent()) {
            //Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.pizzeriaPos));
            //this.currentPath = Optional.of(path);

            //rechargeIfNecessary(this.currentPath.get(), this.pizzeriaPos);
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
            double distToDestThenToChargeStation = Math.ceil(getDistanceOfPathInMeters(path) + getDistanceOfPathInMeters(fromDestToCharge));

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
        if (!canReachStation) {
            this.currentPath = Optional.of(newPath);
            this.goingToCharge = true;

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

        List<Point> dests = new LinkedList<>();
        dests.add(task.getDeliveryLocation());

        List<List<Point>> paths = getAlternativePaths(3, task.getPickupLocation(),dests);
        sendExplorationAnts(paths);
    }

    private void sendExplorationAnts(List<List<Point>> paths) {
        // Send exploration ants over the found paths
        for(List<Point> path: paths){
            this.comm.get().broadcast(new ExplorationAnt(path, this.id, this.antId, this));
        }
        this.antId += 1;
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    public void chargeBattery() {
        this.battery.incrementCapacity();
        if (this.battery.isAtMaxCapacity()) {
            this.isCharging = false;
            dtModel.get().robotLeavingChargingStation(this, this.isAtChargingStation);
            this.isAtChargingStation = null;
        }
    }

    @Override
    public void initPizzaUser(PizzeriaModel model) {
        dtModel = Optional.of(model);
    }
}