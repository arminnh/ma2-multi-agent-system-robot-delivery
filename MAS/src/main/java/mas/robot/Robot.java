package mas.robot;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of a very simple delivery robot.
 */
public class Robot extends Vehicle implements MovingRoadUser, TickListener, RandomUser, CommUser {

    private Battery battery;
    private int id;
    private Optional<Parcel> curr;

    private Optional<RandomGenerator> rnd;
    private Optional<CommDevice> comm;
    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;


    public Robot(VehicleDTO vdto, Battery battery, int id) {
        super(vdto);

        this.battery = battery;
        this.id = id;
        curr = Optional.absent();
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.roadModel = Optional.of(pRoadModel);
        this.pdpModel = Optional.of(pPdpModel);
    }


    @Override
    public Optional<Point> getPosition() {
        if (roadModel.isPresent()) {
            return Optional.of(roadModel.get().getPosition(this));
        }
        return Optional.absent();
    }


    @Override
    public void setRandomGenerator(@NotNull RandomProvider provider) {
        rnd = Optional.of(provider.newInstance());
    }


    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(0.5);
        comm = Optional.of(builder.build());
    }


    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        if (!curr.isPresent()) {
            curr = Optional.fromNullable(RoadModels.findClosestObject(
                roadModel.get().getPosition(this),
                roadModel.get(),
                Parcel.class
            ));
        }

    }


    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) { }
}