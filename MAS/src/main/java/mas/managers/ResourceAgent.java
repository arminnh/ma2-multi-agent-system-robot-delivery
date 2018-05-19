package mas.managers;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import mas.ants.ExplorationAnt;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.LinkedList;
import java.util.List;

public class ResourceAgent implements CommUser, RoadUser, TickListener {

    Optional<CommDevice> device;
    private RoadModel roadModel;
    private Point position;
    private boolean first_tick = true;
    private List<CommUser> neighbors;
    private RandomGenerator rng;


    public ResourceAgent(Point position, RandomGenerator rng) {
        this.position = position;
        this.device = Optional.absent();
        neighbors = new LinkedList<>();
        this.rng = rng;
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }

    @Override
    public void setCommDevice(CommDeviceBuilder builder) {
        builder.setMaxRange(2);
        this.device = Optional.of(builder.build());
    }

    @Override
    public void initRoadUser(RoadModel model) {
        roadModel = model;
        roadModel.addObjectAt(this, this.position);
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (first_tick) {
            first_tick = false;
            this.device.get().broadcast(Messages.NICE_TO_MEET_YOU);
        }

        readMessages();
    }

    private void readMessages() {
        for (Message m : this.device.get().getUnreadMessages()) {
            if (m.getContents() == Messages.NICE_TO_MEET_YOU) {
                neighbors.add(m.getSender());
            } else if (m.getContents().getClass() == ExplorationAnt.class) {
                ExplorationAntLogic(m);
            }
        }
    }

    private void ExplorationAntLogic(Message m) {
        ExplorationAnt ant = (ExplorationAnt) m.getContents();

        System.out.println("Exploration ant at " + this.position);

        // Check if the ant reached its current destination. Once the ant reached the original destination, the
        // destination and path are reversed towards the Robot that sent the ant.
        if (ant.hasReachedDestination(this.position)) {
            if (ant.isReturning) {
                // The ant reached the Robot.
                this.device.get().send(ant.copy(Lists.reverse(ant.path), null, null), ant.robot);

            } else {
                // The ant reached the task's delivery location.
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

    private void sendAntToNextHop(ExplorationAnt ant) {
        if (ant.path.size() == 0) {
            System.out.println("CANNOT SEND ANT TO NEXT HOP FOR EMPTY PATH");
        }

        int nextPositionIndex = ant.path.indexOf(this.position) + 1;
        Point nextPosition = ant.path.get(nextPositionIndex);

        for (CommUser neighbor: this.neighbors) {
            if (neighbor.getPosition().get().equals(nextPosition)) {
                this.device.get().send(ant, neighbor);
            }
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
    }

    enum Messages implements MessageContents {
        NICE_TO_MEET_YOU
    }
}
