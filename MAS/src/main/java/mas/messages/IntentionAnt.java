package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.DeliveryTaskData;

import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link messagecontents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class IntentionAnt extends MultiDestinationAnt {

    public final boolean toChargingStation;
    public final boolean toDeliveryTask;

    public IntentionAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, List<DeliveryTaskData> deliveries) {
        super(id, path, estimatedTime, isReturning, robotID, robot, deliveries);

        this.toDeliveryTask = deliveries.get(0).deliveryTaskID != null;
        this.toChargingStation = !this.toDeliveryTask;
    }

    public IntentionAnt(List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, List<DeliveryTaskData> deliveries) {
        super(path, estimatedTime, isReturning, robotID, robot, deliveries);

        this.toDeliveryTask = deliveries.get(0).deliveryTaskID != null;
        this.toChargingStation = !this.toDeliveryTask;
    }

    public IntentionAnt copy(List<Point> path, boolean isReturning, List<DeliveryTaskData> deliveries) {
        return new IntentionAnt(this.id, path, this.estimatedTime, isReturning, this.robotID, this.robot, deliveries);
    }

    public IntentionAnt copy(long estimatedTime) {
        return new IntentionAnt(this.id, this.path, estimatedTime, this.isReturning, this.robotID, this.robot, this.deliveries);
    }
}
