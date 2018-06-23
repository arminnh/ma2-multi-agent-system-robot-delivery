package mas.agents;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import mas.buildings.ChargingStation;
import mas.buildings.RoadWorks;
import mas.messages.*;
import mas.tasks.DeliveryTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceAgent implements CommUser, TickListener {

    public final Point position;
    private CommDevice commDevice;
    private boolean first_tick = true;

    private List<ResourceAgent> neighbors = new LinkedList<>();
    private Optional<RoadWorks> roadWorks = Optional.absent();
    private Optional<ChargingStation> chargingStation = Optional.absent();
    private HashMap<Integer, DeliveryTask> deliveryTasks = new HashMap<>();

    private HashMap<Integer, List<DeliveryTaskReservation>> deliveryReservations = new HashMap<>();
    private List<ChargingStationReservation> chargingStationReservations = new LinkedList<>();
    private long intentionReservationLifetime;
    private long robotTimePerHop;
    private long tickLength;
    private boolean verbose;

    public ResourceAgent(
            Point position,
            long intentionReservationLifetime,
            int nodeDistance,
            double robotSpeed,
            long tickLength,
            boolean verbose) {
        this.position = position;
        this.intentionReservationLifetime = intentionReservationLifetime;
        // distance in m, speed in m/s, travel time between two nodes = (distance / speed)
        this.robotTimePerHop = (long) (nodeDistance / robotSpeed);
        this.tickLength = tickLength;
        this.verbose = verbose;
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(2);
        this.commDevice = builder.build();
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }

    public List<ResourceAgent> getNeighbors() {
        return this.neighbors;
    }

    public Optional<RoadWorks> getRoadWorks() {
        return roadWorks;
    }

    public void setRoadWorks(RoadWorks roadWorks) {
        this.roadWorks = Optional.of(roadWorks);
    }

    public void removeRoadWorks() {
        this.roadWorks = Optional.absent();
    }

    public void setChargingStation(ChargingStation chargingStation) {
        this.chargingStation = Optional.of(chargingStation);
    }

    public void addDeliveryTask(DeliveryTask task) {
        this.deliveryTasks.put(task.id, task);
        this.deliveryReservations.put(task.id, new LinkedList<>());
    }

    public void removeDeliveryTask(DeliveryTask task) {
        this.deliveryTasks.remove(task.id);
        this.deliveryReservations.remove(task.id);
    }

    public int getPizzasLeftForDeliveryTask(Integer deliveryTaskID) {
        DeliveryTask task = this.deliveryTasks.get(deliveryTaskID);
        List<DeliveryTaskReservation> reservations = this.deliveryReservations.get(deliveryTaskID);

        int pizzasInReservations = 0;
        for (DeliveryTaskReservation r : reservations) {
            pizzasInReservations += r.pizzaAmount;
        }

        return task.getPizzasRemaining() - pizzasInReservations;
    }

    @Override
    public void tick(@NotNull TimeLapse timeLapse) {
        if (first_tick) {
            first_tick = false;
            this.commDevice.broadcast(Messages.NICE_TO_MEET_YOU);
        }

        this.readMessages(timeLapse);

        this.evaporateReservations(timeLapse);
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    private void readMessages(TimeLapse timeLapse) {
        for (Message m : this.commDevice.getUnreadMessages()) {

            if (m.getContents() == Messages.NICE_TO_MEET_YOU) {
                neighbors.add((ResourceAgent) m.getSender());

            } else if (m.getContents().getClass() == DesireAnt.class) {
                this.handleDesireAnt(m, timeLapse);

            } else if (m.getContents().getClass() == ExplorationAnt.class) {
                this.handleExplorationAnt(m, timeLapse);

            } else if (m.getContents().getClass() == IntentionAnt.class) {
                this.handleIntentionAnt(m, timeLapse);
            }
        }
    }

    /**
     * Evaporates deliveryReservations
     */
    private void evaporateReservations(@NotNull TimeLapse timeLapse) {
        // Remove delivery task reservations
        for (Integer k1 : this.deliveryReservations.keySet()) {
            int oldSize = this.deliveryReservations.get(k1).size();
            this.deliveryReservations.get(k1).removeIf(r -> r.evaporationTimestamp < timeLapse.getStartTime());
            if (oldSize > this.deliveryReservations.get(k1).size() && this.verbose) {
                System.out.println("Evaporation of task " + k1 + " at " + timeLapse.getStartTime());
            }
        }

        // Remove charging station reservations
        // int oldSize = this.chargingStationReservations.size();
        this.chargingStationReservations.removeIf(r -> r.evaporationTimestamp < timeLapse.getStartTime());
    }

    private void handleDesireAnt(Message m, TimeLapse time) {
        DesireAnt ant = (DesireAnt) m.getContents();
        if (this.verbose) {
            System.out.println("Desire ant at " + this.position + ": " + ant);
        }

        if (ant.hasReachedDestination()) {
            if (ant.isReturning) {
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), true, null, null, 0), ant.robot);
            } else {
                DeliveryTask task = this.deliveryTasks.get(ant.deliveryTaskID);

                // Calculate distance
                int pizzas = 0;
                Long score = 0L;

                if (task != null) {
                    pizzas = this.getPizzasLeftForDeliveryTask(task.id);
                    if (pizzas > 0) {
                        score = task.getWaitingTime(time.getStartTime());
                        //score = task.getScore();
                    }
                }

                DesireAnt newAnt = ant.copy(Lists.reverse(ant.path), true, score, pizzas, 0);

                this.sendAntToNextHop(newAnt, time);
            }
        } else {
            this.sendAntToNextHop(ant, time);
        }
    }

    private void handleExplorationAnt(Message m, TimeLapse time) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();
        if (this.verbose) {
            System.out.println("Exploration ant at " + this.position + ": " + ant);
        }

        // Check if the ant reached its current destination. Once the ant reached the original destination, the
        // destination and path are reversed towards the RobotAgent that sent the ant.
        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                // The ant reached the RobotAgent.
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), null, null, 0), ant.robot);
            } else {
                // Create new list of IntentionData for next ant
                List<IntentionData> newDeliveriesData = this.updateExplorationAntIntentionData(ant);

                if (ant.hasReachedFinalDestination()) {
                    this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData, 0), time);
                } else {
                    this.sendAntToNextHop(ant.copy(ant.path, false, newDeliveriesData, ant.pathIndex), time);
                }
            }

        } else {
            this.sendAntToNextHop(ant, time);
        }
    }

    private void handleIntentionAnt(Message m, TimeLapse time) {
        IntentionAnt ant = (IntentionAnt) m.getContents();
        if (this.verbose) {
            System.out.println("Intention ant at " + this.position + ": " + ant);
        }

        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), true, ant.intentions, 0), ant.robot);
            } else {
                if (ant.toChargingStation) {
                    this.handleIntentionAntForChargingStation(ant, time);
                } else {
                    this.handleIntentionAntForDeliveryTask(ant, time);
                }
            }
        } else {
            this.sendAntToNextHop(ant, time);
        }
    }

    private void handleIntentionAntForDeliveryTask(IntentionAnt ant, TimeLapse time) {
        if (this.verbose) {
            System.out.println("ResourceAgent.handleIntentionAntForDeliveryTask");
        }
        // Get data of all DeliveryTasks on this position
        List<IntentionData> newDeliveriesData = new LinkedList<>();

        for (IntentionData intentionData : ant.intentions) {
            if (intentionData.position.equals(this.position) && intentionData.deliveryTaskID != 0) {
                // Fetch the relevant DeliveryTask
                DeliveryTask task = this.deliveryTasks.get(intentionData.deliveryTaskID);
                if (this.verbose) {
                    System.out.println("task = " + task);
                }
                // Check if a reservation can be updated or can be made (= if the task has pizzas to be delivered)
                if (task != null) {
                    boolean updated = this.updateDeliveryReservation(task, intentionData, time);

                    if (updated) {
                        // The reservation has been updated, set 'confirmed' to true in the delivery data.
                        newDeliveriesData.add(intentionData.copy(true));

                    } else {
                        if (this.verbose) {
                            System.out.println("Not updated, Pizzas: " + intentionData.pizzas + ", Pizzas left for task: " + this.getPizzasLeftForDeliveryTask(task.id) + " taskID: " + intentionData.deliveryTaskID + " pos: " + intentionData.position);
                        }

                        if (this.getPizzasLeftForDeliveryTask(task.id) > 0
                                && intentionData.pizzas <= this.getPizzasLeftForDeliveryTask(task.id)) {

                            createReservation(time, intentionData, task);

                            // A reservation has been created, set 'confirmed' to true in the delivery data.
                            newDeliveriesData.add(intentionData.copy(true));

                        } else {
                            if (this.verbose) {
                                System.out.println("Denied ant = [" + ant + "]");
                            }
                            // A reservation could not be created, set 'confirmed' to true in the delivery data.
                            newDeliveriesData.add(intentionData.copy(false));
                        }
                    }
                } else {
                    // Task has already been handled by other robot.
                    newDeliveriesData.add(intentionData.copy(false));
                }
            } else {
                // Nothing to be done for this delivery data.
                newDeliveriesData.add(intentionData);
            }
        }

        // Send the ants
        if (ant.hasReachedFinalDestination()) {
            this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData, 0), time);
        } else {
            this.sendAntToNextHop(ant.copy(ant.path, false, newDeliveriesData, ant.pathIndex), time);
        }
    }

    public boolean hasRobotChargeReservation(int robotID) {
        for (ChargingStationReservation resv : this.chargingStationReservations) {
            if (resv.robotID == robotID) {
                return true;
            }
        }
        return false;
    }

    private boolean canRobotChargeAtTime(TimeLapse currentTime, long travelTime) {
        int usedSlots = 0;
        int maxCap = this.chargingStation.get().capacity;
        long arrivalTime = currentTime.getEndTime() + travelTime;

        for (ChargingStationReservation resv : this.chargingStationReservations) {
            if (resv.evaporationTimestamp >= arrivalTime) {
                if (this.verbose) {
                    System.out.println("ArrivalTime: " + arrivalTime + " evapTime: " + resv.evaporationTimestamp);
                }
                usedSlots++;
            }
        }

        return usedSlots < maxCap;
    }

    private void handleIntentionAntForChargingStation(IntentionAnt ant, TimeLapse time) {
        if (this.chargingStation.isPresent()) {
            List<IntentionData> newDeliveriesData = new LinkedList<>();
            if (ant.intentions.size() > 1) {
                throw new IllegalStateException("Ant has more than 1 intention while going to chargingstation: " + ant);
            }

            IntentionData intentionData = ant.intentions.get(0);

            boolean update = updateChargingReservation(ant.estimatedTime, intentionData, time);
            if (update) {
                newDeliveriesData.add(intentionData.copy(true));
            } else {
                // We have less reservations than the total capacity of the charging station
                if (this.verbose) {
                    System.out.println("this.chargingStationReservations.size() = " + this.chargingStationReservations.size());
                    System.out.println("this.chargingStation.get().getChargeCapacity() = " + this.chargingStation.get().capacity);
                }

                if (canRobotChargeAtTime(time, ant.estimatedTime)) {
                    // Confirm the reservation
                    newDeliveriesData.add(intentionData.copy(true));

                    // Add new reservation to the list
                    ChargingStationReservation resv = new ChargingStationReservation(ant.robotID, time.getEndTime() + this.intentionReservationLifetime);
                    this.chargingStationReservations.add(resv);
                    if (this.verbose) {
                        System.out.println("Creating Reservation at charging station. " + this.chargingStationReservations.size() + "/ " + this.chargingStation.get().capacity + " resv in total.");
                    }
                } else {
                    newDeliveriesData.add(intentionData.copy(false));
                }
            }

            if (this.verbose) {
                System.out.println("ResourceAgent.handleIntentionAntForChargingStation: " + this.chargingStation.get());
            }
            this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData, 0), time);
        } else {
            throw new IllegalStateException("Ant arrived at destination with `toChargingStation = true`," +
                    "but there is no charging station at destination");
        }
    }

    @NotNull
    private List<IntentionData> updateExplorationAntIntentionData(ExplorationAnt ant) {
        List<IntentionData> newDeliveriesData = new LinkedList<>();

        for (IntentionData intentionData : ant.intentions) {
            if (intentionData.position == this.position && intentionData.deliveryTaskID != 0) {
                // Get the task with the correct deliveryTaskID
                DeliveryTask task = this.deliveryTasks.get(intentionData.deliveryTaskID);

                // Calculate the (possibly new) amount of pizzas that the robot can send for this task
                if (this.verbose) {
                    System.out.println("updateExplorationAntIntentionData for task " + task);
                }
                Integer newPizzaAmount = 0;

                if (this.deliveryTasks.containsKey(intentionData.deliveryTaskID)) {
                    newPizzaAmount = Math.min(this.getPizzasLeftForDeliveryTask(task.id), intentionData.pizzas);
                }

                // The confirm of an exploration ant doesn't matter so we can just set it on false
                newDeliveriesData.add(new IntentionData(intentionData.position, intentionData.robotID,
                        intentionData.deliveryTaskID, newPizzaAmount, false));

            } else {
                // Nothing to be done for this delivery data.
                newDeliveriesData.add(intentionData);
            }
        }

        return newDeliveriesData;
    }

    private void createReservation(TimeLapse timeLapse, IntentionData intentionData, DeliveryTask task) {
        // Make the reservation and send the ant back to confirm.
        long evaporationTimestamp = timeLapse.getEndTime() + this.intentionReservationLifetime;
        DeliveryTaskReservation reservation = new DeliveryTaskReservation(intentionData.robotID,
                task.id, intentionData.pizzas, evaporationTimestamp
        );

        this.deliveryReservations.get(task.id).add(reservation);
        if (this.verbose) {
            System.out.println("Reservation created for task " + task.id + ", evaporation at: " + evaporationTimestamp);
            System.out.println("Reservation for task " + task.id + ": " + this.getReservationsForTask(task.id));
        }
    }

    public void robotArrivesAtChargingStation(Integer robotId, double amRechargeNeeded, TimeLapse time) {
        ChargingStationReservation oldResv = null;
        ChargingStationReservation newResv = null;

        for (ChargingStationReservation resv : this.chargingStationReservations) {
            if (resv.robotID == robotId) {
                oldResv = resv;
                Double chargeTime = tickLength * amRechargeNeeded / this.chargingStation.get().rechargeCapacity;
                newResv = new ChargingStationReservation(robotId, time.getEndTime() + chargeTime.longValue());
            }
        }
        this.chargingStationReservations.remove(oldResv);
        this.chargingStationReservations.add(newResv);
    }

    private boolean updateDeliveryReservation(DeliveryTask task, IntentionData intentionData, TimeLapse timeLapse) {
        // If the reservation has already been made by the same robot, update it.
        List<DeliveryTaskReservation> reservations = this.deliveryReservations.get(task.id).stream()
                .filter(r -> r.deliveryTaskID == intentionData.deliveryTaskID && r.robotID == intentionData.robotID)
                .collect(Collectors.toList());

        if (reservations.size() > 0) {
            // Update the timer.
            DeliveryTaskReservation r = reservations.get(0);
            DeliveryTaskReservation new_reservation = r.copy(timeLapse.getEndTime() + this.intentionReservationLifetime);

            this.deliveryReservations.get(task.id).remove(r);
            this.deliveryReservations.get(task.id).add(new_reservation);
            return true;
        }

        return false;
    }

    private boolean updateChargingReservation(long estimatedTime, IntentionData intentionData, TimeLapse timeLapse) {


        int usedSlots = 0;
        int maxCap = this.chargingStation.get().capacity;
        long arrivalTime = timeLapse.getEndTime() + estimatedTime;
        ChargingStationReservation myResv = null;

        for (ChargingStationReservation resv : this.chargingStationReservations) {
            if (resv.evaporationTimestamp >= arrivalTime && resv.robotID != intentionData.robotID) {
                usedSlots++;
            }
            if (resv.robotID == intentionData.robotID) {
                myResv = resv;
            }
        }

        if (usedSlots < maxCap && myResv != null) {
            // Update the timer.
            ChargingStationReservation new_reservation = myResv.copy(timeLapse.getEndTime() + this.intentionReservationLifetime);

            this.chargingStationReservations.remove(myResv);
            this.chargingStationReservations.add(new_reservation);
            return true;
        }

        return false;
    }

    private void sendAntToNextHop(Ant ant, TimeLapse time) {
        if (this.verbose) {
            System.out.println("ResourceAgent.sendAntToNextHop");
        }

        if (ant.path.size() == 0) {
            throw new IllegalStateException("Cannot send an ant to next hop with an empty path");
        }

        if (ant.path.size() == 1) {
            // With only 1 node in the path, the ant can be sent straight back to the robot.
            this.commDevice.send(ant, ant.robot);
            return;
        }

        long estimatedTime = ant.estimatedTime;
        if (!ant.isReturning) {
            // Want time in ms
            estimatedTime += this.robotTimePerHop * 1000;
        }

        int nextPositionIndex = ant.pathIndex + 1;
        Point nextPosition = ant.path.get(nextPositionIndex);

        boolean sentOutAnt = false;
        for (ResourceAgent neighbor : this.neighbors) {
            if (neighbor.getPosition().get().equals(nextPosition)) {
                Optional<RoadWorks> roadWorks = neighbor.getRoadWorks();
                if (roadWorks.isPresent()) {
                    estimatedTime += roadWorks.get().endTimestamp - time.getStartTime();
                }
                sentOutAnt = true;
                this.commDevice.send(ant.copy(estimatedTime, nextPositionIndex), neighbor);
            }
        }

        if (!sentOutAnt) {
            throw new IllegalStateException("Ant didn't get sent: " + ant);
        }
    }

    public void dropReservation(RobotAgent agent) {
        this.chargingStationReservations.removeIf(r -> r.robotID == agent.id);
    }

    public boolean robotHasReservation(int robotID, int taskID) {
        if (!this.deliveryReservations.containsKey(taskID)) {
            return false;
        }

        return this.deliveryReservations.get(taskID).stream()
                .filter(r -> r.robotID == robotID).count() != 0;
    }

    public List<DeliveryTaskReservation> getReservationsForTask(int taskID) {
        return this.deliveryReservations.get(taskID);
    }

    public boolean robotCanChargeUntil(int robotId, TimeLapse time, double capacityUsed) {
        Double rechargeTill = time.getEndTime() + tickLength * capacityUsed / this.chargingStation.get().rechargeCapacity;
        int usedSlots = 0;
        int maxCap = this.chargingStation.get().capacity;

        for (ChargingStationReservation resv : this.chargingStationReservations) {
            if (resv.evaporationTimestamp >= rechargeTill.longValue() && resv.robotID != robotId) {
                usedSlots++;
            }
        }

        return usedSlots < maxCap;
    }
}
