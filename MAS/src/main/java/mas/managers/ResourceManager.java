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

import java.util.LinkedList;
import java.util.List;

public class ResourceManager implements CommUser, RoadUser, TickListener {

    private RoadModel roadModel;
    private Point position;
    private boolean first_tick = true;
    Optional<CommDevice> device;
    private List<CommUser> neighbors;

    public ResourceManager(Point position){
        this.position = position;
        this.device = Optional.absent();
        neighbors = new LinkedList<>();
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
        if(first_tick){
            first_tick = false;
            this.device.get().broadcast(Messages.NICE_TO_MEET_YOU);
        }

        for(Message m: this.device.get().getUnreadMessages()){
            if(m.getContents() == Messages.NICE_TO_MEET_YOU) {
                neighbors.add(m.getSender());
            }else if(m.getContents().getClass() == ExplorationAnt.class){
                ExplorationAnt ant = (ExplorationAnt) m.getContents();
                // check if current position is destination
                // send message back
                if(ant.hasReachedDestination()){
                    List<Point> return_path = ant.getReturning_path().subList(1, ant.getReturning_path().size());
                    ExplorationAnt new_ant = new ExplorationAnt(ant.getPath(), ant.getRobot_id(), ant.getDestination(), return_path);
                    for(CommUser neighbor: neighbors) {
                        if(neighbor.getPosition().get() == return_path.get(0)){
                            this.device.get().send(new_ant, neighbor);
                        }
                    }
                }else{
                    List<Point> new_path = new LinkedList<>(ant.getPath());
                    new_path.add(this.getPosition().get());

                    if(this.getPosition().get() == ant.getDestination()){

                        List<Point> return_path = new LinkedList<>(ant.getPath());
                        return_path = Lists.reverse(return_path);
                        ExplorationAnt new_ant = new ExplorationAnt(new_path, ant.getRobot_id(), ant.getDestination(), return_path);

                        this.device.get().send(new_ant, m.getSender());

                    }else{
                        // destination not reached yet
                        for(CommUser neighbor: neighbors){
                            // Check if our neighbor is already visited
                            if(!ant.getPath().contains(neighbor)){
                                ExplorationAnt new_ant = new ExplorationAnt(new_path, ant.getRobot_id(), ant.getDestination());
                                this.device.get().send(new_ant,neighbor);
                            }
                        }
                    }
                }

            }
        }


    }

    @Override
    public void afterTick(TimeLapse timeLapse) { }

    enum Messages implements MessageContents {
        NICE_TO_MEET_YOU
    }
}
