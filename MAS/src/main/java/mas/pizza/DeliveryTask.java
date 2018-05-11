package mas.pizza;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.jetbrains.annotations.NotNull;


public class DeliveryTask implements RoadUser, CommUser {

    private Optional<RoadModel> roadModel;
    private Optional<CommDevice> comm;

    // the time at which the DeliveryTask was created
    public final long start_time;
    private Point position;
    private int pizzaAmount;
    private int pizzasReady;
    private int pizzasDelivered;


    public DeliveryTask(Point position, int pizzaAmount, long time) {
        this.position = position;
        this.pizzaAmount = pizzaAmount;
        this.pizzasDelivered = 0;
        this.start_time = time;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = Optional.of(model);

        model.addObjectAt(this, position);
    }

    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }

    public int getPizzaAmount() {
        return this.pizzaAmount;
    }

    public int getPizzasReady() {
        return this.pizzasReady;
    }

    public int getPizzasDelivered() {
        return this.pizzasDelivered;
    }

    public boolean isFinished() {
        return this.pizzasDelivered == this.pizzaAmount;
    }

    public int getPizzasLeft() {
        return this.pizzaAmount - this.pizzasReady;
    }

    public boolean receivedAllPizzas() {
        return this.pizzasDelivered == this.pizzaAmount;
    }

    public void addReadyPizzas(int amount) {
        this.pizzasReady += amount;
    }

    public void deliverPizzas(int amount) {
        this.pizzasDelivered += amount;
        System.out.println("delivered pizzas: " + amount + ". Requested: " + pizzaAmount);
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        comm = Optional.of(builder.build());
    }

}
