package mas.tasks;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.geom.Point;
import org.jetbrains.annotations.NotNull;


public class DeliveryTask implements RoadUser {

    private static int idCounter = 1;
    // the time at which the DeliveryTask was created
    public final long startTime;
    public final Point position;
    public final int id;
    private int pizzasRequested;
    private int pizzasDelivered;
    private Clock clock;

    public DeliveryTask(Point position, int pizzasRequested, long time, Clock clock) {
        this.position = position;
        this.pizzasRequested = pizzasRequested;
        this.pizzasDelivered = 0;
        this.startTime = time;
        this.id = this.getNextID();
        this.clock = clock;
    }

    private int getNextID() {
        return idCounter++;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel roadModel) {
        roadModel.addObjectAt(this, position);
    }

    @Override
    public String toString() {
        return "{id: " + this.id +
                ", destination: " + this.position +
                ", requested: " + this.pizzasRequested +
                ", delivered: " + this.pizzasDelivered +
                "}";
    }

    public int getPizzasRequested() {
        return this.pizzasRequested;
    }

    public int getPizzasRemaining() {
        return this.pizzasRequested - this.pizzasDelivered;
    }

    public boolean isFinished() {
        return this.pizzasDelivered == this.pizzasRequested;
    }

    public void deliverPizzas(int amount) {
        this.pizzasDelivered += amount;
    }

    public long getWaitingTime(long currentTime) {
        return currentTime - startTime;
    }

    public Long getScore() {
        // TODO: implement a good scoring function
        // TODO: implement a good scoring function
        // TODO: implement a good scoring function
        // TODO: implement a good scoring function
        // TODO: implement a good scoring function
        // TODO: implement a good scoring function
        // Suggestions:
        // - Use waiting time (OK)
        // - Estimated delivery time
        return getWaitingTime(clock.getCurrentTime());
    }
}
