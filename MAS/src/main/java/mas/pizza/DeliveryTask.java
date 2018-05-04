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

    private Point position;
    private int pizzaAmount;

    private Optional<RoadModel> roadModel;
    private Optional<CommDevice> comm;


    public DeliveryTask(Point position, int pizzaAmount) {
        this.position = position;
        this.pizzaAmount = pizzaAmount;
    }

    @Override
    public void initRoadUser(RoadModel model) {
        this.roadModel = Optional.of(model);

        model.addObjectAt(this, position);
    }


    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }


    public int getPizzaAmount() {
        return this.pizzaAmount;
    }


    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        comm = Optional.of(builder.build());
    }
}