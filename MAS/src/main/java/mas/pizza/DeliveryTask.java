package mas.pizza;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.jetbrains.annotations.NotNull;


public class DeliveryTask implements RoadUser, CommUser {

    // the time at which the DeliveryTask was created
    public final long start_time;
    private Optional<RoadModel> roadModel;
    private Optional<CommDevice> comm;
    private Point position;
    private int pizzasRequested;
    private int pizzasReady;
    private int pizzasDelivered;


    public DeliveryTask(Point position, int pizzasRequested, long time) {
        this.position = position;
        this.pizzasRequested = pizzasRequested;
        this.pizzasDelivered = 0;
        this.start_time = time;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = Optional.of(model);

        this.roadModel.get().addObjectAt(this, position);
    }

    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        comm = Optional.of(builder.build());
    }

    public int getPizzasRequested() {
        return this.pizzasRequested;
    }

    public int getPizzasReady() {
        return this.pizzasReady;
    }

    public int getPizzasDelivered() {
        return this.pizzasDelivered;
    }

    public int getPizzasLeft() {
        return this.pizzasRequested - this.pizzasReady;
    }

    public boolean isFinished() {
        return this.pizzasDelivered == this.pizzasRequested;
    }

    public void addReadyPizzas(int amount) {
        this.pizzasReady += amount;
    }

    public void deliverPizzas(int amount) {
        this.pizzasDelivered += amount;
    }

    public long getWaitingTime(long currentTime) {
        return currentTime - start_time;
    }
}
