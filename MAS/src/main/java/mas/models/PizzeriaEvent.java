package mas.models;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.event.Event;
import mas.tasks.DeliveryTask;

import javax.annotation.Nullable;

/**
 * Event object that is dispatched by {@link DeliveryTask}.
 */
public class PizzeriaEvent extends Event {

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

    public PizzeriaEvent(
            PizzeriaEventType type,
            long t,
            @Nullable DeliveryTask task,
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
