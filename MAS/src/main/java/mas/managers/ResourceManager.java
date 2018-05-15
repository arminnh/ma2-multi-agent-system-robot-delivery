package mas.managers;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.LinkedList;
import java.util.List;

public class ResourceManager implements CommUser, RoadUser, TickListener {

    private RoadModel roadModel;
    private Point position;
    private boolean first_tick = true;
    Optional<CommDevice> device;
    private List<Point> neighbors;

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
            if(m.getContents() == Messages.NICE_TO_MEET_YOU){
                neighbors.add(m.getSender().getPosition().get());
            }
        }


    }

    @Override
    public void afterTick(TimeLapse timeLapse) { }

    enum Messages implements MessageContents {
        NICE_TO_MEET_YOU
    }
}
