package mas.agents;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import mas.buildings.ChargingStation;
import mas.messages.*;
import mas.tasks.DeliveryTask;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ResourceAgent implements CommUser, RoadUser, TickListener {

    private RoadModel roadModel;
    private RandomGenerator rng;
    private CommDevice commDevice;

    private Point position;
    private boolean first_tick = true;
    private List<CommUser> neighbors = new LinkedList<>();


    public ResourceAgent(Point position, RandomGenerator rng) {
        this.position = position;
        this.rng = rng;
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(this.position);
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
    }

    @Override
    public void tick(@NotNull TimeLapse timeLapse) {
        if (first_tick) {
            first_tick = false;
            this.commDevice.broadcast(Messages.NICE_TO_MEET_YOU);
        }

        readMessages();
    }

    private void readMessages() {
        for (Message m : this.commDevice.getUnreadMessages()) {
            if (m.getContents() == Messages.NICE_TO_MEET_YOU) {
                neighbors.add(m.getSender());
            } else if (m.getContents().getClass() == ExplorationAnt.class) {
                handleExplorationAnt(m);
            } else if (m.getContents().getClass() == IntentionAnt.class) {
                handleIntentionAnt(m);
            } else if (m.getContents().getClass() == DesireAnt.class) {
                handleDesireAnt(m);
            }
        }
    }

    private void handleDesireAnt(Message m) {
        DesireAnt ant = (DesireAnt) m.getContents();
        System.out.println("Desire ant at " + this.position);

        /*
        Desire ant has:
        - list path
        - long estimatedTime
        - boolean isReturning,
        - int id
        - Integer robotID
        - CommUser robot
        - int score
        - int deliveryID,
        - int capacity
         */

        if(ant.hasReachedDestination(this.position)){
            // Get delivery parcel
            if(!ant.isReturning){
                // We are looking for deliveryTasks
                Set<DeliveryTask> deliveryTasks = this.roadModel.getObjectsAt(this, DeliveryTask.class);

                
            }
            /*
            if(deliveryTasks.size() > 0){
                for(DeliveryTask task: deliveryTasks){
                    // Find the task with the right id
                    if(task.getDeliveryID() == ant.deliveryID){
                        // Check if the task has resources left

                    }
                }
            }else{
                // Get charging station
                Set<ChargingStation> chargingStation = this.roadModel.getObjectsAt(this, ChargingStation.class);
            }*/
        }else{
            sendAntToNextHop(ant);
        }
    }

    private void handleIntentionAnt(Message m) {
        IntentionAnt ant = (IntentionAnt) m.getContents();
        System.out.println("Intention ant at " + this.position);

        if(ant.hasReachedDestination(this.position)){
            // Get delivery parcel
            Set<DeliveryTask> deliveryTasks = this.roadModel.getObjectsAt(this, DeliveryTask.class);
            if(deliveryTasks.size() > 0){
                for(DeliveryTask task: deliveryTasks){
                    // Find the task with the right id
                    if(task.getDeliveryID() == ant.deliveryID){
                        // Check if the task has resources left

                    }
                }
            }else{
                // Get charging station
                Set<ChargingStation> chargingStation = this.roadModel.getObjectsAt(this, ChargingStation.class);
            }

        }else{
            sendAntToNextHop(ant);
        }

    }

    private void handleExplorationAnt(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();

        System.out.println("Exploration ant at " + this.position);

        // Check if the ant reached its current destination. Once the ant reached the original destination, the
        // destination and path are reversed towards the RobotAgent that sent the ant.
        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                // The ant reached the RobotAgent.
                this.commDevice.send(ant.copy(Lists.reverse(ant.path), null, null), ant.robot);

            } else {
                // The ant reached the deliveryTask's delivery location.
                sendAntToNextHop(ant.copy(Lists.reverse(ant.path), null, true));
            }

        } else {
            long estimatedTime = ant.estimatedTime;
            if (!ant.isReturning) {
                estimatedTime++;
                // TODO: calculate new estimated time based on this_location -> next_location
            }

            sendAntToNextHop(ant.copy(null, estimatedTime, null));
        }
    }

    private void sendAntToNextHop(Ant ant) {
        if (ant.path.size() == 0) {
            System.out.println("CANNOT SEND ANT TO NEXT HOP FOR EMPTY PATH");
        }

        int nextPositionIndex = ant.path.indexOf(this.position) + 1;
        Point nextPosition = ant.path.get(nextPositionIndex);

        for (CommUser neighbor: this.neighbors) {
            if (neighbor.getPosition().get().equals(nextPosition)) {
                this.commDevice.send(ant, neighbor);
            }
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }
}
