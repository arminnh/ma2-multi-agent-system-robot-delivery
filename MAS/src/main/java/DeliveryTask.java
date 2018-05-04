import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.jetbrains.annotations.NotNull;

import java.sql.Time;
import java.util.List;

class DeliveryTask extends Depot implements CommUser, TickListener, RoadUser {

    private Optional<CommDevice> comm;
    static final double MIN_RANGE = 0.5;
    static Point location;


    DeliveryTask(Point startPosition, int capacity) {
        super(startPosition);
        setCapacity(capacity);
        this.location = startPosition;
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        comm = Optional.of(builder.build());
    }

    public void tick(TimeLapse t){}

    public void afterTick(TimeLapse t){}

    public Optional<Point> getPosition() {
        return Optional.of(this.location);
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {}

}
