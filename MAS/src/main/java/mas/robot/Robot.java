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
import mas.pizza.DeliveryTask;
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
    private RandomGenerator rnd;
    private RoadModel roadModel;
    private PDPModel pdpModel;
    private CommDevice comm;
    private PizzeriaModel dtModel;
    private boolean goingToCharge;
    private boolean isCharging;
    private boolean isAtPizzeria;
    private ChargingStation currentChargingStation;
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
        this.currentChargingStation = null;
        this.graphModel = graphModel;
        this.intention = Optional.absent();
        this.intentionTimeLeft = Long.MAX_VALUE;
        this.waitingForExplorationAnts = 0;
        this.exploredPaths = new LinkedList<>();
        this.isAtPizzeria = true;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.pdpModel = pPdpModel;
        this.roadModel = pRoadModel;
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(roadModel.getPosition(this));
    }

    @Override
    public void setRandomGenerator(@NotNull RandomProvider provider) {
        rnd = provider.newInstance();
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(1);
        comm = builder.build();
    }

    private Set<ChargingStation> getChargeStations() {
        return this.roadModel.getObjectsOfType(ChargingStation.class);
    }

    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft() || this.isCharging || this.battery.getRemainingCapacity() == 0) {
            return;
        }

        // Read all new messages
        readMessages();

        // If waiting for ExplorationAnts and they all have returned, evaluate the different paths
        if (this.exploredPaths.size() > 0 && this.waitingForExplorationAnts == 0) {
            evaluateExploredPathsAndReviseIntentions();
        }

        // TODO: something for intention ants here ??

        if (this.intention.isPresent()) {
            // Make the Robot follow its intention. If the Robot arrives at the next position it its intention,
            // that position will be removed from the Queue.
            this.move(time);

            // If Robot arrived at the destination of the intention, the intention will be empty
            if (this.intention.get().isEmpty()) {
                this.intention = Optional.absent();

                // If the Robot was delivering a PizzaParcel
                if (this.currentParcel.isPresent()) {
                    deliverParcel(time);

                    // If the robot was going towards a charging station
                } else if (this.goingToCharge) {
                    arriveAtChargingStation();

                    // If the robot was going towards a pizzeria
                } else {
                    arriveAtPizzeria(time);
                }
            }
        } else {
            // Choose a new intention. Either towards a Pizzeria or a ChargingStation.

            // Only choose a new intention if not waiting for exploration ants.
            if (this.waitingForExplorationAnts == 0) {
                // Check if the Robot should go to a charging station
                if (this.getCurrentBatteryCapacity() <= 35) {
                    // TODO: maybe use the robot's idle timer to see if they have to recharge
                    this.goingToCharge = true;
                    ChargingStation station = this.roadModel.getObjectsOfType(ChargingStation.class).iterator().next();

                    explorePaths(station.getPosition());
                }

                // Only explore new paths if not already at pizzeria and not charging
                if (!this.isAtPizzeria && !this.isCharging) {
                    explorePaths(this.pizzeriaPos);
                }
            }
        }
    }

    private void readMessages() {
        for (Message m : this.comm.getUnreadMessages()) {

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

    private void evaluateExploredPathsAndReviseIntentions() {
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

    /**
     * Moves the Robot along its intention.
     */
    private void move(@NotNull TimeLapse time) {
        if (this.intention.isPresent()) {
            MoveProgress progress;

            if (this.getPosition().get() != this.intention.get().peek()) {
                progress = this.roadModel.moveTo(this, this.intention.get().peek(), time);
            } else {
                progress = this.roadModel.moveTo(this, this.intention.get().remove(), time);
            }

            // progress = roadModel.followPath(this, this.intention.get(), time);

            this.metersMoved += progress.distance().doubleValue(SI.METER);
            if (this.metersMoved > 1.0) {
                this.battery.decrementCapacity();
                this.metersMoved -= 1.0;
            }
        }
    }

    private void deliverParcel(@NotNull TimeLapse time) {
        if (this.currentParcel.isPresent()) {
            PizzaParcel currParcel = this.currentParcel.get();
            DeliveryTask deliveryTask = currParcel.getDeliveryTask();

            // If the Robot is at the delivery location of the task
            if (this.roadModel.equalPosition(this, deliveryTask)) {
                // Deliver the pizzas
                this.pdpModel.deliver(this, currParcel, time);
                this.dtModel.deliverPizzas(this, currParcel, time.getEndTime());

                // Unload pizzas
                this.currentCapacity -= currParcel.getAmountPizzas();

                // If all pizzas for a task have been delivered, the task can be removed from the RoadModel.
                if (deliveryTask.isFinished()) {
                    this.roadModel.removeObject(deliveryTask);
                }

                // Remove current PizzaParcel
                this.currentParcel = Optional.absent();
            }
        }
    }

    private void arriveAtChargingStation() {
        this.goingToCharge = false;
        this.isCharging = true;

        System.out.println("arriveAtChargingStation");

        // Find the charging station the Robot arrived at
        Set<ChargingStation> stations = this.roadModel.getObjectsAt(this, ChargingStation.class);
        ChargingStation station = stations.iterator().next();

        dtModel.robotAtChargingStation(this, station);
        this.currentChargingStation = station;
    }

    private void arriveAtPizzeria(@NotNull TimeLapse time) {
        if (this.roadModel.getPosition(this) == this.pizzeriaPos) {
            this.isAtPizzeria = true;

            if (!this.timestamp_idle.isPresent()) {
                this.timestamp_idle = Optional.of(time.getEndTime());
                System.out.println("SET TIMESTAMP IDLE: " + this.timestamp_idle);
            }
        }
    }

    private void rechargeIfNecessary(Queue<Point> path, Point nextStartPos) {
        // If estimatedTime of our path is more than our battery can take, recharge!
        boolean canReachStation = false;
        Queue<Point> newPath = null;

        for (final ChargingStation station : getChargeStations()) {
            Queue<Point> tempPath = new LinkedList<>(roadModel.getShortestPathTo(this, station.getPosition()));
            if (newPath == null) {
                newPath = tempPath;
            }

            // Check the path between our destination and the charging station
            Queue<Point> fromDestToCharge = new LinkedList<>(roadModel.getShortestPathTo(nextStartPos, station.getPosition()));

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
        return roadModel.getDistanceOfPath(newPath).doubleValue(SI.METER);
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
        this.isAtPizzeria = false;

        this.explorePaths(parcel.getDeliveryLocation());
    }

    private void explorePaths(Point destination) {
        List<Point> destinations = new LinkedList<Point>();
        destinations.add(destination);

        explorePaths(destinations);
    }

    private void explorePaths(List<Point> dests) {
        // Generate 3 possible paths and send exploration ants to explore them
        List<List<Point>> paths = getAlternativePaths(3, this.getPosition().get(), dests);
        sendExplorationAnts(paths);
    }

    private void sendExplorationAnts(List<List<Point>> paths) {
        // Send exploration ants over the found paths
        for (List<Point> path : paths) {
            this.comm.broadcast(new ExplorationAnt(path, 0, false, this.antId, this.id, this));
            this.antId += 1;
        }

        this.waitingForExplorationAnts = paths.size();
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    public void chargeBattery() {
        this.battery.incrementCapacity();

        if (this.battery.isAtMaxCapacity()) {
            this.isCharging = false;
            dtModel.robotLeavingChargingStation(this, this.currentChargingStation);
            this.currentChargingStation = null;
        }
    }

    @Override
    public void initPizzaUser(PizzeriaModel model) {
        dtModel = model;
    }

    private List<List<Point>> getAlternativePaths(int numberOfPaths, Point start, List<Point> dest) {
        List<List<Point>> S = new LinkedList<>();

        int numFails = 0;
        int maxFails = 3;
        double penalty = 2.0;
        double alpha = rnd.nextDouble();
        Table<Point, Point, Double> weights = HashBasedTable.create();

        while (S.size() < numberOfPaths && numFails < maxFails) {
            List<Point> p = AStar.getShortestPath(this.graphModel, weights, start, new LinkedList<>(dest));

            if (S.contains(p)) {
                numFails += 1;
            } else {
                S.add(p);
                numFails = 0;
            }

            for (Point v : p) {
                double beta = rnd.nextDouble();
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
}