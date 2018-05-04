import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;

import javax.annotation.Nullable;

public class PizzaParcel extends Parcel {

    public PizzaParcel(ParcelDTO parcelDto, @Nullable String toString) {
        super(parcelDto, toString);
    }
}
