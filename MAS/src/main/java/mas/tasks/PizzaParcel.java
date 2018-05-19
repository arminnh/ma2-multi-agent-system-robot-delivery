package mas.tasks;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;

public class PizzaParcel extends Parcel {

    public final DeliveryTask deliveryTask;
    public final int amountOfPizzas;
    // the time at which the PizzaParcel was created
    public final long start_time;

    public PizzaParcel(ParcelDTO parcelDto, DeliveryTask deliveryTask, int pizzaAmount, long time) {
        super(parcelDto);

        this.deliveryTask = deliveryTask;
        this.amountOfPizzas = pizzaAmount;
        this.start_time = time;
    }
}
