package mas.pizza;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;

public class PizzaParcel extends Parcel {
    private DeliveryTask task;
    private int amountPizzas;

    // the time at which the PizzaParcel was created
    public final long start_time;

    public PizzaParcel(ParcelDTO parcelDto, DeliveryTask task, int pizzaAmount, long time) {
        super(parcelDto);
        this.task = task;
        this.amountPizzas = pizzaAmount;

        this.start_time = time;
    }

    public DeliveryTask getDeliveryTask() {
        return this.task;
    }

    public int getAmountPizzas() {
        return amountPizzas;
    }
}
