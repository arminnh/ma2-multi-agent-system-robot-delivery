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
import mas.buildings.ChargingStation;
import mas.graphs.AStar;
import mas.messages.*;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.tasks.DeliveryTask;
import mas.tasks.PizzaParcel;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of a delivery robot.
 */
public class RobotAgent extends Vehicle implements MovingRoadUser, TickListener, RandomUser, CommUser, PizzeriaUser {

    public final int id;
    private final int alternativePathsToExplore;
    private final long batteryRescueDelay;
    private final long explorationRefreshTime;
    private final long intentionRefreshTime;

    private RandomGenerator rng;
    private RoadModel roadModel;
    private PDPModel pdpModel;
    private CommDevice commDevice;
    private PizzeriaModel pizzeriaModel;
    private ListenableGraph<LengthData> staticGraph;
    private ListenableGraph<LengthData> dynamicGraph;

    private Battery battery;
    private boolean isOnNode = true;
    private Point pizzeriaPosition;
    private boolean goingToPizzeria = false;
    private boolean isAtPizzeria = true;
    private Point chargingStationPosition;
    private boolean goingToCharge = false;
    private boolean isCharging = false;
    private Optional<ChargingStation> currentChargingStation = Optional.absent();
    private long idleTime = 0;
    private long chargeTime = 0;

    private int currentAmountOfPizzas = 0;
    private Queue<PizzaParcel> currentParcels = new LinkedList<>();
    private long intendedArrivalTime = 0;
    private List<IntentionAnt> intentionAnts = new LinkedList<>();
    private int waitingForIntentionAnts = 0;
    private Optional<Queue<Point>> intention = Optional.absent();
    private Optional<Long> nextIntentionAntsUpdate = Optional.absent();
    private HashMap<DesireAnt, Long> desireAnts = new HashMap<>();

    private int waitingForDesireAnts = 0;
    private List<ExplorationAnt> explorationAnts = new LinkedList<>();
    private int waitingForExplorationAnts = 0;
    private Optional<Long> nextExplorationAntsUpdate = Optional.absent();
    private Optional<Long> drainedBatteryRescueTime = Optional.absent();
    private boolean verbose;

    public RobotAgent(
            int id,
            int capacity,
            double speed,
            ListenableGraph<LengthData> staticGraph,
            Battery battery,
            long batteryRescueDelay,
            Point pizzeriaPosition,
            Point chargingStationPosition,
            int alternativePathsToExplore,
            long explorationRefreshTime,
            long intentionRefreshTime,
            boolean verbose) {
        super(VehicleDTO.builder().capacity(capacity).startPosition(pizzeriaPosition).speed(speed).build());

        this.id = id;
        this.battery = battery;
        this.staticGraph = staticGraph;
        this.pizzeriaPosition = pizzeriaPosition;
        this.chargingStationPosition = chargingStationPosition;
        this.alternativePathsToExplore = alternativePathsToExplore;
        this.batteryRescueDelay = batteryRescueDelay;
        this.explorationRefreshTime = explorationRefreshTime;
        this.intentionRefreshTime = intentionRefreshTime;
        this.verbose = verbose;
    }

    @Override
    public String toString() {
        return "Robot " + this.id + " parcels: " + this.currentParcels.size() + ", intention: " + this.intention +
                ", isCharging: " + this.isCharging + ", isAtPizzeria: " + this.isAtPizzeria + ", goingToCharge: " +
                this.goingToCharge + ", goingToPizzeria: " + this.goingToPizzeria + ". Waiting for ants: desire: " +
                this.waitingForDesireAnts + ", exploration: " + this.waitingForExplorationAnts + ", intention: " +
                this.waitingForIntentionAnts;
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

    public boolean waitingForAnts() {
        return this.waitingForExplorationAnts > 0 || this.waitingForIntentionAnts > 0 || this.waitingForDesireAnts > 0;
    }

    private void setIntentionForAnt(Ant ant) {
        this.intendedArrivalTime = ant.estimatedTime;
        this.intention = Optional.of(new LinkedList<>(ant.path));
    }

    public long getIntendedArrivalTime() {
        return intendedArrivalTime;
    }

    private void deleteIntention() {
        this.intendedArrivalTime = 0;
        this.intention = Optional.absent();
    }

    private Optional<PizzaParcel> getPizzaParcelAtCurrentPosition() {
        Point position = this.getPosition().get();

        for (PizzaParcel p : this.currentParcels) {
            if (p.getDeliveryLocation().equals(position)) {
                return Optional.of(p);
            }
        }

        return Optional.absent();
    }

    public boolean isIdle() {
        // Charging does not count as idle
        if (this.isCharging) {
            return false;
        }

        if (this.intention.isPresent()) {
            // If intention is present, robot will move except when waiting for ants. So is idle if waiting for ants
            return this.waitingForAnts();
        } else {
            // No intention present, so robot is standing still
            return true;
        }
    }

    public long getIdleTime() {
        return this.idleTime;
    }

    public long getAndResetIdleTime() {
        long time = this.idleTime;
        this.idleTime = 0;
        return time;
    }

    public long getAndResetChargeTime() {
        long time = this.chargeTime;
        this.chargeTime = 0;
        return time;
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
            throw new IllegalStateException("Can only broadcast ants if standing on a node. Robot " + this.id);
        }
        this.commDevice.broadcast(ant);
    }

    /**
     * The main logic of the robot. Handles messages, (re)evaluates paths and intentions, and makes the robot perform
     * actions in the virtual environment.
     */
    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        if (this.getRemainingBatteryCapacityPercentage() == 0.0 && !this.isCharging) {
            this.rechargeBatteryIfRescued(time);
        } else {
            // Read all new messages
            this.readMessages();

            // Process different types of ants if enough of them have returned
            this.processAnts(time);

            // Check if the current intentions should be replaced.
            this.reconsiderIntentions();

            // Resend intention or exploration ants if necessary.
            this.resendAnts(time);

            // Make the robot do its next action
            if (this.intention.isPresent() && !this.waitingForAnts() && !this.isCharging) {
                this.doAction(time);
            } else if (this.isIdle()) {
                this.idleTime += time.getTickLength();
            }
        }
    }

    private void rechargeBatteryIfRescued(TimeLapse timeLapse) {
        if (!this.drainedBatteryRescueTime.isPresent()) {
            this.drainedBatteryRescueTime = Optional.of(timeLapse.getStartTime() + this.batteryRescueDelay);
        } else {
            if (timeLapse.getStartTime() >= this.drainedBatteryRescueTime.get()) {
                this.battery.increaseCapacity(this.battery.maxCapacity);
                this.drainedBatteryRescueTime = Optional.absent();
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
            this.processExploredPathsAndReviseIntentions(time);
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
        List<DesireAnt> bestTasks = this.getHighestDesires();
        List<IntentionData> intentionData = new LinkedList<>();

        if (this.verbose) {
            System.out.println("RobotAgent.processDesiresAndStartExploration: " + this.desireAnts);
            System.out.println("BestTasks (" + bestTasks.size() + "):" + bestTasks);
        }

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
    private void processExploredPathsAndReviseIntentions(TimeLapse time) {
        if (this.verbose) {
            System.out.println("RobotAgent.processExploredPathsAndReviseIntentions, this.intention.isPresent(): " + this.intention.isPresent());
        }
        Long bestTime = Long.MAX_VALUE;
        ExplorationAnt bestAnt = null;

        for (ExplorationAnt ant : this.explorationAnts) {
            if (ant.estimatedTime < bestTime) {
                bestTime = ant.estimatedTime;
                bestAnt = ant;
            }
            if (ant.path.get(ant.path.size() - 1).equals(this.chargingStationPosition) && this.goingToCharge) {
                bestAnt = ant;
                // We break here because charging takes precedence over everything else.
                break;
            }
        }

        this.explorationAnts.clear();
        if (bestAnt == null) {
            return;
        }

        // If exploration ants were sent while an intention was already present, then they were sent to find a better
        // path for the current intention. => See if there is a new better path for the current intention.
        if (this.intention.isPresent()) {
            if (this.verbose) {
                System.out.println("EXPLORED PATHS WHILE HAVING INTENTION, bestAnt = " + bestAnt);
            }

            // Set the new path if it is better
            if (bestAnt.estimatedTime < this.intendedArrivalTime) {
                if (this.verbose) {
                    System.out.println("SET INTENTION TO BETTER PATH");
                }

                this.setIntentionForAnt(bestAnt);
            }
        } else {
            // If no current intention and going to pizzeria, no need for intention ant. No reservations on pizzerias.
            if (this.goingToPizzeria) {
                this.setIntentionForAnt(bestAnt);
            } else {
                // If no intention, then robot was looking for first path towards new parcels or the parcels already carried.
                // Drop all carried parcels for tasks for which the amount of pizzas is 0
                for (IntentionData intention : bestAnt.intentions) {
                    if (intention.pizzas == 0) {
                        this.dropParcelForTaskIfCarrying(intention.deliveryTaskID, time);
                    }
                }

                this.sendIntentionAnt(bestAnt);
            }
        }
    }

    private void dropParcelForTaskIfCarrying(int deliveryTaskID, TimeLapse time) {
        java.util.Optional<PizzaParcel> pizzaParcel = this.currentParcels.stream()
                .filter(p -> p.deliveryTaskID == deliveryTaskID).findFirst();

        pizzaParcel.ifPresent(pp -> this.dropParcel(pp, time));
    }

    /**
     * Processes the IntentionAnts that have returned. Currently, only one IntentionAnt at a time is supported.
     */
    private void processIntentionAnts(TimeLapse time) {
        if (this.verbose) {
            System.out.println("RobotAgent.processIntentionAnts: " + this.intentionAnts);
        }

        IntentionAnt ant = this.intentionAnts.remove(0);

        if (ant.toChargingStation) {
            if (this.verbose) {
                System.out.println("SET TO GO CHARGE, this.hasPizzaParcel() = " + this.hasPizzaParcel() + ", this.intention = " + this.intention);
            }
            if (ant.intentions.get(0).reservationConfirmed) {
                this.setIntentionForAnt(ant);
            }

        } else {
            for (IntentionData intentionData : ant.intentions) {
                Optional<PizzaParcel> parcelForTask = this.getParcelForDeliveryTaskID(intentionData.deliveryTaskID);

                // If a reservation has been confirmed, create a new parcel if necessary
                if (intentionData.reservationConfirmed) {
                    if (!parcelForTask.isPresent()) {
                        PizzaParcel parcel = this.pizzeriaModel.createPizzaParcel(intentionData.deliveryTaskID,
                                this.getPosition().get(), intentionData.pizzas, time.getStartTime());

                        this.pdpModel.pickup(this, parcel, time);

                        this.addPizzaParcel(parcel);
                    }
                } else {
                    // A reservation was denied or has expired, if the robot is carrying the parcel for the reservation,
                    // make it drop the parcel.
                    if (parcelForTask.isPresent()) {
                        if (this.verbose) {
                            System.out.println("Dropping parcel!!" + parcelForTask);
                        }
                        this.deleteIntention();
                        this.dropParcel(parcelForTask.get(), time);
                    }
                }
            }

            if (this.hasPizzaParcel()) {
                if (this.verbose) {
                    System.out.println("Has PizzaParcel, setting intention to " + ant.path);
                }
                this.setIntentionForAnt(ant);
            }
        }
    }

    /**
     * Updates the robot's intention if another intention should be chosen.
     */
    private void reconsiderIntentions() {
        // If already waiting for ants, new intentions are being looked for.
        if (this.waitingForAnts() || !this.isOnNode) {
            return;
        }

        // If the path the robot is currently trying to take does not exist anymore, remove the current intention.
        if (this.intention.isPresent() && !this.existsConnectionForNextMove()) {
            if (this.verbose) {
                System.out.println("DELETE THE INTENTION BECAUSE THERE WAS NO CONNECTION");
            }
            this.deleteIntention();
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
                if (this.verbose) {
                    System.out.println("NO INTENTION BUT GOT PARCELS, CHECKING NEW PATHS FOR PARCELS");
                }
                this.explorePathsForCurrentParcels();
                return;
            }

            // If at pizzeria, ask for tasks.
            if (this.isAtPizzeria) {
                List<DeliveryTask> tasks = this.pizzeriaModel.getDeliveryTasks();
                if (!tasks.isEmpty()) {
                    if (this.verbose) {
                        System.out.println("SENT OUT DESIRE ANTS TO EXPLORE TASKS");
                    }
                    this.sendDesireAntsForTasks(tasks);
                    return;
                }
            }

            // Go to a pizzeria to get new tasks. Only explore new paths towards pizzeria if not already
            // at pizzeria and not charging.
            if (!this.isAtPizzeria && !this.isCharging && !this.goingToCharge) {
                if (this.verbose) {
                    System.out.println("SENT OUT EXPLORATION ANTS TO GO TO PIZZERIA");
                }
                this.goingToPizzeria = true;
                this.explorePaths(this.pizzeriaPosition);
                return;
            }

        } else {
            // Else, check if there is a better or more important intention.
            // Remove the current intention if another one should be set up.

            // Check if need to charge.
            if (this.getRemainingBatteryCapacityPercentage() <= 0.3 && !this.goingToCharge && !this.isCharging) {
                if (this.verbose) {
                    System.out.println("SET ROBOT TO GO TO CHARGE");
                }
                this.deleteIntention();
                this.goingToCharge = true;
                this.goingToPizzeria = false;
                this.explorePaths(this.chargingStationPosition);
                return;
            }

            // If intention is present, the robot is carrying no parcels (delivered or dropped), and it is not going to
            // pizzeria or charging station, then delete intention.
            if (this.currentParcels.size() == 0 && !this.goingToCharge && !this.goingToPizzeria) {
                if (this.verbose) {
                    System.out.println("DELETING INTENTION AFTER DELIVERING OR DROPPING LAST PARCEL");
                }
                this.deleteIntention();
            }
        }
    }

    /**
     * Makes the robot move and then optionally deliver a package or to a charging station or pizzeria.
     */
    private void doAction(@NotNull TimeLapse time) {
        // If an intention is present, make the robot follow it.
        this.move(time);

        // Deliver a PizzaParcel if applicable
        Optional<PizzaParcel> pizzaParcel = this.getPizzaParcelAtCurrentPosition();
        if (pizzaParcel.isPresent()) {
            if (this.pizzeriaModel.canDeliverPizzaParcel(this, pizzaParcel.get())) {
                this.deliverPizzaParcel(pizzaParcel.get(), time);
            } else {
                // Drop if reservation expired and an IntentionAnt has not informed the robot on time.
                this.dropParcel(pizzaParcel.get(), time);
            }
        }

        // If robot arrived at the destination of the intention, the intention will be empty
        if (this.intention.get().isEmpty()) {
            this.deleteIntention();

            if (this.goingToCharge) {
                this.arriveAtChargingStation(time);
            } else if (this.goingToPizzeria) {
                this.arriveAtPizzeria();
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
                            Optional.of(time.getEndTime() + this.intentionRefreshTime);

                } else if (this.nextIntentionAntsUpdate.get() < time.getEndTime() && !this.goingToPizzeria) {
                    this.nextIntentionAntsUpdate = Optional.absent();

                    this.resendIntentionAnts();
                }
            }

            if (this.waitingForExplorationAnts == 0) {
                if (!this.nextExplorationAntsUpdate.isPresent()) {
                    this.nextExplorationAntsUpdate =
                            Optional.of(time.getStartTime() + this.explorationRefreshTime);

                } else if (this.nextExplorationAntsUpdate.get() < time.getEndTime()) {
                    this.nextExplorationAntsUpdate = Optional.absent();

                    if (this.goingToPizzeria) {
                        if (this.verbose) {
                            System.out.println("RobotAgent.resendAnts goingToPizzeria");
                        }
                        this.explorePaths(this.pizzeriaPosition);

                    } else if (this.goingToCharge) {
                        if (this.verbose) {
                            System.out.println("RobotAgent.resendAnts goingToCharge");
                        }
                        this.explorePaths(this.chargingStationPosition);

                    } else {
                        if (this.verbose) {
                            System.out.println("RobotAgent.resendAnts for current parcels");
                        }
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
        if (this.verbose) {
            System.out.println("RobotAgent.resendIntentionAnts, " + this.id);
        }
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
                    0, 0, false));
        }

        IntentionAnt ant = new IntentionAnt(pathWithCurrentPos, 0, false, this.id,
                this, 0, intentions);

        this.broadcastAnt(ant);
        this.waitingForIntentionAnts++;
    }

    private void sendDesireAntsForTasks(List<DeliveryTask> tasks) {
        if (this.verbose) {
            System.out.println("sendDesireAntsForTasks");
        }

        for (DeliveryTask task : tasks) {

            List<Point> destinations = new LinkedList<>(Collections.singletonList(task.position));
            List<Point> path = this.getAlternativePaths(1, this.getPosition().get(), destinations).get(0);

            DesireAnt desireAnt = new DesireAnt(path, 0, false, this.id,
                    this, 0, 0L, task.id, 0);

            this.broadcastAnt(desireAnt);
            this.waitingForDesireAnts++;
        }

        if (this.verbose) {
            System.out.println("Sent out " + this.waitingForDesireAnts);
        }
    }

    /**
     * Sends out ExplorationAnts to explore each of the given paths.
     */
    private void sendExplorationAnts(List<List<Point>> paths, List<IntentionData> intentionData) {
        if (this.verbose) {
            System.out.println("RobotAgent.sendExplorationAnts, intentionData: " + intentionData + ", paths: " + paths);
        }

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
        // Only build IntentionAnt for the IntentionData that has pizzas > 0. When going to charge, pizzas should be 0.
        List<IntentionData> intentions;
        if (this.goingToCharge) {
            intentions = explorationAnt.intentions;
        } else {
            intentions = explorationAnt.intentions.stream()
                    .filter(i -> i.pizzas > 0)
                    .collect(Collectors.toList());
        }

        // Only send IntentionAnt if intentions could be sent for reservations.
        if (!intentions.isEmpty()) {
            IntentionAnt ant = new IntentionAnt(explorationAnt.path, 0, false, this.id, this,
                    0, intentions);
            this.waitingForIntentionAnts++;

            if (this.verbose) {
                System.out.println("Robot " + this.id + " sending IntentionAnt = " + ant);
            }
            this.broadcastAnt(ant);
        }
    }

    /**
     * Explores different paths towards a single destination.
     */
    private void explorePaths(Point destination) {
        if (this.verbose) {
            System.out.println("RobotAgent.explorePaths1");
        }
        List<IntentionData> intentions = new LinkedList<>();
        intentions.add(new IntentionData(destination, this.id, 0, 0, false));

        this.explorePaths(intentions);
    }

    /**
     * Explores different paths towards multiple destinations to be followed sequentially.
     */
    private void explorePaths(List<IntentionData> intentionData) {
        if (this.verbose) {
            System.out.println("RobotAgent.explorePaths2");
        }
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
        if (this.verbose) {
            System.out.println("RobotAgent.explorePathsForCurrentParcels");
        }
        List<IntentionData> intentions = new LinkedList<>();

        for (PizzaParcel parcel : this.currentParcels) {
            intentions.add(new IntentionData(parcel.getDeliveryLocation(), this.id,
                    parcel.deliveryTaskID, parcel.amountOfPizzas, false));
        }

        this.explorePaths(intentions);
    }

    private void handleExplorationAntMessage(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();
        if (this.verbose) {
            System.out.println("Robot " + this.id + " got " + ant);
        }

        this.explorationAnts.add(ant);
        this.waitingForExplorationAnts--;
    }

    private void handleDesireAntMessage(Message m) {
        DesireAnt ant = (DesireAnt) m.getContents();
        if (this.verbose) {
            System.out.println("Robot " + this.id + " got " + ant);
        }

        // Only store the ant if pizzas can be delivered for the task by this robot.
        if (ant.pizzas > 0) {
            this.desireAnts.put(ant, ant.score);
        }
        this.waitingForDesireAnts--;
    }

    private void handleIntentionAntMessage(Message m) {
        IntentionAnt ant = (IntentionAnt) m.getContents();
        if (this.verbose) {
            System.out.println("Robot " + this.id + " got " + ant);
        }

        this.intentionAnts.add(ant);
        this.waitingForIntentionAnts--;
    }

    private Optional<PizzaParcel> getParcelForDeliveryTaskID(int deliveryTaskID) {
        if (this.verbose) {
            System.out.println("RobotAgent.getParcelForDeliveryTaskID");
        }

        for (PizzaParcel p : this.currentParcels) {
            if (p.deliveryTaskID == deliveryTaskID) {
                return Optional.of(p);
            }
        }

        return Optional.absent();
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

            // Use the dynamic graph to check if the connection still exists (can disappear due to roadworks).
            if (this.existsConnectionForNextMove(nextPosition)) {
                // Perform the actual move
                MoveProgress progress = this.roadModel.moveTo(this, nextPosition, time);
                this.intendedArrivalTime -= progress.time().getValue();

                this.isOnNode = this.getPosition().get().equals(nextPosition);
                if (this.isOnNode) {
                    // Remove the next position in the intention when that position has been reached.
                    path.remove();
                }

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
        if (this.verbose) {
            System.out.println("Current parcels: " + this.currentParcels.size());
        }
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
    private void deliverPizzaParcel(PizzaParcel parcel, @NotNull TimeLapse time) {
        if (this.verbose) {
            System.out.println("RobotAgent.deliverCurrentPizzaParcel");
        }

        this.removePizzaParcelFromRobot(parcel);
        this.pdpModel.deliver(this, parcel, time);
        this.pizzeriaModel.deliverPizzaParcel(this, parcel, time.getEndTime());

        if (this.verbose) {
            System.out.println("[INFO] Delivered " + parcel.amountOfPizzas + " pizzas. Parcels left:" + this.currentParcels.size());
        }
    }

    /**
     * Makes the robot drop the parcel.
     */
    private void dropParcel(PizzaParcel parcel, TimeLapse time) {
        if (this.verbose) {
            System.out.println("RobotAgent.dropParcel");
        }
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
    private void arriveAtChargingStation(TimeLapse time) {
        if (this.verbose) {
            System.out.println("arriveAtChargingStation");
        }
        this.goingToCharge = false;
        this.isCharging = true;
        this.chargeTime = 0;

        // Find the charging station the robot arrived at.
        Optional<ChargingStation> station = this.pizzeriaModel.getChargingStationAtPosition(this.getPosition().get());

        if (station.isPresent()) {
            if(this.pizzeriaModel.canArriveAtChargingStation(this, time, this.battery.getCapacityUsed())){
                this.pizzeriaModel.robotArrivedAtChargingStation(this, station.get(), time, this.battery.getCapacityUsed());
                this.currentChargingStation = station;
            }
        } else {
            throw new IllegalStateException("Trying to arrive at charging station but no station at position "
                    + this.getPosition().get());
        }
    }

    /**
     * Charges the battery of the robot.
     */
    public void chargeBattery(double capacity, TimeLapse time) {
        this.battery.increaseCapacity(capacity);
        this.chargeTime += time.getTickLength();

        if (this.battery.isAtMaxCapacity()) {
            this.isCharging = false;
            pizzeriaModel.robotLeftChargingStation(this, this.currentChargingStation.get());
            this.currentChargingStation = Optional.absent();
        }
    }

    /**
     * Sets the relevant variables for arriving at a Pizzeria.
     */
    private void arriveAtPizzeria() {
        if (this.roadModel.getPosition(this).equals(this.pizzeriaPosition)) {
            if (this.verbose) {
                System.out.println("User at pizzeria!");
            }
            this.isAtPizzeria = true;
            this.goingToPizzeria = false;
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
        List<DesireAnt> bestTasks = new LinkedList<>();
        int remainingCapacity = this.getRemainingCapacity();

        if (this.verbose) {
            System.out.println("RobotAgent.getHighestDesires: " + this.desireAnts.size());
            System.out.println("remainingCapacity = " + remainingCapacity);
        }

        // Get all tasks with highest score sorted in descending
        HashMap<DesireAnt, Long> currentDesireAnts = this.desireAnts;
        while (bestTasks.size() < currentDesireAnts.size() && remainingCapacity > 0) {
            // Choose an ant randomly
            // Each ant has a change p proportional to its score to be chosen
            double p = this.rng.nextDouble();
            Long total_sum = sumAllDesireScores(currentDesireAnts);
            double current_sum = 0.0;
            DesireAnt chosenAnt = null;
            for (Map.Entry<DesireAnt, Long> entry : currentDesireAnts.entrySet()) {
                current_sum += entry.getValue().doubleValue() / total_sum.doubleValue();
                if (this.verbose) {
                    System.out.println("p val: " + p + " check with: " + current_sum);
                }
                if (p < current_sum) {
                    chosenAnt = entry.getKey();
                    break;
                }
            }
            int capacity = Math.min(remainingCapacity, chosenAnt.pizzas);
            remainingCapacity -= capacity;
            bestTasks.add(chosenAnt);
            currentDesireAnts.remove(chosenAnt);
        }

        return bestTasks;
    }

    private long sumAllDesireScores(HashMap<DesireAnt, Long> currentDesireAnts) {
        long sum = 0;
        for (Map.Entry<DesireAnt, Long> entry : currentDesireAnts.entrySet()) {
            sum += entry.getValue();
        }
        return sum;
    }

    private List<Map.Entry<DesireAnt, Long>> sortMapDescending(HashMap<DesireAnt, Long> map) {
        // From: https://beginnersbook.com/2013/12/how-to-sort-hashmap-in-java-by-keys-and-values/

        LinkedList<Map.Entry<DesireAnt, Long>> list = new LinkedList<>(map.entrySet());

        // Defined Custom Comparator here
        list.sort(Comparator.comparing(Map.Entry::getValue));

        return Lists.reverse(list);
    }

    public String getWaitingForAntsType() {
        if (this.waitingForIntentionAnts > 0) {
            return "i";
        } else if (this.waitingForExplorationAnts > 0) {
            return "e";
        } else if (this.waitingForDesireAnts > 0) {
            return "d";
        }
        return null;
    }
}