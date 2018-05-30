package mas.messages;


import com.github.rinde.rinsim.geom.Point;

public class IntentionData {
    public final Point position;
    public final int robotID;
    public final int deliveryTaskID;
    public final int pizzas;
    public final boolean reservationConfirmed;

    public IntentionData(Point position, int robotID, int deliveryTaskID, int pizzas, boolean confirmed) {
        if (position == null) {
            throw new IllegalArgumentException("IntentionData's position cannot be null.");
        }

        this.position = position;
        this.robotID = robotID;
        this.deliveryTaskID = deliveryTaskID;
        this.pizzas = pizzas;
        this.reservationConfirmed = confirmed;
    }

    public IntentionData copy(boolean confirmed) {
        return new IntentionData(this.position, this.robotID, this.deliveryTaskID, this.pizzas, confirmed);
    }

    @Override
    public String toString() {
        return "{destination: " + this.position +
                ", robotID: " + this.robotID +
                ", deliveryTaskID: " + this.deliveryTaskID +
                ", pizzas: " + this.pizzas +
                ", reservationConfirmed: " + this.reservationConfirmed +
                "}";
    }
}
