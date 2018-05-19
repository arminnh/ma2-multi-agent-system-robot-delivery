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
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import mas.AStar;
import mas.ants.ExplorationAnt;
import mas.ants.IntentionAnt;
import mas.buildings.ChargingStation;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.pizza.PizzaParcel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

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
    private Optional<PizzaParcel> currentParcel;
    private Optional<RandomGenerator> rnd;
    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;
    private Optional<CommDevice> comm;
    private Optional<PizzeriaModel> dtModel;
    private boolean goingToCharge;
    private boolean isCharging;
    private ChargingStation isAtChargingStation;
    private GraphRoadModel graphModel;
    private Optional<Queue<Point>> intention;
    private long intentionTimeLeft;
    private int waitingForExplorationAnts;
    private List<ImmutablePair<List<Point>, Long>> exploredPaths;

    public Robot(VehicleDTO vdto, Battery battery, int id, Point pizzeriaPos, GraphRoadModel graphModel) {
        super(vdto);

        this.id = id;
        this.battery = battery;
        this.pizzeriaPos = pizzeriaPos;
        this.currentParcel = Optional.absent();
        this.timestamp_idle = Optional.absent();
        this.isAtChargingStation = null;
        this.graphModel = graphModel;
        this.intention = Optional.absent();
        this.intentionTimeLeft = Long.MAX_VALUE;
        this.waitingForExplorationAnts = 0;
        this.exploredPaths = new LinkedList<>();
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

    private List<List<Point>> getAlternativePaths(int numberOfPaths, Point start, List<Point> dest) {
        List<List<Point>> S = new LinkedList<>();

        int numFails = 0;
        int maxFails = 3;
        double penalty = 2.0;
        double alpha = rnd.get().nextDouble();
        Table<Point, Point, Double> weights = HashBasedTable.create();

        while (S.size() < numberOfPaths && numFails < maxFails) {
            System.out.print("numFails " + numFails + " alpha " + alpha);

            List<Point> p = AStar.getShortestPath(this.graphModel, weights, start, new LinkedList<>(dest));

            if (S.contains(p)) {
                numFails += 1;
            } else {
                S.add(p);
                numFails = 0;
            }

            for (Point v : p) {
                double beta = rnd.get().nextDouble();
                if (beta < alpha) {

                    // Careful: there is also an getOutgoingConnections
                    for (Point v2 : this.graphModel.getGraph().getIncomingConnections(v)) {
                        if (weights.contains(v, v2)) {
                            weights.put(v, v2, weights.get(v, v2) + penalty);
                        } else {
                            weights.put(v, v2, penalty);
                        }

                        if (weights.contains(v2, v)) {
                            weights.put(v2, v, weights.get(v2, v) + penalty);
                        } else {
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
                    if (this.intention.isPresent()) {
                        this.intention = Optional.absent();
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

        // Read all new messages
        if (this.comm.isPresent()) {
            for (Message m : this.comm.get().getUnreadMessages()) {

                // If an exploration ant has returned, store the explored path and its estimated cost.
                if (m.getContents().getClass() == ExplorationAnt.class) {
                    ExplorationAnt ant = (ExplorationAnt) m.getContents();

                    this.exploredPaths.add(new ImmutablePair<>(ant.path, ant.estimatedTime));

                    this.waitingForExplorationAnts--;

                    System.out.println("GOT ExplorationAnt!!!!");
                    System.out.println(ant.path + ", estimation: " + ant.estimatedTime);
                }

                // Intention ant
                if (m.getContents().getClass() == IntentionAnt.class) {
                    // TODO
                }
            }
        }

        // If waiting for ExplorationAnts and they all have returned, evaluate the different paths
        if (this.exploredPaths.size() > 0 && this.waitingForExplorationAnts == 0) {
            // Select the best path.
            Long bestTime = Long.MAX_VALUE;
            Queue<Point> bestPath = null;

            for (ImmutablePair<List<Point>, Long> p : this.exploredPaths) {
                if (p.right < bestTime) {
                    bestTime = p.right;
                    bestPath = new LinkedList<>(p.left);
                }
            }
            this.exploredPaths = new LinkedList<>();

            // If the new best path is better than the current one, update the intention of the Robot.
            if (!this.intention.isPresent() || bestTime < this.intentionTimeLeft) {
                this.intention = Optional.of(bestPath);
                this.intentionTimeLeft = bestTime;

                // TODO: Send intention ant. If it turns out that the intention ant says that the path cannot be taken
                // TODO: (e.g. because of new road works), then a new set of exploration ants should be sent.
            }
        }


        if (this.intention.isPresent()) {
            this.move(time);

            // TODO: CHECK IF ARRIVED AT DESTINATION
        } else {

        }

//        // No intention, so move to pizzeria
//        if (!this.currentParcel.isPresent()) {
//            toPizzeriaOrChargingStation();
//
//        } else {
//            // Robot has a parcel
//            toCustomerOrChargingStation();
//
//            if (intention.isEmpty()) {
//            }
//        }
//
//        RoadModel rModel = roadModel.get();
//        PDPModel pModel = pdpModel.get();
//        PizzeriaModel dtModel = this.dtModel.get();
//
//        if (currentParcel.isPresent()) {
//            PizzaParcel currParcel = this.currentParcel.get();
//            DeliveryTask currentTask = currParcel.getDeliveryTask();
//
//            if (rModel.equalPosition(this, currentTask)) {
//                // Deliver the pizzas
//                pModel.deliver(this, currParcel, time);
//                dtModel.deliverPizzas(this, currParcel, time.getEndTime());
//
//                // Unload pizzas
//                this.currentCapacity -= currParcel.getAmountPizzas();
//
//                if (currParcel.getDeliveryTask().isFinished()) {
//                    // All pizzas have been delivered, now we have to delete the task.
//                    rModel.removeObject(currParcel.getDeliveryTask());
//                }
//
//                // Remove current task
//                this.currentParcel = Optional.absent();
//
//                // Remove current path
//                this.intention = Optional.absent();
//
//                this.isMovingToDestination = false;
//
//            }
//        } else {
//            if (rModel.getPosition(this) == this.pizzeriaPos) {
//                this.intention = Optional.absent();
//                this.isMovingToDestination = false;
//
//                if (!this.timestamp_idle.isPresent()) {
//                    this.timestamp_idle = Optional.of(time.getEndTime());
//                    System.out.println("SET TIMESTAMP IDLE: " + this.timestamp_idle);
//                }
//            }
//        }
    }

    /**
     * Moves the Robot along its intention.
     */
    private void move(@NotNull TimeLapse time) {
        if (this.intention.isPresent() && this.roadModel.isPresent()) {
            RoadModel rModel = roadModel.get();

            MoveProgress progress;

            if (this.getPosition().get() != this.intention.get().peek()) {
                progress = rModel.moveTo(this, this.intention.get().peek(), time);
            } else {
                progress = rModel.moveTo(this, this.intention.get().remove(), time);
            }

            // progress = roadModel.get().followPath(this, this.intention.get(), time);

            this.metersMoved += progress.distance().doubleValue(SI.METER);
            if (this.metersMoved > 1.0) {
                this.battery.decrementCapacity();
                this.metersMoved -= 1.0;
            }
        }
    }

    private void toCustomerOrChargingStation() {
        if (this.intention.isPresent()) {
            //Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.currentParcel.get().getDeliveryLocation()));
            //this.intention = Optional.of(path);

            //rechargeIfNecessary(this.intention.get(), this.currentParcel.get().getDeliveryLocation());
        }
    }

    private void toPizzeriaOrChargingStation() {
        // Logic to decide if we have to go to the pizzeria or recharge our battery in a charging station

        if (this.intention.isPresent()) {
            //Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.pizzeriaPos));
            //this.intention = Optional.of(path);

            //rechargeIfNecessary(this.intention.get(), this.pizzeriaPos);
        }
    }

    private void rechargeIfNecessary(Queue<Point> path, Point nextStartPos) {
        // If estimatedTime of our path is more than our battery can take, recharge!
        boolean canReachStation = false;
        Queue<Point> newPath = null;

        for (final ChargingStation station : getChargeStations()) {
            Queue<Point> tempPath = new LinkedList<>(roadModel.get().getShortestPathTo(this, station.getPosition()));
            if (newPath == null) {
                newPath = tempPath;
            }

            // Check the path between our destination and the charging station
            Queue<Point> fromDestToCharge = new LinkedList<>(roadModel.get().getShortestPathTo(nextStartPos, station.getPosition()));

            // What is the total estimatedTime of our current path + moving from our current destination to the charging station?
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
            this.intention = Optional.of(newPath);
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

    public void setTask(PizzaParcel parcel) {
        this.currentParcel = Optional.of(parcel);
        this.currentCapacity += parcel.getAmountPizzas();

        // For now, Robots have only 1 destination at a time
        List<Point> dests = new LinkedList<>();
        dests.add(parcel.getDeliveryLocation());

        this.explorePaths(dests);
    }

    private void explorePaths(List<Point> dests) {
        if (this.currentParcel.isPresent()) {
            PizzaParcel p = this.currentParcel.get();

            // Generate 3 possible paths and send exploration ants to explore them
            List<List<Point>> paths = getAlternativePaths(3, p.getPickupLocation(), dests);
            sendExplorationAnts(paths);
        } else {
            System.out.println("Trying to explore paths while there is no currentParcel.");
        }
    }

    private void sendExplorationAnts(List<List<Point>> paths) {
        // Send exploration ants over the found paths
        if (this.comm.isPresent()) {
            for (List<Point> path : paths) {
                this.comm.get().broadcast(new ExplorationAnt(path, 0, false, this.antId, this.id, this));
                this.antId += 1;
            }

            this.waitingForExplorationAnts = paths.size();
        }
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