package mas.agents;

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
import mas.buildings.ChargingStation;
import mas.graphs.AStar;
import mas.messages.ExplorationAnt;
import mas.messages.IntentionAnt;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.tasks.DeliveryTask;
import mas.tasks.PizzaParcel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Implementation of a delivery robot.
 */
public class RobotAgent extends Vehicle implements MovingRoadUser, TickListener, RandomUser, CommUser, PizzeriaUser {

    private final int alternativePathsToExplore;
    public Optional<Long> timestampIdle = Optional.absent();
    private RandomGenerator rng;
    private RoadModel roadModel;
    private PDPModel pdpModel;
    private CommDevice commDevice;
    private PizzeriaModel pizzeriaModel;
    private GraphRoadModel graphRoadModel;

    private int id = -1;
    private int antID = 0;
    private Battery battery;
    private int currentCapacity = -1;
    private double metersMoved = 0;
    private Point pizzeriaPosition;
    private boolean goingToCharge = false;
    private boolean isCharging = false;
    private boolean isAtPizzeria = true;
    private int waitingForExplorationAnts = 0;
    private long intentionTimeLeft = Long.MAX_VALUE;
    private Optional<Queue<Point>> intention = Optional.absent();
    private Optional<PizzaParcel> currentParcel = Optional.absent();
    private Optional<ChargingStation> currentChargingStation = Optional.absent();
    private List<ImmutablePair<List<Point>, Long>> exploredPaths = new LinkedList<>();

    public RobotAgent(VehicleDTO vdto, Battery battery, int id, Point pizzeriaPosition, int alternativePathsToExplore) {
        super(vdto);

        this.id = id;
        this.battery = battery;
        this.pizzeriaPosition = pizzeriaPosition;
        this.alternativePathsToExplore = alternativePathsToExplore;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.pdpModel = pPdpModel;
        this.roadModel = pRoadModel;
        this.graphRoadModel = (GraphRoadModel) pRoadModel;
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(roadModel.getPosition(this));
    }

    @Override
    public void setRandomGenerator(@NotNull RandomProvider provider) {
        rng = provider.newInstance();
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(1);
        commDevice = builder.build();
    }

    @Override
    public void initPizzaUser(PizzeriaModel model) {
        pizzeriaModel = model;
    }

    /**
     * The main logic of the robot. Handles messages, (re)evaluates paths and intentions, and moves the robot around
     * the virtual environment.
     */
    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft() || this.isCharging || this.getRemainingBatteryCapacity() == 0) {
            return;
        }

        // Read all new messages
        this.readMessages();

        // If waiting for ExplorationAnts and they all have returned, evaluate the different paths
        if (this.exploredPaths.size() > 0 && this.waitingForExplorationAnts == 0) {
            this.evaluateExploredPathsAndReviseIntentions();
        }

        // TODO: something for intention messages here ??

        if (this.intention.isPresent()) {
            // Make the robot follow its intention. If the robot arrives at the next position it its
            // intention, that position will be removed from the Queue.
            this.move(time);

            // If robot arrived at the destination of the intention, the intention will be empty
            if (this.intention.get().isEmpty()) {
                this.intention = Optional.absent();

                // If the robot was delivering a PizzaParcel
                if (this.currentParcel.isPresent()) {
                    this.deliverPizzaParcel(time);

                    // If the robot was going towards a charging station
                } else if (this.goingToCharge) {
                    this.arriveAtChargingStation();

                    // If the robot was going towards a pizzeria
                } else {
                    this.arriveAtPizzeria(time);
                }
            }
        } else {
            // Choose a new intention. Either towards a Pizzeria or a ChargingStation.

            // Only choose a new intention if not waiting for exploration ants.
            if (this.waitingForExplorationAnts == 0) {
                // Check if the robot should go to a charging station
                if (this.getRemainingBatteryCapacity() <= 35) {
                    // TODO: maybe use the robot's idle timer to see if they have to recharge
                    this.goingToCharge = true;
                    ChargingStation station = this.roadModel.getObjectsOfType(ChargingStation.class).iterator().next();

                    explorePaths(station.getPosition());
                }

                // Only explore new paths if not already at pizzeria and not charging
                if (!this.isAtPizzeria && !this.isCharging) {
                    explorePaths(this.pizzeriaPosition);
                }
            }
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    /**
     * Read all unread messages and take appropriate actions for each message.
     */
    private void readMessages() {
        for (Message m : this.commDevice.getUnreadMessages()) {

            // If an exploration ant has returned, store the explored path and its estimated cost.
            if (m.getContents().getClass() == ExplorationAnt.class) {
                ExplorationAnt ant = (ExplorationAnt) m.getContents();

                this.exploredPaths.add(new ImmutablePair<>(ant.path, ant.estimatedTime));

                this.waitingForExplorationAnts--;

                System.out.println("Robot " + this.id + " got ExplorationAnt for robotID " + ant.robotID);
                System.out.println(ant.path + ", estimation: " + ant.estimatedTime);
            }

            // Intention ant
            if (m.getContents().getClass() == IntentionAnt.class) {
                // TODO
            }
        }
    }

    /**
     * Finds the best path among the ones that were explored by ExplorationAnts. If the best path is better than the
     * one the robot is currently following, the intention will be updated and new IntentionAnts will be sent.
     */
    private void evaluateExploredPathsAndReviseIntentions() {
        Long bestTime = Long.MAX_VALUE;
        Queue<Point> bestPath = null;

        for (ImmutablePair<List<Point>, Long> p : this.exploredPaths) {
            if (p.right < bestTime) {
                bestTime = p.right;
                bestPath = new LinkedList<>(p.left);
            }
        }
        this.exploredPaths = new LinkedList<>();

        // If the new best path is better than the current one, update the intention of the robot.
        if (!this.intention.isPresent() || bestTime < this.intentionTimeLeft) {
            if (bestPath == null) {
                System.err.println("EVALUATED EXPLORED PATHS BUT NO PATH WAS SELECTED AFTER EVALUATION.");
            } else {
                this.intention = Optional.of(bestPath);
                this.intentionTimeLeft = bestTime;
            }

            // TODO: Send intention ant. If it turns out that the intention ant says that the path cannot be taken
            // TODO: (e.g. because of new road works), then a new set of exploration messages should be sent.
        }
    }

    /**
     * Moves the robot along its intention. If the robot reached the next position in the current intention, the
     * position will be removed from the Queue.
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

    /**
     * Delivers a PizzaParcel for a certain DeliveryTask. It will deliver the parcel in the PDPModel, decrease the
     * amount of pizzas for the DeliveryTask in the PizzeriaModel, and decrease the robot's currentCapacity.
     */
    private void deliverPizzaParcel(@NotNull TimeLapse time) {
        if (this.currentParcel.isPresent()) {
            PizzaParcel currParcel = this.currentParcel.get();
            DeliveryTask deliveryTask = currParcel.deliveryTask;

            // If the robot is at the delivery location of the deliveryTask
            if (this.roadModel.equalPosition(this, deliveryTask)) {
                // Deliver the pizzas
                this.pdpModel.deliver(this, currParcel, time);
                this.pizzeriaModel.deliverPizzas(this, currParcel, time.getEndTime());

                // Unload pizzas
                this.currentCapacity -= currParcel.amountOfPizzas;

                // If all pizzas for a deliveryTask have been delivered, the deliveryTask can be removed from the RoadModel.
                if (deliveryTask.isFinished()) {
                    this.roadModel.removeObject(deliveryTask);
                }

                // Remove current PizzaParcel
                this.currentParcel = Optional.absent();
            }
        }
    }

    /**
     * Sets the robot up for charging at a charging station. The robot is linked to the ChargingStation it arrived at.
     */
    private void arriveAtChargingStation() {
        this.goingToCharge = false;
        this.isCharging = true;

        System.out.println("arriveAtChargingStation");

        // Find the charging station the robot arrived at.
        Set<ChargingStation> stations = this.roadModel.getObjectsAt(this, ChargingStation.class);
        ChargingStation station = stations.iterator().next();

        pizzeriaModel.robotArrivedAtChargingStation(this, station);
        this.currentChargingStation = Optional.of(station);
    }

    /**
     * Sets the relevant variables for arriving at a Pizzeria.
     */
    private void arriveAtPizzeria(@NotNull TimeLapse time) {
        if (this.roadModel.getPosition(this) == this.pizzeriaPosition) {
            this.isAtPizzeria = true;

            if (!this.timestampIdle.isPresent()) {
                this.timestampIdle = Optional.of(time.getEndTime());
                System.out.println("SET TIMESTAMP IDLE: " + this.timestampIdle);
            }
        }
    }

    public int getRemainingBatteryCapacity() {
        return this.battery.getRemainingCapacity();
    }

    public int getCapacityLeft() {
        return new Double(this.getCapacity()).intValue() - this.currentCapacity;
    }

    public boolean hasPizzaParcel() {
        return currentParcel.isPresent();
    }

    /**
     * Sets a new PizzaParcel to be delivered by the robot.
     */
    public void setPizzaParcel(PizzaParcel parcel) {
        this.currentParcel = Optional.of(parcel);
        this.currentCapacity += parcel.amountOfPizzas;
        this.isAtPizzeria = false;

        // TODO: move this to tick logic.
        this.explorePaths(parcel.getDeliveryLocation());
    }

    /**
     * Explores different paths towards a single destination.
     */
    private void explorePaths(Point destination) {
        List<Point> destinations = new LinkedList<Point>();
        destinations.add(destination);

        explorePaths(destinations);
    }

    /**
     * Explores different paths towards multiple destinations to be followed sequentially.
     */
    private void explorePaths(List<Point> dests) {
        // Generate a couple of possible paths towards the destination(s) and send exploration messages to explore them.
        List<List<Point>> paths = getAlternativePaths(this.alternativePathsToExplore, this.getPosition().get(), dests);
        sendExplorationAnts(paths);
    }

    /**
     * Sends out ExplorationAnts to explore each of the given paths.
     */
    private void sendExplorationAnts(List<List<Point>> paths) {
        // Send exploration messages over the found paths
        for (List<Point> path : paths) {
            this.commDevice.broadcast(new ExplorationAnt(path, 0, false, this.antID, this.id, this));
            this.antID += 1;
        }

        this.waitingForExplorationAnts = paths.size();
    }

    /**
     * Charges the battery of the robot.
     */
    public void chargeBattery() {
        this.battery.incrementCapacity();

        if (this.battery.isAtMaxCapacity()) {
            this.isCharging = false;
            pizzeriaModel.robotLeftChargingStation(this, this.currentChargingStation.get());
            this.currentChargingStation = Optional.absent();
        }
    }

    /**
     * Creates a set of different paths to be explored by ExplorationAnts. Based on A* + probabilistic penalty approach
     * described in "Multi-agent route planning using delegate MAS." (2016).
     */
    private List<List<Point>> getAlternativePaths(int numberOfPaths, Point start, List<Point> dest) {
        List<List<Point>> alternativePaths = new LinkedList<>();

        int fails = 0;
        int maxFails = 3;
        double penalty = 2.0;
        double alpha = rng.nextDouble();
        Table<Point, Point, Double> weights = HashBasedTable.create();

        while (alternativePaths.size() < numberOfPaths && fails < maxFails) {
            List<Point> path = AStar.getShortestPath(this.graphRoadModel, weights, start, new LinkedList<>(dest));

            if (path != null) {
                if (alternativePaths.contains(path)) {
                    fails += 1;
                } else {
                    alternativePaths.add(path);
                    fails = 0;
                }

                for (Point v : path) {
                    double beta = rng.nextDouble();
                    if (beta < alpha) {

                        // Careful: there is also an getOutgoingConnections
                        for (Point v2 : this.graphRoadModel.getGraph().getIncomingConnections(v)) {
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
            } else {
                System.err.println("GOT null PATH OUT OF A* SEARCH");
            }
        }

        return alternativePaths;
    }

    /**
     * TODO: review this after our new tickImpl logic.
     */
    private void rechargeIfNecessary(Queue<Point> path, Point nextStartPos) {
        // If estimatedTime of our path is more than our battery can take, recharge!
        boolean canReachStation = false;
        Queue<Point> newPath = null;

        for (final ChargingStation station : this.roadModel.getObjectsOfType(ChargingStation.class)) {
            Queue<Point> tempPath = new LinkedList<>(roadModel.getShortestPathTo(this, station.getPosition()));
            if (newPath == null) {
                newPath = tempPath;
            }

            // Check the path between our destination and the charging station
            Queue<Point> fromDestToCharge = new LinkedList<>(roadModel.getShortestPathTo(nextStartPos, station.getPosition()));

            // What is the total estimatedTime of our current path + moving from our current destination to the charging station?
            double distToDestThenToChargeStation = Math.ceil(getDistanceOfPathInMeters(path) + getDistanceOfPathInMeters(fromDestToCharge));

            // If our battery can handle this, then we can reach the station
            if (new Double(distToDestThenToChargeStation).intValue() < this.getRemainingBatteryCapacity()) {
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
}