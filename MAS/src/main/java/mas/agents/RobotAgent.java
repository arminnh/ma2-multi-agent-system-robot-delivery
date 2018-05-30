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
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
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
import mas.SimulatorSettings;
import mas.buildings.ChargingStation;
import mas.graphs.AStar;
import mas.messages.*;
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
    private ListenableGraph<LengthData> dynamicGraph;

    public final int id;
    private Battery battery;
    private boolean isOnNode = true;
    private Point pizzeriaPosition;
    private boolean goingToPizzeria = false;
    private boolean isAtPizzeria = true;
    private Point chargingStationPosition;
    private boolean goingToCharge = false;
    private boolean isCharging = false;
    private Optional<ChargingStation> currentChargingStation = Optional.absent();

    private int currentAmountOfPizzas = 0;
    private Queue<PizzaParcel> currentParcels = new LinkedList<>();
    private long intendedArrivalTime = Long.MAX_VALUE;
    private List<IntentionAnt> intentionAnts = new LinkedList<>();
    private int waitingForIntentionAnts = 0;
    private Optional<Queue<Point>> intention = Optional.absent();
    private Optional<Long> nextIntentionAntsUpdate = Optional.absent();
    private HashMap<DesireAnt, Long> desireAnts = new HashMap<>();
    private int waitingForDesireAnts = 0;
    private List<ExplorationAnt> explorationAnts = new LinkedList<>();
    private int waitingForExplorationAnts = 0;
    private Optional<Long> nextExplorationAntsUpdate = Optional.absent();

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
        this.dynamicGraph = (ListenableGraph<LengthData>) ((DynamicGraphRoadModelImpl) pRoadModel).getGraph();
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

    private boolean waitingForAnts() {
        return this.waitingForExplorationAnts > 0 || this.waitingForIntentionAnts > 0 || this.waitingForDesireAnts > 0;
    }

    private boolean isAtCurrentPizzaParcelDeliveryLocation() {
        if (this.currentParcels.isEmpty()) {
            return false;
        }
        return this.roadModel.equalPosition(this, this.currentParcels.peek().deliveryTask);
    }

    /**
     * True if a connection towards the next move in the intention exists in the dynamic graph. Also true if the robot
     * is not currently on a node, because then is is on a connection already.
     */
    private boolean existsConnectionForNextMove() {
        if (!this.isOnNode || this.getPosition().get().equals(this.intention.get().peek())) {
            return true;
        }
        if (this.intention.isPresent()) {
            return this.dynamicGraph.hasConnection(this.getPosition().get(), this.intention.get().peek());
        }
        return false;
    }

    /**
     * True if a connection towards the next move in the intention exists in the dynamic graph. Also true if the robot
     * is not currently on a node, because then is is on a connection already.
     */
    private boolean existsConnectionForNextMove(Point nextPosition) {
        if (!this.isOnNode || this.getPosition().get().equals(nextPosition)) {
            return true;
        }
        if (this.intention.isPresent()) {
            return this.dynamicGraph.hasConnection(this.getPosition().get(), nextPosition);
        }
        return false;
    }

    /**
     * Broadcasts an ant to CommUsers in the robots range. Throws an exception if the robot is not on a node, as
     * otherwise the ant could be sent to the ResourceAgent the robot was just on and then the path given to the ant
     * would not be correct.
     */
    private void broadcastAnt(Ant ant) {
        if (!this.isOnNode) {
            throw new IllegalStateException("Can only broadcast ants if standing on a node.");
        }
        this.commDevice.broadcast(ant);
    }

    /**
     * The main logic of the robot. Handles messages, (re)evaluates paths and intentions, and makes the robot perform
     * actions in the virtual environment.
     */
    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft() || this.getRemainingBatteryCapacityPercentage() == 0.0) {
            return;
        }

        // Read all new messages
        this.readMessages();

        // Process different types of ants if enough of them have returned
        this.processAnts(time);

        // Check if the current intentions should be replaced.
        this.reconsiderIntentions();

        System.out.println("Intention: " + this.intention);
        System.out.println("isCharging: " + this.isCharging + ", goingToCharge: " + this.goingToCharge);
        System.out.println("isAtPizzeria: " + this.isAtPizzeria + ", goingToPizzeria: " + this.goingToPizzeria);
        System.out.println("waiting: " + this.waitingForAnts() + ", intention: " + this.waitingForIntentionAnts);

        // Make the robot do its next action
        if (this.intention.isPresent() && !this.waitingForAnts() && !this.isCharging) {
            this.doAction(time);
        }

        // Resend intention or exploration ants if necessary.
        this.resendAnts(time);
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    /**
     * Read all unread messages and take appropriate actions for each message.
     */
    private void readMessages() {
        for (Message m : this.commDevice.getUnreadMessages()) {

            if (Ant.class.isInstance(m.getContents())) {
                Ant ant = (Ant) m.getContents();
                if (ant.robotID != this.id) {
                    continue;
                }
            }

            if (m.getContents().getClass() == DesireAnt.class) {
                this.handleDesireAntMessage(m);

            } else if (m.getContents().getClass() == ExplorationAnt.class) {
                this.handleExplorationAntMessage(m);

            } else if (m.getContents().getClass() == IntentionAnt.class) {
                this.handleIntentionAntMessage(m);
            }
        }
    }

    /**
     * Processes lists of ants that have returned and checks which ants to take next.
     */
    private void processAnts(TimeLapse time) {
        // If waiting for DesireAnts, and they all have returned, process the different desireAnts.
        if (!this.desireAnts.isEmpty() && this.waitingForDesireAnts == 0) {
            this.processDesiresAndStartExploration();
        }

        // If waiting for ExplorationAnts, and they all have returned, process the different paths.
        if (!this.explorationAnts.isEmpty() && this.waitingForExplorationAnts == 0) {
            this.processExploredPathsAndReviseIntentions();
        }

        // If waiting for IntentionAnts, and they all have returned, process the results.
        if (!this.intentionAnts.isEmpty() && this.waitingForIntentionAnts == 0) {
            this.processIntentionAnts(time);
        }
    }

    /**
     * Processes the DesireAnts for tasks that have returned and selects the order to explore the tasks.
     */
    private void processDesiresAndStartExploration() {
        System.out.println("RobotAgent.processDesiresAndStartExploration: " + this.desireAnts);
        List<DesireAnt> bestTasks = this.getHighestDesires();
        List<IntentionData> intentionData = new LinkedList<>();

        System.out.println("BestTasks (" + bestTasks.size() + "):" + bestTasks);

        int remainingCapacity = this.getRemainingCapacity();
        for (DesireAnt ant : bestTasks) {
            Point destination = ant.path.get(ant.path.size() - 1);

            int pizzas = Math.min(remainingCapacity, ant.pizzas);
            remainingCapacity -= pizzas;

            intentionData.add(new IntentionData(destination, this.id, ant.deliveryTaskID, pizzas, false));
        }

        this.explorePaths(intentionData);
        this.desireAnts.clear();
    }

    /**
     * Finds the best path among the ones that were explored by ExplorationAnts. If the best path is better than the
     * one the robot is currently following, the intention will be updated and new IntentionAnts will be sent.
     */
    private void processExploredPathsAndReviseIntentions() {
        System.out.println("RobotAgent.processExploredPathsAndReviseIntentions, this.intention.isPresent(): " + this.intention.isPresent());
        Long bestTime = Long.MAX_VALUE;
        ExplorationAnt bestAnt = null;

        for (ExplorationAnt ant : this.explorationAnts) {
            if (ant.estimatedTime < bestTime) {
                bestTime = ant.estimatedTime;
                bestAnt = ant;
            }
            if (ant.path.get(ant.path.size() - 1).equals(this.chargingStationPosition) && this.goingToCharge) {
                bestTime = ant.estimatedTime;
                bestAnt = ant;
                // We break here because charging takes precedence over everything else.
                break;
            }
        }

        this.explorationAnts.clear();

        // If the new best path is better than the current one, update the intention of the robot.
        if (!this.intention.isPresent() || bestTime < this.intendedArrivalTime || this.goingToCharge) {
            this.intendedArrivalTime = bestTime;

            if (this.goingToPizzeria) {
                this.intention = Optional.of(new LinkedList<>(bestAnt.path));
            } else {
                this.sendIntentionAnt(bestAnt);
            }
        }
    }

    /**
     * Processes the IntentionAnts that have returned. Currently, only one IntentionAnt at a time is supported.
     */
    private void processIntentionAnts(TimeLapse time) {
        System.out.println("RobotAgent.processIntentionAnts: " + this.id + ", " + this.intentionAnts);

        IntentionAnt ant = this.intentionAnts.remove(0);

        if (ant.toChargingStation) {
            System.out.println("SET TO GO CHARGE, this.hasPizzaParcel() = " + this.hasPizzaParcel() + ", this.intention = " + this.intention);
            if(ant.intentions.get(0).reservationConfirmed){
                this.intention = Optional.of(new LinkedList<>(ant.path));

            }

        } else {
            for (IntentionData intentionData : ant.intentions) {
                // If a reservation has been confirmed, create a new parcel if necessary
                if (intentionData.reservationConfirmed) {
                    if (this.getParcelForDeliveryTaskID(intentionData.deliveryTaskID) == null) {
                        PizzaParcel parcel = this.pizzeriaModel.newPizzaParcel(intentionData.deliveryTaskID,
                                this.getPosition().get(), intentionData.pizzas, time.getStartTime());

                        this.pdpModel.pickup(this, parcel, time);

                        this.addPizzaParcel(parcel);
                    }
                } else {
                    // A reservation was denied or has expired, if the robot is carrying the parcel for the reservation,
                    // make it drop the parcel.
                    PizzaParcel dropParcel = this.getParcelForDeliveryTaskID(intentionData.deliveryTaskID);

                    if (dropParcel != null) {
                        System.out.println("Dropping parcel!!" + dropParcel);
                        this.intention = Optional.absent();
                        this.dropParcel(dropParcel, time);

                    }
                }
            }

            if (this.hasPizzaParcel()) {
                System.out.println("Has PizzaParcel, setting intention to " + ant.path);
                this.intention = Optional.of(new LinkedList<>(ant.path));
            }
        }
    }

    /**
     * Updates the robot's intention if another intention should be chosen.
     */
    private void reconsiderIntentions() {
        // If already waiting for ants, new intentions are being looked for.
        if (this.waitingForAnts()) {
            return;
        }

        // If the path the robot is currently trying to take does not exist anymore, remove the current intention.
        if (this.intention.isPresent() && !this.existsConnectionForNextMove()) {
            System.out.println("RESET THE INTENTION BECAUSE THERE WAS NO CONNECTION");
            this.intention = Optional.absent();
        }

        // If no intention is present, should decide on a new one.
        // Only search for new intentions when not waiting for ants.
        if (!this.intention.isPresent() && !this.waitingForAnts()) {
            // If robot was going to charge, explore paths towards charging station
            if (this.goingToCharge) {
                this.explorePaths(this.chargingStationPosition);
                return;
            }

            // If robot is carrying PizzaParcels, explore paths to deliver the parcels.
            if (!this.currentParcels.isEmpty()) {
                // The robot has parcels but no intention. This can happen because reservations expired, the robot
                // has just charged, or sudden roadworks have made the intention impossible. => Recalculate the best
                // path for our current parcels, so that a new intention can be calculated.
                System.out.println("NO INTENTION BUT GOT PARCELS, CHECKING NEW PATHS FOR PARCELS");
                this.explorePathsForCurrentParcels();
                return;
            }

            // If at pizzeria, ask for tasks.
            if (this.isAtPizzeria) {
                List<DeliveryTask> tasks = this.pizzeriaModel.getDeliveryTasks();
                if (!tasks.isEmpty()) {
                    System.out.println("SENT OUT DESIRE ANTS TO EXPLORE TASKS");
                    this.sendDesireAntsForTasks(tasks);
                    return;
                }
            }

            // Go to a pizzeria to get new tasks. Only explore new paths towards pizzeria if not already
            // at pizzeria and not charging.
            if (!this.isAtPizzeria && !this.isCharging && !this.goingToCharge) {
                System.out.println("SENT OUT EXPLORATION ANTS TO GO TO PIZZERIA");
                this.goingToPizzeria = true;
                this.explorePaths(this.pizzeriaPosition);
                return;
            }

        } else {
            // Else, check if there is a better or more important intention.
            // Remove the current intention if another one should be set up.

            // Check if need to charge.
            if (this.getRemainingBatteryCapacityPercentage() <= 0.35 && !this.goingToCharge && !this.isCharging && this.isOnNode) {
                System.out.println("SET ROBOT TO GO TO CHARGE");
                this.intention = Optional.absent();
                this.goingToCharge = true;
                this.goingToPizzeria = false;
                this.explorePaths(this.chargingStationPosition);
                return;
            }
        }
    }

    /**
     * Makes the robot move and then optionally deliver a package or to a charging station or pizzeria.
     */
    private void doAction(@NotNull TimeLapse time) {
        // If an intention is present, follow it.
        System.out.println("RobotAgent.doAction, position = " + getPosition().get() + ", this.intention.get() = " + this.intention.get());

        // Make the robot follow its intention.
        this.move(time);

        // Deliver a PizzaParcel if applicable
        if (this.isAtCurrentPizzaParcelDeliveryLocation()) {
            this.deliverCurrentPizzaParcel(time);
        }

        // If robot arrived at the destination of the intention, the intention will be empty
        if (this.intention.get().isEmpty()) {
            this.intention = Optional.absent();
            this.intendedArrivalTime = 0;

            if (this.goingToCharge) {
                this.arriveAtChargingStation();
            } else if (this.goingToPizzeria) {
                this.arriveAtPizzeria(time);
            }
        }
    }

    /**
     * While intention is present, regularly send out ants to refresh reservations or find better paths.
     * Can only send ants when there is some battery left and the robot is on a node.
     */
    private void resendAnts(TimeLapse time) {
        if (this.intention.isPresent() && this.getRemainingCapacity() > 0 && this.isOnNode) {

            if (this.waitingForIntentionAnts == 0) {
                if (!this.nextIntentionAntsUpdate.isPresent()) {
                    this.nextIntentionAntsUpdate =
                            Optional.of(time.getEndTime() + SimulatorSettings.REFRESH_INTENTIONS);

                } else if (this.nextIntentionAntsUpdate.get() < time.getEndTime() && !this.goingToPizzeria) {
                    this.nextIntentionAntsUpdate = Optional.absent();

                    this.resendIntentionAnts();
                }
            }

            if (this.waitingForExplorationAnts == 0) {
                if (!this.nextExplorationAntsUpdate.isPresent()) {
                    this.nextExplorationAntsUpdate =
                            Optional.of(time.getStartTime() + SimulatorSettings.REFRESH_EXPLORATIONS);

                } else if (this.nextExplorationAntsUpdate.get() < time.getEndTime()) {
                    this.nextExplorationAntsUpdate = Optional.absent();

                    if (this.goingToPizzeria) {
                        System.out.println("RobotAgent.resendAnts goingToPizzeria");
                        this.explorePaths(this.pizzeriaPosition);

                    } else if (this.goingToCharge) {
                        System.out.println("RobotAgent.resendAnts goingToCharge");
                        this.explorePaths(this.chargingStationPosition);

                    } else {
                        System.out.println("RobotAgent.resendAnts for current parcels");
                        this.explorePathsForCurrentParcels();
                    }
                }
            }
        }
    }

    /**
     * Resends intention ants to refresh reservations.
     */
    private void resendIntentionAnts() {
        System.out.println("RobotAgent.resendIntentionAnts, " + this.id);
        LinkedList<Point> pathWithCurrentPos = new LinkedList<>(this.intention.get());
        if (!pathWithCurrentPos.get(0).equals(this.getPosition().get())) {
            pathWithCurrentPos.add(0, this.getPosition().get());
        }
        List<IntentionData> intentions = new LinkedList<>();

        // If robot is carrying parcels, set up intentions for delivery tasks
        if (this.currentParcels.size() > 0 && !this.goingToCharge) {
            for (PizzaParcel parcel : this.currentParcels) {
                intentions.add(new IntentionData(parcel.getDeliveryLocation(), this.id,
                        parcel.deliveryTaskID, parcel.amountOfPizzas, false));
            }

        } else if (this.goingToCharge) {
            intentions.add(new IntentionData(this.chargingStationPosition, this.id,
                    null, null, false));
        }

        IntentionAnt ant = new IntentionAnt(pathWithCurrentPos, 0, false, this.id,
                this, 0, intentions);

        this.broadcastAnt(ant);
        this.waitingForIntentionAnts++;
    }

    private void sendDesireAntsForTasks(List<DeliveryTask> tasks) {

        System.out.println("sendDesireAntsForTasks");
        for (DeliveryTask task : tasks) {

            List<Point> destinations = new LinkedList<>(Collections.singletonList(task.getPosition().get()));
            List<Point> path = this.getAlternativePaths(1, this.getPosition().get(), destinations).get(0);

            DesireAnt desireAnt = new DesireAnt(path, 0, false, this.id,
                    this, 0, null, task.id, 0);

            this.broadcastAnt(desireAnt);
            this.waitingForDesireAnts++;
        }

        System.out.println("Sent out " + this.waitingForDesireAnts);
    }

    /**
     * Sends out ExplorationAnts to explore each of the given paths.
     */
    private void sendExplorationAnts(List<List<Point>> paths, List<IntentionData> intentionData) {
        System.out.println("RobotAgent.sendExplorationAnts, intentionData: " + intentionData + ", paths: " + paths);
        // Send exploration messages over the found paths
        for (List<Point> path : paths) {
            this.broadcastAnt(
                    new ExplorationAnt(path, 0, false, this.id, this, 0, intentionData)
            );
            this.waitingForExplorationAnts++;
        }
    }

    /**
     * Sends out IntentionAnt to perform the intentions explored by an exploration ant.
     */
    private void sendIntentionAnt(ExplorationAnt explorationAnt) {

        List<Point> shorterPath = shortenExplorationAntPath(explorationAnt.path, explorationAnt.intentions);
        IntentionAnt ant = new IntentionAnt(shorterPath, 0, false, this.id, this,
                0, explorationAnt.intentions);
        this.waitingForIntentionAnts++;

        this.broadcastAnt(ant);
    }

    private List<Point> shortenExplorationAntPath(List<Point> path, List<IntentionData> intentions) {
        int max_index = 0;

        for(IntentionData intention: intentions){
            int currentFirstIndex = path.indexOf(intention.position);
            if(currentFirstIndex > max_index){
                max_index = currentFirstIndex;
            }
        }
        
        // Last index is exclusive
        return path.subList(0, max_index+1);
    }

    /**
     * Explores different paths towards a single destination.
     */
    private void explorePaths(Point destination) {
        System.out.println("RobotAgent.explorePaths1");
        List<IntentionData> intentions = new LinkedList<>();
        intentions.add(new IntentionData(destination, this.id, null, null, false));

        this.explorePaths(intentions);
    }

    /**
     * Explores different paths towards multiple destinations to be followed sequentially.
     */
    private void explorePaths(List<IntentionData> intentionData) {
        System.out.println("RobotAgent.explorePaths2");
        List<Point> destinations = new LinkedList<>();

        for (IntentionData data : intentionData) {
            destinations.add(data.position);
        }

        // Generate a couple of possible paths towards the destination(s) and send exploration messages to explore them.
        List<List<Point>> paths = this.getAlternativePaths(this.alternativePathsToExplore, this.getPosition().get(), destinations);

        this.sendExplorationAnts(paths, intentionData);
    }

    /**
     * Sends explores different paths that visit the parcels the robot is currently carrying.
     */
    private void explorePathsForCurrentParcels() {
        System.out.println("RobotAgent.explorePathsForCurrentParcels");
        List<IntentionData> intentions = new LinkedList<>();

        for (PizzaParcel parcel : this.currentParcels) {
            intentions.add(new IntentionData(parcel.getDeliveryLocation(), this.id,
                    parcel.deliveryTaskID, parcel.amountOfPizzas, false));
        }

        this.explorePaths(intentions);
    }

    private void handleExplorationAntMessage(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();
        System.out.println("Robot " + this.id + " got ant: " + ant);

        this.explorationAnts.add(ant);
        this.waitingForExplorationAnts--;
    }

    private void handleDesireAntMessage(Message m) {
        DesireAnt ant = (DesireAnt) m.getContents();
        System.out.println("Robot " + this.id + " got ant: " + ant);

        System.out.println("Got desire ant: " + ant);
        // Only store the ant if pizzas can be delivered for the task by this robot.
        if (ant.pizzas > 0) {
            this.desireAnts.put(ant, ant.score);
        }
        this.waitingForDesireAnts--;
    }

    private void handleIntentionAntMessage(Message m) {
        IntentionAnt ant = (IntentionAnt) m.getContents();
        System.out.println("Robot " + this.id + " got ant: " + ant);

        this.intentionAnts.add(ant);
        this.waitingForIntentionAnts--;
    }

    @Nullable
    private PizzaParcel getParcelForDeliveryTaskID(int deliveryTaskID) {
        System.out.println("RobotAgent.getParcelForDeliveryTaskID");

        for (PizzaParcel p : this.currentParcels) {
            if (p.deliveryTaskID == deliveryTaskID) {
                return p;
            }
        }

        return null;
    }

    /**
     * Moves the robot along its intention.
     * <p>
     * When the robot reaches the next position in its intention, that position is be removed from the intention.
     */
    private void move(@NotNull TimeLapse time) {
        if (this.intention.isPresent()) {
            Queue<Point> path = this.intention.get();
            Point nextPosition = path.peek();

            this.isOnNode = this.getPosition().get().equals(nextPosition);
            if (this.isOnNode) {
                // Remove the next position in the intention when that position has been reached.
                path.remove();
            }

            // Use the dynamic graph to check if the connection still exists (can disappear due to roadworks).
            if (this.existsConnectionForNextMove(nextPosition)) {
                // Perform the actual move
                MoveProgress progress = this.roadModel.moveTo(this, nextPosition, time);

                this.battery.decreaseCapacity(progress.distance().doubleValue(SI.METER));
            } else {
                throw new IllegalStateException("Trying to move towards position for which there is no connection."
                        + " position = " + this.getPosition().get() + ", nextPosition = " + nextPosition);
            }
        }
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
     * True if the robot is carrying at least one PizzaParcel.
     */
    public boolean hasPizzaParcel() {
        return this.currentParcels.size() > 0;
    }

    /**
     * Returns the robots remaining capacity for carrying pizzas.
     */
    public int getRemainingCapacity() {
        return new Double(this.getCapacity()).intValue() - this.currentAmountOfPizzas;
    }

    /**
     * Delivers a PizzaParcel for a certain DeliveryTask.
     * <p>
     * Delivers the parcel in the PDPModel, decreases the amount of pizzas for the DeliveryTask in the PizzeriaModel,
     * and decreases the amount of pizzas held by the robot.
     */
    private void deliverCurrentPizzaParcel(@NotNull TimeLapse time) {
        System.out.println("RobotAgent.deliverCurrentPizzaParcel");

        PizzaParcel parcel = this.currentParcels.peek();

        this.removePizzaParcelFromRobot(parcel);
        this.pdpModel.deliver(this, parcel, time);
        this.pizzeriaModel.deliverPizzaParcel(this, parcel, time.getEndTime());

        System.out.println("[INFO] Delivered " + parcel.amountOfPizzas + " pizzas. Current capacity left:" + this.currentParcels.size());
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
     * Returns the remaining capacity of the robots battery as a percentage.
     */
    public double getRemainingBatteryCapacityPercentage() {
        return this.battery.getRemainingCapacityPercentage();
    }

    /**
     * Sets the robot up for charging at a charging station. The robot is linked to the ChargingStation it arrived at.
     */
    private void arriveAtChargingStation() {
        System.out.println("arriveAtChargingStation");
        this.goingToCharge = false;
        this.isCharging = true;

        // Find the charging station the robot arrived at.
        Optional<ChargingStation> station = this.pizzeriaModel.getChargingStationAtPosition(this.getPosition().get());

        if (station.isPresent()) {
            this.pizzeriaModel.robotArrivedAtChargingStation(this, station.get());
            this.currentChargingStation = station;

        } else {
            throw new IllegalStateException("Trying to arrive at charging station but no station at position "
                    + this.getPosition().get());
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

    private List<DesireAnt> getHighestDesires() {
        System.out.println("RobotAgent.getHighestDesires: " + this.desireAnts.size());

        List<DesireAnt> bestTasks = new LinkedList<>();
        int remainingCapacity = this.getRemainingCapacity();

        System.out.println("remainingCapacity = " + remainingCapacity);
        // Get all tasks with highest score sorted in descending
        for (Map.Entry<DesireAnt, Long> entry : sortMapDescending(this.desireAnts)) {
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

    private List<Map.Entry<DesireAnt, Long>> sortMapDescending(HashMap<DesireAnt, Long> map) {
        // From: https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/

        LinkedList<Map.Entry<DesireAnt, Long>> list = new LinkedList<>(map.entrySet());

        // Defined Custom Comparator here
        list.sort(Comparator.comparing(Map.Entry::getValue));

        return Lists.reverse(list);
    }
}