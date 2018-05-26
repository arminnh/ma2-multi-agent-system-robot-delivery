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
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import mas.DeliveryTaskData;
import mas.buildings.ChargingStation;
import mas.graphs.AStar;
import mas.messages.*;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.tasks.DeliveryTask;
import mas.tasks.PizzaParcel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.analysis.function.Exp;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.measure.unit.SI;
import javax.swing.text.html.Option;
import java.util.*;

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
    private Queue<PizzaParcel> currentParcels = new LinkedList<>();
    private Optional<ChargingStation> currentChargingStation = Optional.absent();
    private List<ExplorationAnt> explorationAnts = new LinkedList<>();
    private int waitingForDesireAnts = 0;
    private HashMap<DesireAnt, Long> desires = new HashMap<>();
    private boolean goingToPizzeria = false;
    private int waitingForIntentionAnt = 0;

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
        // TODO: Make sure that the static map never changes when we add a roadwork!!
        // Maybe need to do a copy!
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

    private List<DesireAnt> getHighestDesires() {
        System.out.println(this.desires);

        List<DesireAnt> bestTasks = new LinkedList<>();
        int remainingCapacity = this.getCapacityLeft();

        // Get all tasks with highest score sorted in descending
        for (Map.Entry<DesireAnt, Long> entry : sortMapDescending(this.desires)) {
            int capacity = Math.min(remainingCapacity, entry.getValue().intValue());

            if (capacity > 0) {
                bestTasks.add(entry.getKey());
                remainingCapacity -= capacity;
            } else {
                break;
            }
        }
        return bestTasks;
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
        this.readMessages(time);

        // If waiting for ExplorationAnts and they all have returned, evaluate the different paths
        if (this.explorationAnts.size() > 0 && this.waitingForExplorationAnts == 0) {
            this.evaluateExploredPathsAndReviseIntentions();
        }

        if (!this.desires.isEmpty() && this.waitingForDesireAnts == 0) {
            System.out.println(this.desires);
            // Decide on desire
            List<DesireAnt> bestTasks = getHighestDesires();
            List<DeliveryTaskData> deliveries = new LinkedList<>();
            System.out.println("BestTasks " + bestTasks);

            int remainingCapacity = this.getCapacityLeft();
            for (DesireAnt ant : bestTasks) {
                Point destination = ant.path.get(ant.path.size() - 1);

                int pizzas = Math.min(remainingCapacity, ant.pizzas);
                remainingCapacity -= pizzas;

                deliveries.add(new DeliveryTaskData(destination, this.id, ant.deliveryTaskID, pizzas, false));
            }

            explorePaths(deliveries);
            if(deliveries.size() > 1){
                explorePaths(Lists.reverse(deliveries));
            }
            this.desires.clear();
        }

        // TODO: something for intention messages here ??
        if (!this.currentParcels.isEmpty() && !this.intention.isPresent()) {
            // Exploration or wait on ants
            System.out.println("Nothing to see here");


        } else if (this.intention.isPresent()) {
            // Make the robot follow its intention. If the robot arrives at the next position it its
            // intention, that position will be removed from the Queue.
            this.move(time);

            //intentionReconsideration();

            if (!this.currentParcels.isEmpty() &&
                    this.currentParcels.peek().getDeliveryLocation() == this.getPosition().get()) {
                System.out.println("Pizza delivery");

                this.deliverPizzaParcel(time);
            }

            // If robot arrived at the destination of the intention, the intention will be empty
            if (this.intention.get().isEmpty()) {
                System.out.println("intention is empty");

                this.intention = Optional.absent();

                // If the robot was delivering a PizzaParcel
                if (this.goingToCharge) {
                    this.arriveAtChargingStation();
                    // If the robot was going towards a pizzeria
                } else if(this.goingToPizzeria) {
                    this.arriveAtPizzeria(time);
                }
            }
        } else {
            // No intention is present and no desire. First choose a new desire.


            // Only choose a new intention if not waiting for exploration ants.
            if (this.waitingForExplorationAnts == 0 && this.waitingForIntentionAnt == 0 && waitingForDesireAnts == 0) {
                // If at pizzeria ask for tasks
                if (this.isAtPizzeria) {
                    List<DeliveryTask> tasks = pizzeriaModel.getDeliveryTasks();
                    if(!tasks.isEmpty()){
                        sendDesireAntsForTasks(tasks);
                        return;
                    }
                }

                // If a new parcel has been set on the robot, explore paths towards the destination of the parcel.
                // Search new intentions
                if (!this.currentParcels.isEmpty()) {
                    List<DeliveryTaskData> deliveries = new LinkedList<>();

                    for (PizzaParcel parcel: this.currentParcels) {
                        deliveries.add(new DeliveryTaskData(parcel.getDeliveryLocation(), this.id,
                                parcel.deliveryTaskID, parcel.amountOfPizzas, false));
                    }

                    this.explorePaths(deliveries);

                    // Otherwise, either go to a charging station to charge or a pizzeria to pick up a new parcel.
                } else {
                    // Check if the robot should go to a charging station
                    if (this.getRemainingBatteryCapacity() <= 35) {
                        System.out.println("GoingToCharge");
                        // TODO: maybe use the robot's idle timer to see if they have to recharge
                        this.goingToCharge = true;
                        ChargingStation station = this.roadModel.getObjectsOfType(ChargingStation.class).iterator().next();

                        explorePaths(station.getPosition());
                    }

                    // Only explore new paths towards pizzeria if not already at pizzeria and not charging
                    if (!this.isAtPizzeria && !this.isCharging && !this.goingToCharge) {
                        explorePaths(this.pizzeriaPosition);
                        this.goingToPizzeria = true;
                    }
                }
            }
        }
    }

    private void sendDesireAntsForTasks(List<DeliveryTask> tasks) {
        System.out.println("sendDesireAntsForTasks");
        for (DeliveryTask task : tasks) {
            List<Point> path = this.roadModel.getShortestPathTo(this, task.getPosition().get());
            DesireAnt desireAnt = new DesireAnt(path, 0,
                    false, this.id, this, null, task.id, 0);
            this.commDevice.broadcast(desireAnt);
            this.waitingForDesireAnts++;
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    /**
     * Read all unread messages and take appropriate actions for each message.
     */
    private void readMessages(TimeLapse time) {
        for (Message m : this.commDevice.getUnreadMessages()) {

            if(Ant.class.isInstance(m.getContents())){
                Ant ant = (Ant) m.getContents();
                if(ant.robotID != this.id){
                    continue;
                }
            }

            // If an exploration ant has returned, store the explored path and its estimated cost.
            if (m.getContents().getClass() == ExplorationAnt.class) {
                handleExplorationAntMessage(m);
            }

            // Intention ant
            if (m.getContents().getClass() == IntentionAnt.class) {
                handleIntentionAntMessage(time, m);
            }

            if (m.getContents().getClass() == DesireAnt.class) {
                handleDesireAntMessage(m);
            }
        }
    }

    private void handleExplorationAntMessage(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();

        this.explorationAnts.add(ant);
        this.waitingForExplorationAnts--;

        System.out.println("Robot " + this.id + " got ExplorationAnt for robotID " + ant.robotID);
        System.out.println(ant.path + ", estimation: " + ant.estimatedTime);
    }

    private void handleDesireAntMessage(Message m) {
        System.out.println("Got desire ant");
        DesireAnt ant = (DesireAnt) m.getContents();
        if(ant.pizzas > 0){
            this.desires.put(ant, ant.score);
            System.out.println( this.desires);
        }

        this.waitingForDesireAnts--;
    }

    private void handleIntentionAntMessage(TimeLapse time, Message m) {
        IntentionAnt ant = (IntentionAnt) m.getContents();

        if (ant.toChargingStation) {
            // TODO: Charging logic
            //System.out.println("CHARGING");
            this.intention = Optional.of(new LinkedList<>(ant.path));
        } else {
            for (DeliveryTaskData deliveryTaskData: ant.deliveries) {
                // Check if we don't have a parcel yet and if the reservation is confirmed
                if (deliveryTaskData.reservationConfirmed && this.getParcelForDeliveryTaskID(deliveryTaskData) == null) {
                    PizzaParcel parcel = this.pizzeriaModel.newPizzaParcel(deliveryTaskData.deliveryTaskID,
                            this.getPosition().get(), deliveryTaskData.pizzas, time.getStartTime());

                    this.pdpModel.pickup(this, parcel, time);

                    this.addPizzaParcel(parcel);
                }else{
                    System.out.println("Reservation denied");
                    if(this.intention.isPresent()){
                        // There was a reservation denied
                        // Get the correct parcel from our current parcels
                        PizzaParcel removeParcel = getParcelForDeliveryTaskID(deliveryTaskData);

                        if(removeParcel != null){
                            this.currentParcels.remove(removeParcel);

                            // Notify the model that we're dropping a parcel
                            this.pizzeriaModel.dropParcel(this, removeParcel, time);

                            // Unregister the parcel from pdp
                            this.pdpModel.unregister(removeParcel);
                        }
                    }
                }
            }

            if(this.hasPizzaParcel()){
                this.intention = Optional.of(new LinkedList<>(ant.path));
            }else{
                System.out.println("d");

            }
        }
        this.waitingForIntentionAnt--;
    }

    @Nullable
    private PizzaParcel getParcelForDeliveryTaskID(DeliveryTaskData deliveryTaskData) {
        PizzaParcel removeParcel = null;
        for(PizzaParcel p : this.currentParcels){
            if(p.deliveryTaskID == deliveryTaskData.deliveryTaskID){
                removeParcel = p;
            }
        }
        return removeParcel;
    }

    /**
     * Finds the best path among the ones that were explored by ExplorationAnts. If the best path is better than the
     * one the robot is currently following, the intention will be updated and new IntentionAnts will be sent.
     */
    private void evaluateExploredPathsAndReviseIntentions() {
        Long bestTime = Long.MAX_VALUE;
        ExplorationAnt bestAnt = null;

        for (ExplorationAnt ant : this.explorationAnts) {
            if (ant.estimatedTime < bestTime) {
                bestTime = ant.estimatedTime;
                bestAnt = ant;

            }
        }
        this.explorationAnts.clear();

        // If the new best path is better than the current one, update the intention of the robot.
        if (!this.intention.isPresent() || bestTime < this.intentionTimeLeft) {
            if(this.goingToPizzeria){
                this.intention = Optional.of(new LinkedList<>(bestAnt.path));
            }else{
                this.sendIntentionAnt(bestAnt);
            }
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
        PizzaParcel parcel = this.currentParcels.peek();
        DeliveryTask deliveryTask = parcel.deliveryTask;

        // If the robot is at the delivery position of the deliveryTask
        if (this.roadModel.equalPosition(this, deliveryTask)) {
            // Deliver the pizzas
            this.pdpModel.deliver(this, parcel, time);
            this.pizzeriaModel.deliverPizzas(this, parcel, time.getEndTime());

            // Unload pizzas
            this.currentCapacity -= parcel.amountOfPizzas;

            // Remove current PizzaParcel
            this.currentParcels.remove();

        } else {
            System.err.println("TRYING TO DELIVER FROM WRONG POSITION");
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
            System.out.println("User at pizzeria!");
            this.isAtPizzeria = true;
            this.goingToPizzeria = false;

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
        return this.currentParcels.size() > 0;
    }

    /**
     * Sets a new PizzaParcel to be delivered by the robot.
     */
    public void addPizzaParcel(PizzaParcel parcel) {
        this.currentParcels.add(parcel);
        this.currentCapacity += parcel.amountOfPizzas;
        this.isAtPizzeria = false;
        System.out.println("Current parcels: " + this.currentParcels.size());
    }

    /**
     * Explores different paths towards a single destination.
     */
    private void explorePaths(Point destination) {
        List<DeliveryTaskData> deliveries = new LinkedList<>();
        deliveries.add(new DeliveryTaskData(destination, this.id, null, null, false));

        this.explorePaths(deliveries);
    }

    /**
     * Explores different paths towards multiple destinations to be followed sequentially.
     */
    private void explorePaths(List<DeliveryTaskData> deliveries) {
        List<Point> destinations = new LinkedList<>();

        for (DeliveryTaskData data: deliveries) {
            destinations.add(data.position);
        }

        // Generate a couple of possible paths towards the destination(s) and send exploration messages to explore them.
        List<List<Point>> paths = this.getAlternativePaths(this.alternativePathsToExplore, this.getPosition().get(), destinations);

        this.sendExplorationAnts(paths, deliveries);
    }

    /**
     * Sends out IntentionAnt to perform the deliveries explored by an exploration ant.
     */
    private void sendIntentionAnt(ExplorationAnt explorationAnt) {

        IntentionAnt ant = new IntentionAnt(explorationAnt.path, 0, false, this.id,this,
                explorationAnt.deliveries);
        this.waitingForIntentionAnt++;

        this.commDevice.broadcast(ant);
    }

    /**
     * Sends out IntentionAnt to reserve the destination for a path.
     */
    private void sendIntentionAnt(Queue<Point> path) {
        IntentionAnt ant = new IntentionAnt(new LinkedList<>(path), 0, false,
                this.id, this, null);

        this.commDevice.broadcast(ant);
    }

    /**
     * Sends out ExplorationAnts to explore each of the given paths.
     */
    private void sendExplorationAnts(List<List<Point>> paths, List<DeliveryTaskData> pairs) {
        // Send exploration messages over the found paths
        for (List<Point> path : paths) {
            this.commDevice.broadcast(
                    new ExplorationAnt(path, 0, false, this.id, this, pairs)
            );
            this.waitingForExplorationAnts++;
        }
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
        System.out.println("Nodes: " + this.graphRoadModel.getGraph().getNodes().size() +
                " Connections: " + this.graphRoadModel.getGraph().getConnections().size());
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

    private static List<Map.Entry<DesireAnt, Long>> sortMapDescending(HashMap map) {
        // From: https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/

        List<Map.Entry<DesireAnt, Long>> list = new LinkedList(map.entrySet());

        // Defined Custom Comparator here
        Collections.sort(list, new Comparator<Map.Entry<DesireAnt, Long>>() {
            public int compare(Map.Entry o1, Map.Entry o2) {
                return ((Comparable) o1.getValue())
                        .compareTo(o2.getValue());
            }
        });

        return Lists.reverse(list);
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