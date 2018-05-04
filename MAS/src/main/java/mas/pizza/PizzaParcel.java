package mas.pizza;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;

import javax.annotation.Nullable;

public class PizzaParcel extends Parcel {
    private DeliveryTask task;
    private int amountPizzas;

    public PizzaParcel(ParcelDTO parcelDto, DeliveryTask task, int pizzaAmount) {
        super(parcelDto);
        this.task = task;
        this.amountPizzas = pizzaAmount;
    }

    public DeliveryTask getDeliveryTask(){
        return this.task;
    }

    public int getAmountPizzas() {
        return amountPizzas;
    }
}
