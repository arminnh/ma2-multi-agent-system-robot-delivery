package mas.tasks;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;

public class PizzaParcel extends Parcel {

    public final DeliveryTask deliveryTask;
    public final int amountOfPizzas;
    // the time at which the PizzaParcel was created
    public final long startTime;
    public int deliveryTaskID;

    public PizzaParcel(ParcelDTO parcelDto, DeliveryTask deliveryTask, int pizzaAmount, long time) {
        super(parcelDto);

        this.deliveryTask = deliveryTask;
        this.amountOfPizzas = pizzaAmount;
        this.startTime = time;
        this.deliveryTaskID = deliveryTask.id;
    }

    @Override
    public String toString() {
        return "PizzaParcel{deliveryTaskID: " + this.deliveryTaskID + ", pizzas: " + this.amountOfPizzas + "}";
    }
}
