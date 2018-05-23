package mas;


import com.github.rinde.rinsim.geom.Point;

public class DeliveryTaskData {
    public final Point position;
    public final Integer robotID;
    public final Integer deliveryTaskID;
    public final Integer pizzas;
    public final boolean reservationConfirmed;

    public DeliveryTaskData(Point position, Integer robotID, Integer deliveryTaskID, Integer pizzas, boolean confirmed) {
        this.position = position;
        this.robotID = robotID;
        this.deliveryTaskID = deliveryTaskID;
        this.pizzas = pizzas;
        this.reservationConfirmed = confirmed;
    }

    public DeliveryTaskData copy(boolean confirmed) {
        return new DeliveryTaskData(this.position, this.robotID, this.deliveryTaskID, this.pizzas, confirmed);
    }
}
