package mas.tasks;

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.jetbrains.annotations.NotNull;


public class DeliveryTask implements RoadUser, CommUser {

    private RoadModel roadModel;
    private CommDevice commDevice;

    // the time at which the DeliveryTask was created
    public final long start_time;
    private Point position;
    private int pizzasRequested;
    private int pizzasReady;
    private int pizzasDelivered;
    private int deliveryID;
    private static int idCounter = 0;

    public DeliveryTask(Point position, int pizzasRequested, long time) {
        this.position = position;
        this.pizzasRequested = pizzasRequested;
        this.pizzasDelivered = 0;
        this.start_time = time;
        this.deliveryID = this.getNextID();
    }

    private int getNextID(){
        return idCounter++;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = model;

        this.roadModel.addObjectAt(this, position);
    }

    public Optional<Point> getPosition() {
        return Optional.of(this.position);
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        commDevice = builder.build();
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

    public int getDeliveryID() {
        return deliveryID;
    }
}
