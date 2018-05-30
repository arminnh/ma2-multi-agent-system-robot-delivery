package mas.agents;

public class DeliveryTaskReservation {
    public final int robotID;
    public final int deliveryTaskID;
    public final int pizzaAmount;
    public final long evaporationTimestamp;

    public DeliveryTaskReservation(int robotID, int deliveryTaskID, int pizzaAmount, long evaporationTimestamp) {
        this.robotID = robotID;
        this.deliveryTaskID = deliveryTaskID;
        this.pizzaAmount = pizzaAmount;
        this.evaporationTimestamp = evaporationTimestamp;
    }

    public DeliveryTaskReservation copy(long evaporationTimestamp) {
        return new DeliveryTaskReservation(this.robotID, this.deliveryTaskID, this.pizzaAmount, evaporationTimestamp);
    }

    @Override
    public String toString() {
        return "DeliveryTaskReservation{robotID: " + this.robotID + ", pizzaAmount: " + this.pizzaAmount + "}";
    }
}
