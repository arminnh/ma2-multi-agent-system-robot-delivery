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
import mas.SimulatorSettings;
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

    public ResourceAgent(Point position) {
        this.position = position;
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

    public void setRoadWorks(RoadWorks roadWorks) {
        this.roadWorks = Optional.of(roadWorks);
    }

    public Optional<RoadWorks> getRoadWorks() {
        return roadWorks;
    }

    public boolean hasRoadWorks() {
        return this.roadWorks.isPresent();
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

    private int getPizzasLeftForDeliveryTask(Integer deliveryTaskID) {
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
                this.handleDesireAnt(m);

            } else if (m.getContents().getClass() == ExplorationAnt.class) {
                this.handleExplorationAnt(m);

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
            if (oldSize > this.deliveryReservations.get(k1).size()) {
                System.out.println("Evaporation!");
            }
        }

        // Remove charging station reservations
        this.chargingStationReservations.removeIf(r -> r.evaporationTimestamp < timeLapse.getStartTime());
    }

    private void handleDesireAnt(Message m) {
        DesireAnt ant = (DesireAnt) m.getContents();
        System.out.println("Desire ant at " + this.position + ": " + ant);

        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), true, null, null, 0), ant.robot);
            } else {
                DeliveryTask task = this.deliveryTasks.get(ant.deliveryTaskID);

                // Calculate distance
                int amount = 0;
                Long score = 0L;
                if (task != null) {
                    amount = this.getPizzasLeftForDeliveryTask(task.id);
                    score = task.getScore();
                }

                DesireAnt newAnt = ant.copy(Lists.reverse(ant.path),
                        true, score, amount, 0);
                this.sendAntToNextHop(newAnt);
            }
        } else {
            this.sendAntToNextHop(ant);
        }
    }

    private void handleExplorationAnt(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();
        System.out.println("Exploration ant at " + this.position + ": " + ant);

        // Check if the ant reached its current destination. Once the ant reached the original destination, the
        // destination and path are reversed towards the RobotAgent that sent the ant.
        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                // The ant reached the RobotAgent.
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), null, null, 0), ant.robot);
            } else {
                // Create new list of IntentionData for next ant
                List<IntentionData> newDeliveriesData = this.updateExplorationAntIntentionData(ant);

                if (ant.hasReachedFinalDestination(this.position)) {
                    this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData, 0));
                } else {
                    this.sendAntToNextHop(ant.copy(ant.path, false, newDeliveriesData, ant.pathIndex));
                }
            }

        } else {
            this.sendAntToNextHop(ant);
        }
    }

    private void handleIntentionAnt(Message m, TimeLapse timeLapse) {
        IntentionAnt ant = (IntentionAnt) m.getContents();
        System.out.println("Intention ant at " + this.position + ": " + ant);

        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), true, ant.intentions, 0), ant.robot);
            } else {
                if (ant.toChargingStation) {
                    this.handleIntentionAntForChargingStation(timeLapse, ant);
                } else {
                    this.handleIntentionAntForDeliveryTask(timeLapse, ant);
                }
            }
        } else {
            this.sendAntToNextHop(ant);
        }
    }

    private void handleIntentionAntForDeliveryTask(TimeLapse timeLapse, IntentionAnt ant) {
        System.out.println("ResourceAgent.handleIntentionAntForDeliveryTask");
        // Get data of all DeliveryTasks on this position
        List<IntentionData> newDeliveriesData = new LinkedList<>();

        for (IntentionData intentionData : ant.intentions) {
            if (intentionData.position.equals(this.position) && intentionData.deliveryTaskID != null) {
                // Fetch the relevant DeliveryTask
                DeliveryTask task = this.deliveryTasks.get(intentionData.deliveryTaskID);
                System.out.println("task = " + task);
                // Check if a reservation can be updated or can be made (= if the task has pizzas to be delivered)
                if (task != null) {
                    boolean updated = this.updateDeliveryReservation(task, intentionData, timeLapse);

                    if (updated) {
                        // The reservation has been updated, set 'confirmed' to true in the delivery data.
                        newDeliveriesData.add(intentionData.copy(true));

                    } else {
                        System.out.println("Not updated, Pizzas: " + intentionData.pizzas + ", Pizzas left for task: " + this.getPizzasLeftForDeliveryTask(task.id) + " taskID: " +intentionData.deliveryTaskID + " pos: " + intentionData.position) ;
                        if (intentionData.pizzas <= this.getPizzasLeftForDeliveryTask(task.id) &&
                                this.getPizzasLeftForDeliveryTask(task.id) > 0) {

                            createReservation(timeLapse, intentionData, task);

                            // A reservation has been created, set 'confirmed' to true in the delivery data.
                            newDeliveriesData.add(intentionData.copy(true));

                        } else {
                            System.out.println("Denied ant = [" + ant + "]" );
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
        if (ant.hasReachedFinalDestination(this.position)) {
            this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData, 0));
        } else {
            this.sendAntToNextHop(ant.copy(ant.path, false, newDeliveriesData, ant.pathIndex));
        }
    }

    private void handleIntentionAntForChargingStation(TimeLapse timeLapse, IntentionAnt ant) {
        if (this.chargingStation.isPresent()) {
            List<IntentionData> newDeliveriesData = new LinkedList<>();
            if(ant.intentions.size() > 1){
                throw new IllegalStateException("Ant has more than 1 intention while going to chargingstation: " + ant);
            }

            IntentionData intentionData = ant.intentions.get(0);

            boolean update = updateChargingReservation(intentionData, timeLapse);
            if(update){
                newDeliveriesData.add(intentionData.copy(true));
            }else {
                // We have less reservations than the total capacity of the charging station
                System.out.println("this.chargingStationReservations.size() = " + this.chargingStationReservations.size());
                System.out.println("this.chargingStation.get().getCapacity() = " + this.chargingStation.get().getCapacity());

                if (this.chargingStationReservations.size() < this.chargingStation.get().getCapacity()) {
                    // Confirm the reservation
                    newDeliveriesData.add(intentionData.copy(true));

                    // Add new reservation to the list
                    ChargingStationReservation resv = new ChargingStationReservation(ant.robotID, timeLapse.getEndTime() + SimulatorSettings.INTENTION_RESERVATION_LIFETIME);
                    this.chargingStationReservations.add(resv);
                    System.out.println("Creating Reservation at charging station. " + this.chargingStationReservations.size() + "/ " +this.chargingStation.get().getCapacity()+ " resv in total.");
                } else {
                    newDeliveriesData.add(intentionData);
                }
            }

            System.out.println("ResourceAgent.handleIntentionAntForChargingStation: " + this.chargingStation.get());
            this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData, 0));
        } else {
            throw new IllegalStateException("Ant arrived at destination with `toChargingStation = true`," +
                    "but there is no charging station at destination");
        }
    }

    @NotNull
    private List<IntentionData> updateExplorationAntIntentionData(ExplorationAnt ant) {
        List<IntentionData> newDeliveriesData = new LinkedList<>();

        for (IntentionData intentionData : ant.intentions) {
            if (intentionData.position == this.position && intentionData.deliveryTaskID != null) {
                // Get the task with the correct deliveryTaskID
                DeliveryTask task = this.deliveryTasks.get(intentionData.deliveryTaskID);

                // Calculate the (possibly new) amount of pizzas that the robot can send for this task
                System.out.println("updateExplorationAntIntentionData for task " + task);
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
        long evaporationTimestamp = timeLapse.getEndTime() + SimulatorSettings.INTENTION_RESERVATION_LIFETIME;
        DeliveryTaskReservation reservation = new DeliveryTaskReservation(intentionData.robotID,
                task.id, intentionData.pizzas, evaporationTimestamp
        );

        System.out.println("Reservation " + task.id + ": " + intentionData + ", evaporation at: " + evaporationTimestamp);
        this.deliveryReservations.get(task.id).add(reservation);
    }

    private boolean updateDeliveryReservation(DeliveryTask task, IntentionData intentionData, TimeLapse timeLapse) {
        // If the reservation has already been made by the same robot, update it.
        List<DeliveryTaskReservation> reservations = this.deliveryReservations.get(task.id).stream()
                .filter(r -> r.deliveryTaskID == intentionData.deliveryTaskID && r.robotID == intentionData.robotID)
                .collect(Collectors.toList());

        if (reservations.size() > 0) {
            // Update the timer.
            DeliveryTaskReservation r = reservations.get(0);
            DeliveryTaskReservation new_reservation = r.copy(timeLapse.getEndTime() + SimulatorSettings.INTENTION_RESERVATION_LIFETIME);

            this.deliveryReservations.get(task.id).remove(r);
            this.deliveryReservations.get(task.id).add(new_reservation);
            return true;
        }

        return false;
    }

    private boolean updateChargingReservation(IntentionData intentionData, TimeLapse timeLapse){
        List<ChargingStationReservation> reservations = this.chargingStationReservations.stream()
                .filter(r -> r.robotID == intentionData.robotID)
                .collect(Collectors.toList());

        if (reservations.size() > 0) {
            // Update the timer.
            ChargingStationReservation r = reservations.get(0);
            ChargingStationReservation new_reservation = r.copy(timeLapse.getEndTime() + SimulatorSettings.INTENTION_RESERVATION_LIFETIME);

            this.chargingStationReservations.remove(r);
            this.chargingStationReservations.add(new_reservation);
            return true;
        }

        return false;
    }

    private void sendAntToNextHop(Ant ant) {
        System.out.println("ResourceAgent.sendAntToNextHop");

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
            estimatedTime++;
            // TODO: calculate new estimated time based on this_location -> next_location
        }

        int nextPositionIndex = ant.pathIndex + 1;
        Point nextPosition = ant.path.get(nextPositionIndex);

        boolean sendOutAnt = false;
        for (ResourceAgent neighbor : this.neighbors) {
            if (neighbor.getPosition().get().equals(nextPosition)) {
                if (neighbor.hasRoadWorks()) {
                    estimatedTime += SimulatorSettings.TIME_ROAD_WORKS;
                }
                ant = ant.copy(estimatedTime, nextPositionIndex);
                sendOutAnt = true;
                this.commDevice.send(ant, neighbor);
            }
        }

        if(!sendOutAnt){
            throw new IllegalStateException("Ant didn't get send: " + ant);
        }
    }

    public void dropReservation(RobotAgent agent) {
        this.chargingStationReservations.removeIf(r -> r.robotID == agent.id);
    }
}
