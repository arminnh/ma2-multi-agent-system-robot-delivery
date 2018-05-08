package mas.pizza;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.event.Event;
import mas.pizza.DeliveryTask.DeliveryTaskEventType;

/**
 * Event object that is dispatched by {@link DeliveryTask}.
 * @author ARMIN HALILOVIC!!!!!
 */
public class DeliveryTaskEvent extends Event {

    /**
     * The {@link DeliveryTask} that dispatched this event.
     */
    public final DeliveryTask deliveryTask;

    /**
     * The time at which the event was dispatched.
     */
    public final long time;

    /**
     * The {@link Parcel} that was involved in the event, or <code>null</code> if
     * there was no {@link Parcel} involved in the event.
     */
    @Nullable
    public final Parcel parcel;

    /**
     * The {@link Vehicle} that was involved in the event, or <code>null</code> if
     * there was no {@link Vehicle} involved in the event.
     */
    @Nullable
    public final Vehicle vehicle;

    public DeliveryTaskEvent(
            DeliveryTaskEventType type,
            DeliveryTask task,
            long t,
            @Nullable Parcel p,
            @Nullable Vehicle v
    ) {
        super(type, task);
        deliveryTask = task;
        time = t;
        parcel = p;
        vehicle = v;
    }
}
