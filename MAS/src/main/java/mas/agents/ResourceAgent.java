package mas.agents;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import mas.IntentionData;
import mas.SimulatorSettings;
import mas.buildings.ChargingStation;
import mas.buildings.RoadWorks;
import mas.messages.*;
import mas.tasks.DeliveryTask;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ResourceAgent implements CommUser, RoadUser, TickListener {

    private RoadModel roadModel;
    private RandomGenerator rng;
    private CommDevice commDevice;

    public final Point position;
    private boolean first_tick = true;
    private List<ResourceAgent> neighbors = new LinkedList<>();
    private Optional<RoadWorks> roadWorks = Optional.absent();
    private Optional<ChargingStation> chargingStation = Optional.absent();
    private HashMap<Integer, DeliveryTask> deliveryTasks = new HashMap<>();
    private HashMap<Integer, List<DeliveryTaskReservation>> reservations = new HashMap<>();

    public ResourceAgent(Point position, RandomGenerator rng) {
        this.position = position;
        this.rng = rng;
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(2);
        this.commDevice = builder.build();
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        roadModel = model;
        roadModel.addObjectAt(this, this.position);

        Set<ChargingStation> stations = roadModel.getObjectsAt(this, ChargingStation.class);
        if (stations.size() > 0) {
            this.chargingStation = Optional.of(stations.iterator().next());
        }
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }

    public List<ResourceAgent> getNeighbors() {
        return this.neighbors;
    }

    @Override
    public void tick(@NotNull TimeLapse timeLapse) {
        if (first_tick) {
            first_tick = false;
            this.commDevice.broadcast(Messages.NICE_TO_MEET_YOU);
        }

        readMessages(timeLapse);

        evaporateReservations(timeLapse);
    }

    private void evaporateReservations(@NotNull TimeLapse timeLapse) {
        // evaporates reservations
        //System.out.println("It is now: " + timeLapse.getStartTime());
        for(Integer k1: this.reservations.keySet()){
            //System.out.println("Resv for " + k1 + " is " + this.reservations.get(k1).size());
            int oldSize = this.reservations.get(k1).size();
            this.reservations.get(k1).removeIf(r -> r.evaporationTimestamp < timeLapse.getStartTime());
            //System.out.println("Updated Resv for " + k1 + " is " + this.reservations.get(k1).size());
            if(oldSize > this.reservations.get(k1).size()){
                System.out.println("Evaporation!");
            }
        }
    }

    private void readMessages(TimeLapse timeLapse) {
        for (Message m : this.commDevice.getUnreadMessages()) {
            if (m.getContents() == Messages.NICE_TO_MEET_YOU) {
                neighbors.add((ResourceAgent) m.getSender());
            } else if (m.getContents().getClass() == ExplorationAnt.class) {
                handleExplorationAnt(m);
            } else if (m.getContents().getClass() == IntentionAnt.class) {
                handleIntentionAnt(m, timeLapse);
            } else if (m.getContents().getClass() == DesireAnt.class) {
                handleDesireAnt(m);
            }
        }
    }

    private void handleDesireAnt(Message m) {
        DesireAnt ant = (DesireAnt) m.getContents();
        System.out.println("Desire ant at " + this.position + " id: " + ant.id);

        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), true, null, null), ant.robot);
            } else {
                DeliveryTask task = this.deliveryTasks.get(ant.deliveryTaskID);

                // Calculate distance
                int amount = 0;
                Long score = 0L;
                if(task != null){
                    amount = this.getPizzasLeftForDeliveryTask(task.id);
                    score = task.getScore();
                }

                DesireAnt newAnt = ant.copy(Lists.reverse(ant.path),
                        true, score, amount);
                sendAntToNextHop(newAnt);
            }
        } else {
            sendAntToNextHop(ant);
        }
    }

    private void handleExplorationAnt(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();
        System.out.println("Exploration ant at " + this.position + " id: " + ant.id);

        // Check if the ant reached its current destination. Once the ant reached the original destination, the
        // destination and path are reversed towards the RobotAgent that sent the ant.
        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                // The ant reached the RobotAgent.
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), null, null), ant.robot);
            } else {
                // Create new list of IntentionData for next ant
                List<IntentionData> newDeliveriesData = updateExplorationAntDeliveryData(ant);

                if (ant.hasReachedFinalDestination(this.position)) {
                    sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData));
                } else {
                    sendAntToNextHop(ant.copy(ant.path, false, newDeliveriesData));
                }
            }

        } else {
            sendAntToNextHop(ant);
        }
    }

    private void handleIntentionAnt(Message m, TimeLapse timeLapse) {
        IntentionAnt ant = (IntentionAnt) m.getContents();
        System.out.println("Intention ant at " + this.position + " id: " + ant.id + " deliveries "+ ant.deliveries);

        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                System.out.println("ant.isReturning = " + ant.isReturning);
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), true, ant.deliveries), ant.robot);
            } else {
                if (ant.toChargingStation) {
                    System.out.println("ant.toChargingStation = " + ant.toChargingStation);

                    handleIntentionAntForChargingStation(ant);
                } else {
                    System.out.println("delivery");

                    handleIntentionAntForDeliveryTask(timeLapse, ant);
                }
            }
        } else {
            sendAntToNextHop(ant);
        }
    }

    private void handleIntentionAntForDeliveryTask(TimeLapse timeLapse, IntentionAnt ant) {
        System.out.println("ResourceAgent.handleIntentionAntForDeliveryTask");
        // Get data of all DeliveryTasks on this position
        List<IntentionData> newDeliveriesData = new LinkedList<>();

        for (IntentionData deliveryData : ant.deliveries) {
            if (deliveryData.position.equals(this.position) && deliveryData.deliveryTaskID != null) {
                // Fetch the relevant DeliveryTask
                DeliveryTask task = this.deliveryTasks.get(deliveryData.deliveryTaskID);

                // Check if a reservation can be updated or can be made (= if the task has pizzas to be delivered)
                if(task != null){
                    boolean updated = this.updateReservation(task, deliveryData, timeLapse);

                    if (updated) {
                        // The reservation has been updated, set 'confirmed' to true in the delivery data.
                        newDeliveriesData.add(deliveryData.copy(true));

                    } else {
                        if(deliveryData.pizzas <= this.getPizzasLeftForDeliveryTask(task.id) &&
                                this.getPizzasLeftForDeliveryTask(task.id) > 0) {

                            createReservation(timeLapse, deliveryData, task);

                            // A reservation has been created, set 'confirmed' to true in the delivery data.
                            newDeliveriesData.add(deliveryData.copy(true));

                        } else {
                            // A reservation could not be created, set 'confirmed' to true in the delivery data.
                            newDeliveriesData.add(deliveryData.copy(false));
                        }
                    }
                }else{
                    // Task has already been handled by other robot.
                    newDeliveriesData.add(deliveryData.copy(false));
                }
            } else {
                // Nothing to be done for this delivery data.
                newDeliveriesData.add(deliveryData);
            }
        }

        // Send the ants
        if (ant.hasReachedFinalDestination(this.position)) {
            this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, newDeliveriesData));
        } else {
            this.sendAntToNextHop(ant.copy(ant.path, false, newDeliveriesData));
        }
    }

    private void handleIntentionAntForChargingStation(IntentionAnt ant) {
        if (this.chargingStation.isPresent()) {
            System.out.println(this.chargingStation.get());
            this.sendAntToNextHop(ant.copy(Lists.reverse(ant.path), true, ant.deliveries));
            // TODO: charging logic
        } else {
            System.err.println("ANT ARRIVED AT DESTINATION WHILE GOING TO CHARGING STATION, BUT THERE WAS NO CHARGING STATION");
        }
    }

    @NotNull
    private List<IntentionData> updateExplorationAntDeliveryData(ExplorationAnt ant) {
        List<IntentionData> newDeliveriesData = new LinkedList<>();

        for (IntentionData deliveryData : ant.deliveries) {
            if (deliveryData.position == this.position && deliveryData.deliveryTaskID != null) {
                // Get the task with the correct deliveryTaskID
                DeliveryTask task = this.deliveryTasks.get(deliveryData.deliveryTaskID);

                // Calculate the (possibly new) amount of pizzas that the robot can send for this task
                System.out.println("updateExplorationAntDeliveryData: " + task + " " + deliveryData);
                Integer newPizzaAmount = 0;

                if(this.deliveryTasks.containsKey(deliveryData.deliveryTaskID)){
                    newPizzaAmount = Math.min(this.getPizzasLeftForDeliveryTask(task.id), deliveryData.pizzas);
                }

                newDeliveriesData.add(new IntentionData(deliveryData.position, deliveryData.robotID,
                        deliveryData.deliveryTaskID, newPizzaAmount, false));

            } else {
                // Nothing to be done for this delivery data.
                newDeliveriesData.add(deliveryData);
            }
        }

        return newDeliveriesData;
    }

    private void createReservation(TimeLapse timeLapse, IntentionData deliveryData, DeliveryTask task) {
        // Make the reservation and send the ant back to confirm.
        long evaporationTimestamp = timeLapse.getEndTime() + SimulatorSettings.INTENTION_RESERVATION_LIFETIME;
        DeliveryTaskReservation reservation = new DeliveryTaskReservation(deliveryData.robotID,
                task.id, deliveryData.pizzas, evaporationTimestamp
        );

        System.out.println("Reservation " + task.id + " "+  deliveryData.robotID+" "+ deliveryData.pizzas + " " + evaporationTimestamp);
        this.reservations.get(task.id).add(reservation);
    }

    private boolean updateReservation(DeliveryTask task, IntentionData deliveryData, TimeLapse timeLapse) {
        // If the reservation has already been made by the same robot, update it.
        List<DeliveryTaskReservation> reservations = this.reservations.get(task.id).stream()
                .filter(r -> r.deliveryTaskID == deliveryData.deliveryTaskID && r.robotID == deliveryData.robotID)
                .collect(Collectors.toList());

        if (reservations.size() > 0) {
            // Update the timer.
            DeliveryTaskReservation r = reservations.get(0);
            DeliveryTaskReservation new_reservation = r.copy(timeLapse.getEndTime() + SimulatorSettings.INTENTION_RESERVATION_LIFETIME);

            this.reservations.get(task.id).remove(r);
            this.reservations.get(task.id).add(new_reservation);
            return true;
        }

        return false;
    }

    private void sendAntToNextHop(Ant ant) {
        if (ant.path.size() == 0) {
            System.out.println("CANNOT SEND ANT TO NEXT HOP FOR EMPTY PATH");
        }
        if(ant.path.size() == 1){
            this.commDevice.send(ant, ant.robot);
            return;
        }

        long estimatedTime = ant.estimatedTime;
        if (!ant.isReturning) {
            estimatedTime++;
            // TODO: calculate new estimated time based on this_location -> next_location
        }
        ant = ant.copy(estimatedTime);
        System.out.println("ant.path = " + ant.path);
        int nextPositionIndex = ant.path.indexOf(this.position) + 1;
        Point nextPosition = ant.path.get(nextPositionIndex);

        for (ResourceAgent neighbor : this.neighbors) {
            if (neighbor.getPosition().get().equals(nextPosition)) {
                this.commDevice.send(ant, neighbor);
            }
        }
    }

    private int getPizzasLeftForDeliveryTask(Integer deliveryTaskID) {
        DeliveryTask task = this.deliveryTasks.get(deliveryTaskID);
        List<DeliveryTaskReservation> reservations = this.reservations.get(deliveryTaskID);

        int pizzasInReservations = 0;
        for (DeliveryTaskReservation r : reservations) {
            pizzasInReservations += r.pizzaAmount;
        }

        return task.getPizzasRemaining() - pizzasInReservations;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    public void addDeliveryTask(DeliveryTask task) {
        this.deliveryTasks.put(task.id, task);
        this.reservations.put(task.id, new LinkedList<>());
    }

    public void removeDeliveryTask(DeliveryTask task) {
        this.deliveryTasks.remove(task.id);
        this.reservations.remove(task.id);
    }

    public void setRoadWorks(RoadWorks roadWorks) {
        this.roadWorks = Optional.of(roadWorks);
    }

    public void removeRoadWorks() {
        this.roadWorks = Optional.absent();
    }

    public Optional<RoadWorks> getRoadWorks() {
        return roadWorks;
    }
}
