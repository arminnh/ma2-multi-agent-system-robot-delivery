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
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import mas.messages.IntentionData;
import mas.SimulatorSettings;
import mas.buildings.ChargingStation;
import mas.graphs.AStar;
import mas.messages.Ant;
import mas.messages.DesireAnt;
import mas.messages.ExplorationAnt;
import mas.messages.IntentionAnt;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.tasks.DeliveryTask;
import mas.tasks.PizzaParcel;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.measure.unit.SI;
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
    private ListenableGraph<LengthData> staticGraph;

    private int id = -1;
    private Battery battery;
    private int currentAmountOfPizzas = 0;
    private double metersMoved = 0;
    private Point pizzeriaPosition;
    private boolean goingToCharge = false;
    private boolean isCharging = false;
    private boolean isAtPizzeria = true;
    private int waitingForExplorationAnts = 0;
    private long intendedArrivalTime = Long.MAX_VALUE;
    private Optional<Queue<Point>> intention = Optional.absent();
    private Queue<PizzaParcel> currentParcels = new LinkedList<>();
    private Optional<ChargingStation> currentChargingStation = Optional.absent();
    private List<ExplorationAnt> explorationAnts = new LinkedList<>();
    private int waitingForDesireAnts = 0;
    private HashMap<DesireAnt, Long> desires = new HashMap<>();
    private boolean goingToPizzeria = false;
    private int waitingForIntentionAnt = 0;
    private Optional<Long> nextIntentionAntsUpdate = Optional.absent();
    private Optional<Long> nextExplorationAntsUpdate = Optional.absent();
    private Point chargingStationPosition;
    private boolean isAtNode;

    public RobotAgent(
            int id,
            VehicleDTO vdto,
            Battery battery,
            ListenableGraph<LengthData> staticGraph,
            Point pizzeriaPosition,
            int alternativePathsToExplore,
            Point chargingStationPosition
    ) {
        super(vdto);

        this.id = id;
        this.battery = battery;
        this.staticGraph = staticGraph;
        this.pizzeriaPosition = pizzeriaPosition;
        this.chargingStationPosition = chargingStationPosition;
        this.alternativePathsToExplore = alternativePathsToExplore;
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
        System.out.println("RobotAgent.getHighestDesires: " + this.desires.size());

        List<DesireAnt> bestTasks = new LinkedList<>();
        int remainingCapacity = this.getCapacityLeft();
        System.out.println("remainingCapacity = " + remainingCapacity);
        // Get all tasks with highest score sorted in descending
        for (Map.Entry<DesireAnt, Long> entry : sortMapDescending(this.desires)) {
            System.out.println("entry.getKey().pizzas = " + entry.getKey().pizzas);
            int capacity = Math.min(remainingCapacity, entry.getKey().pizzas);
            System.out.println("capacity = " + capacity);
            if (capacity > 0) {
                bestTasks.add(entry.getKey());
                remainingCapacity -= capacity;
                System.out.println("remainingCapacity = " + remainingCapacity);
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

        if (!this.desires.isEmpty() && this.waitingForDesireAnts == 0) {
            evaluateDesiresAndStartExploration();
        }

        // If waiting for ExplorationAnts and they all have returned, evaluate the different paths
        if (!this.explorationAnts.isEmpty() && this.waitingForExplorationAnts == 0) {
            this.evaluateExploredPathsAndReviseIntentions();
        }

        this.doAction(time);

        this.resendAnts(time);
    }

    private boolean waitingForAnts() {
        return this.waitingForExplorationAnts > 0 || this.waitingForIntentionAnt > 0 || this.waitingForDesireAnts > 0;
    }

    private void resendAnts(TimeLapse time) {
        // If intention is present, send out intention ants for refresh.
        // Also, can only send ants when there is some battery left.
        if (this.intention.isPresent() && this.getCapacityLeft() > 0) {

            if (this.nextIntentionAntsUpdate.isPresent() && this.nextIntentionAntsUpdate.get() < time.getEndTime()
                    && this.waitingForIntentionAnt == 0 && !this.goingToPizzeria) {
                System.out.println("resendAnts: Sending out intention ants");
                this.nextIntentionAntsUpdate = Optional.absent();

                if (this.currentParcels.size() > 0) {
                    // Robot is making a delivery
                    this.sendIntentionAntForCurrentParcels(this.intention.get());
                } else if (this.goingToCharge) {
                    this.sendIntentionAntToChargingStation(this.intention.get());
                }
            }

            if (this.nextExplorationAntsUpdate.isPresent() && this.nextExplorationAntsUpdate.get() < time.getEndTime()
                    && this.waitingForExplorationAnts == 0) {
                System.out.println("resendAnts: Sending out exploration ants");
                this.nextIntentionAntsUpdate = Optional.absent();

                if (this.goingToPizzeria) {
                    System.out.println("RobotAgent.resendAnts.this.goingToPizzeria");
                    explorePaths(this.pizzeriaPosition);

                } else if (this.goingToCharge) {
                    System.out.println("RobotAgent.resendAnts.this.goingToCharge");
                    explorePaths(this.chargingStationPosition);

                } else {
                    System.out.println("RobotAgent.resendAnts.else");
                    checkPathsForCurrentParcels();
                }
            }
        }
    }

    private void doAction(@NotNull TimeLapse time) {
        // TODO: something for intention messages here ??
        if (!this.currentParcels.isEmpty() && !this.intention.isPresent()) {
            // We do have parcels but we don't have an intention!
            // This is because one of our reservations got dropped or the robot just charged!
            // 1. Recalculate the best path for our current parcels
            // 2. Resend Intention ants
            checkPathsForCurrentParcels();
        } else if (this.intention.isPresent() && !waitingForAnts()) {
            followIntention(time);
        } else {
            // No intention is present and no desire. First choose a new desire.
            // Only choose a new intention if not waiting for exploration ants.
            if (this.waitingForExplorationAnts == 0 && this.waitingForIntentionAnt == 0 && waitingForDesireAnts == 0) {
                // If at pizzeria ask for tasks
                if (this.isAtPizzeria) {
                    List<DeliveryTask> tasks = pizzeriaModel.getDeliveryTasks();
                    if (!tasks.isEmpty()) {
                        sendDesireAntsForTasks(tasks);
                        return;
                    }
                }

                // If a new parcel has been set on the robot, explore paths towards the destination of the parcel.
                // Search new intentions
                if (!this.currentParcels.isEmpty()) {
                    checkPathsForCurrentParcels();
                } else {
                    // Otherwise, either go to a charging station to charge or a pizzeria to pick up a new parcel.
                    toChargingStationOrPizzeria();
                }
            }
        }
    }

    private void checkPathsForCurrentParcels() {
        System.out.println("RobotAgent.checkPathsForCurrentParcels");
        List<IntentionData> deliveries = new LinkedList<>();

        for (PizzaParcel parcel : this.currentParcels) {
            deliveries.add(new IntentionData(parcel.getDeliveryLocation(), this.id,
                    parcel.deliveryTaskID, parcel.amountOfPizzas, false));
        }

        this.explorePaths(deliveries);
    }

    private void toChargingStationOrPizzeria() {
        System.out.println("RobotAgent.toChargingStationOrPizzeria");
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

    private void evaluateDesiresAndStartExploration() {
        System.out.println("RobotAgent.evaluateDesiresAndStartExploration: " + this.desires);
        // Decide on desire
        List<DesireAnt> bestTasks = getHighestDesires();
        List<IntentionData> intentionData = new LinkedList<>();
        System.out.println("BestTasks " + bestTasks);
        System.out.println("Remaining ants " + this.waitingForDesireAnts);
        System.out.println("Amount of desires: "+ this.desires.size());
        System.out.println("Best tasks: " + bestTasks.size());
        int remainingCapacity = this.getCapacityLeft();
        for (DesireAnt ant : bestTasks) {
            Point destination = ant.path.get(ant.path.size() - 1);

            int pizzas = Math.min(remainingCapacity, ant.pizzas);
            remainingCapacity -= pizzas;

            intentionData.add(new IntentionData(destination, this.id, ant.deliveryTaskID, pizzas, false));
        }

        explorePaths(intentionData);
        /*if (intentionData.size() > 1) {
            explorePaths(Lists.reverse(intentionData));
        }*/
        this.desires.clear();
    }

    private void followIntention(@NotNull TimeLapse time) {
        System.out.println("RobotAgent.followIntention, this.intention.get() = " + this.intention.get());
        // Make the robot follow its intention. If the robot arrives at the next position it its
        // intention, that position will be removed from the Queue.
        this.move(time);

        //intentionReconsideration();

        PizzaParcel parcel = this.currentParcels.peek();
        if (!this.currentParcels.isEmpty() && parcel.getDeliveryLocation().equals(this.getPosition().get())) {
            this.deliverPizzaParcel(parcel, time);
        }

        // If robot arrived at the destination of the intention, the intention will be empty
        if (this.intention.get().isEmpty()) {
            System.out.println("intention is empty");

            this.intention = Optional.absent();
            this.intendedArrivalTime = 0;
            // If the robot was delivering a PizzaParcel
            if (this.goingToCharge) {
                System.out.println("Arriving at charging");
                this.arriveAtChargingStation();
                // If the robot was going towards a pizzeria
            } else if (this.goingToPizzeria) {
                this.arriveAtPizzeria(time);
            }
        }
    }

    private void sendDesireAntsForTasks(List<DeliveryTask> tasks) {
        System.out.println("sendDesireAntsForTasks");
        for (DeliveryTask task : tasks) {

            List<Point> destinations = new LinkedList<>(Arrays.asList(task.getPosition().get()));
            List<Point> path = this.getAlternativePaths(1, this.getPosition().get(), destinations).get(0);

            DesireAnt desireAnt = new DesireAnt(path, 0, false, this.id, this, 0,null, task.id, 0);

            this.commDevice.broadcast(desireAnt);
            this.waitingForDesireAnts++;
        }
        System.out.println("Sent out " + this.waitingForDesireAnts);
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    /**
     * Read all unread messages and take appropriate actions for each message.
     */
    private void readMessages(TimeLapse time) {
        for (Message m : this.commDevice.getUnreadMessages()) {

            if (Ant.class.isInstance(m.getContents())) {
                Ant ant = (Ant) m.getContents();
                if (ant.robotID != this.id) {
                    continue;
                }
            }

            // If an exploration ant has returned, store the explored path and its estimated cost.
            if (m.getContents().getClass() == ExplorationAnt.class) {
                handleExplorationAntMessage(time, m);
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

    private void handleExplorationAntMessage(TimeLapse time, Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();
        System.out.println("Robot " + this.id + " got ExplorationAnt for robotID " + ant.robotID);

        this.explorationAnts.add(ant);
        this.waitingForExplorationAnts--;
        if (this.waitingForIntentionAnt == 0) {
            this.nextExplorationAntsUpdate = Optional.of(time.getStartTime() + SimulatorSettings.REFRESH_EXPLORATIONS);
        }
    }

    private void handleDesireAntMessage(Message m) {
        DesireAnt ant = (DesireAnt) m.getContents();
        System.out.println("Got desire ant: " + ant);
        if (ant.pizzas > 0) {
            this.desires.put(ant, ant.score);
        }

        this.waitingForDesireAnts--;
    }

    private void handleIntentionAntMessage(TimeLapse time, Message m) {
        IntentionAnt ant = (IntentionAnt) m.getContents();

        if (ant.toChargingStation) {
            this.intention = Optional.of(new LinkedList<>(ant.path));
            System.out.println("CHARGING, this.hasPizzaParcel() = " + this.hasPizzaParcel() + ", this.intention = " + this.intention);
            this.nextIntentionAntsUpdate = Optional.of(time.getStartTime() + SimulatorSettings.REFRESH_INTENTIONS);

        } else {
            for (IntentionData intentionData : ant.deliveries) {

                // Check if we don't have a parcel yet and if the reservation is confirmed
                if (intentionData.reservationConfirmed && this.getParcelForDeliveryTaskID(intentionData) == null) {
                    PizzaParcel parcel = this.pizzeriaModel.newPizzaParcel(intentionData.deliveryTaskID,
                            this.getPosition().get(), intentionData.pizzas, time.getStartTime());

                    this.pdpModel.pickup(this, parcel, time);

                    this.addPizzaParcel(parcel);
                } else {
                    System.out.println("Reservation denied");
                    if (this.intention.isPresent()) {
                        // There was a reservation denied
                        // Get the correct parcel from our current parcels
                        PizzaParcel removeParcel = getParcelForDeliveryTaskID(intentionData);
                        System.out.println("removeParcel = " + removeParcel + " goingToChargeStation " + this.goingToCharge);
                        if (removeParcel != null) {
                            System.out.println("Removing parcel!!");
                            this.intention = Optional.absent();

                            System.out.println("intention = " + this.intention);
                            this.dropParcel(removeParcel, time);

                        } else if (goingToCharge) {
                            this.intention = Optional.absent();
                            this.goingToCharge = false;

                        }
                    }
                }
            }

            if (this.hasPizzaParcel()) {
                System.out.println("Got parcel, setting intention " + ant.path);
                this.intention = Optional.of(new LinkedList<>(ant.path));
                this.nextIntentionAntsUpdate = Optional.of(time.getStartTime() + SimulatorSettings.REFRESH_INTENTIONS);
            } else {
                System.out.println("RobotAgent.handleIntentionAntMessage: ????????????????????????????????");
            }
        }
        this.waitingForIntentionAnt--;
    }

    @Nullable
    private PizzaParcel getParcelForDeliveryTaskID(IntentionData intentionData) {
        System.out.println("RobotAgent.getParcelForDeliveryTaskID");
        PizzaParcel removeParcel = null;

        for (PizzaParcel p : this.currentParcels) {
            System.out.println("p.deliveryTaskID: " + p.deliveryTaskID + " intentionData.deliveryTaskID: " + intentionData.deliveryTaskID);
            if (p.deliveryTaskID == intentionData.deliveryTaskID) {

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
        System.out.println("RobotAgent.evaluateExploredPathsAndReviseIntentions, this.intention.isPresent(): " + this.intention.isPresent());
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
        if (!this.intention.isPresent() || bestTime < this.intendedArrivalTime) {

            if (this.goingToPizzeria) {
                this.intention = Optional.of(new LinkedList<>(bestAnt.path));
            } else {
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

            if (!this.getPosition().get().equals(this.intention.get().peek())) {
                progress = this.roadModel.moveTo(this, this.intention.get().peek(), time);
            } else {
                progress = this.roadModel.moveTo(this, this.intention.get().remove(), time);
            }

            this.metersMoved += progress.distance().doubleValue(SI.METER);
            if (this.metersMoved > 1.0) {
                this.battery.decrementCapacity();
                this.metersMoved -= 1.0;
            }
        }
    }

    /**
     * Delivers a PizzaParcel for a certain DeliveryTask. Delivers the parcel in the PDPModel, decreases the amount of
     * pizzas for the DeliveryTask in the PizzeriaModel, and decreases the amount of pizzas held by the robot.
     */
    private void deliverPizzaParcel(PizzaParcel parcel, @NotNull TimeLapse time) {
        System.out.println("RobotAgent.deliverPizzaParcel");
        DeliveryTask deliveryTask = parcel.deliveryTask;

        // If the robot is at the delivery position of the delivery task, deliver the pizza parcel
        if (this.roadModel.equalPosition(this, deliveryTask)) {
            this.removePizzaParcelFromRobot(parcel);

            this.pdpModel.deliver(this, parcel, time);
            this.pizzeriaModel.deliverPizzaParcel(this, parcel, time.getEndTime());
        } else {
            System.err.println("TRYING TO DELIVER FROM WRONG POSITION");
        }
    }

    /**
     * Makes the robot drop the parcel.
     */
    private void dropParcel(PizzaParcel parcel, TimeLapse time) {
        System.out.println("RobotAgent.dropParcel");
        this.removePizzaParcelFromRobot(parcel);

        this.pdpModel.drop(this, parcel, time);
        this.pizzeriaModel.dropPizzaParcel(this, parcel, time.getStartTime());
    }

    /**
     * Removes a PizzaParcel from the robot by decreasing the amount of pizzas that are currently carried and removing
     * the parcel from the list of parcels.
     */
    private void removePizzaParcelFromRobot(PizzaParcel parcel) {
        // Unload pizzas
        this.currentAmountOfPizzas -= parcel.amountOfPizzas;

        // Remove the parcel
        this.currentParcels.remove(parcel);
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
        if (this.roadModel.getPosition(this).equals(this.pizzeriaPosition)) {
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
        return new Double(this.getCapacity()).intValue() - this.currentAmountOfPizzas;
    }

    public boolean hasPizzaParcel() {
        return this.currentParcels.size() > 0;
    }

    /**
     * Sets a new PizzaParcel to be delivered by the robot.
     */
    public void addPizzaParcel(PizzaParcel parcel) {
        this.currentParcels.add(parcel);
        this.currentAmountOfPizzas += parcel.amountOfPizzas;
        this.isAtPizzeria = false;
        System.out.println("Current parcels: " + this.currentParcels.size());
    }

    /**
     * Explores different paths towards a single destination.
     */
    private void explorePaths(Point destination) {
        System.out.println("RobotAgent.explorePaths22222");
        List<IntentionData> deliveries = new LinkedList<>();
        deliveries.add(new IntentionData(destination, this.id, null, null, false));

        this.explorePaths(deliveries);
    }

    /**
     * Explores different paths towards multiple destinations to be followed sequentially.
     */
    private void explorePaths(List<IntentionData> intentionData) {
        System.out.println("RobotAgent.explorePaths, intentionData: " + intentionData);
        List<Point> destinations = new LinkedList<>();

        for (IntentionData data : intentionData) {

            destinations.add(data.position);
        }

        // Generate a couple of possible paths towards the destination(s) and send exploration messages to explore them.
        List<List<Point>> paths = this.getAlternativePaths(this.alternativePathsToExplore, this.getPosition().get(), destinations);

        this.sendExplorationAnts(paths, intentionData);
    }

    /**
     * Sends out IntentionAnt to perform the deliveries explored by an exploration ant.
     */
    private void sendIntentionAnt(ExplorationAnt explorationAnt) {

        IntentionAnt ant = new IntentionAnt(explorationAnt.path, 0, false, this.id, this,
                0,explorationAnt.deliveries);
        this.waitingForIntentionAnt++;

        this.commDevice.broadcast(ant);
    }

    /**
     * Sends out IntentionAnt to reserve the destination for a path.
     */
    private void sendIntentionAntToChargingStation(Queue<Point> path) {
        List<IntentionData> deliveries = new LinkedList<>();
        deliveries.add(new IntentionData(this.chargingStationPosition, this.id,
                0, 0, false));

        IntentionAnt ant = new IntentionAnt(new LinkedList<>(path), 0, false,
                this.id, this,0, deliveries);

        this.commDevice.broadcast(ant);
        this.waitingForIntentionAnt++;
    }

    private void sendIntentionAntForCurrentParcels(Queue<Point> path) {
        System.out.println("RobotAgent.sendIntentionAntForCurrentParcels, path:" + path);
        List<IntentionData> deliveries = new LinkedList<>();

        for (PizzaParcel parcel : this.currentParcels) {
            deliveries.add(new IntentionData(parcel.getDeliveryLocation(), this.id,
                    parcel.deliveryTaskID, parcel.amountOfPizzas, false));
        }

        IntentionAnt ant = new IntentionAnt(new LinkedList<>(path), 0, false,
                this.id, this, 0, deliveries);
        this.commDevice.broadcast(ant);
        this.waitingForIntentionAnt++;
        System.out.println("[OUT] RobotAgent.sendIntentionAntForCurrentParcels");

    }

    /**
     * Sends out ExplorationAnts to explore each of the given paths.
     */
    private void sendExplorationAnts(List<List<Point>> paths, List<IntentionData> intentionData) {
        System.out.println("RobotAgent.sendExplorationAnts, paths: " + paths + ", intentionData: " + intentionData);
        // Send exploration messages over the found paths
        for (List<Point> path : paths) {
            this.commDevice.broadcast(
                    new ExplorationAnt(path, 0, false, this.id, this, 0, intentionData)
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
        // System.out.println("Nodes: " + this.staticGraph.getNodes().size() + " Connections: " + this.staticGraph.getConnections().size());
        List<List<Point>> alternativePaths = new LinkedList<>();

        int fails = 0;
        int maxFails = 3;
        double penalty = 2.0;
        double alpha = rng.nextDouble();
        Table<Point, Point, Double> weights = HashBasedTable.create();

        while (alternativePaths.size() < numberOfPaths && fails < maxFails) {
            List<Point> path = AStar.getShortestPath(this.staticGraph, weights, start, new LinkedList<>(dest));

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
                        for (Point v2 : this.staticGraph.getIncomingConnections(v)) {
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
                fails++;
                System.err.println("GOT null PATH OUT OF A* SEARCH");
            }
        }

        return alternativePaths;
    }

    private List<Map.Entry<DesireAnt, Long>> sortMapDescending(HashMap<DesireAnt, Long> map) {
        // From: https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/

        LinkedList<Map.Entry<DesireAnt, Long>> list = new LinkedList<>(map.entrySet());

        // Defined Custom Comparator here
        list.sort(Comparator.comparing(Map.Entry::getValue));

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