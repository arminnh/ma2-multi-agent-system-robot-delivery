package mas;


import com.github.rinde.rinsim.geom.Point;

public class IntentionData {
    public final Point position;
    public final Integer robotID;
    public final Integer deliveryTaskID;
    public final Integer pizzas;
    public final boolean reservationConfirmed;

    public IntentionData(Point position, Integer robotID, Integer deliveryTaskID, Integer pizzas, boolean confirmed) {
        this.position = position;
        this.robotID = robotID;
        this.deliveryTaskID = deliveryTaskID;
        this.pizzas = pizzas;
        this.reservationConfirmed = confirmed;
    }

    public IntentionData copy(boolean confirmed) {
        return new IntentionData(this.position, this.robotID, this.deliveryTaskID, this.pizzas, confirmed);
    }
}
