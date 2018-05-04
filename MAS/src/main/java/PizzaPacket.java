import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class PizzaPacket extends Parcel implements CommUser, TickListener, RoadUser {

    private Optional<CommDevice> comm;
    private final double range;
    static final double MIN_RANGE = 0.5;
    private RoadModel roadModel;
    private PDPModel pdpModel;

    PizzaPacket(ParcelDTO dto) {
        super(dto);
        range = MIN_RANGE;
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        if (range >= 0) {
            builder.setMaxRange(range);
        }
        comm = Optional.of(builder.build());
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.of(super.getPickupLocation());
    }

    public void tick(@NotNull TimeLapse timeLapse) {
        
    }

    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    public void dissableComm(){
        comm = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.roadModel = pRoadModel;
        this.pdpModel = pPdpModel;
    }
}
