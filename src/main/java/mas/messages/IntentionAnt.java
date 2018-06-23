package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link messagecontents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class IntentionAnt extends MultiDestinationAnt {

    public final boolean toChargingStation;
    public final boolean toDeliveryTask;

    public IntentionAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, int robotID, CommUser robot, int pathIndex, List<IntentionData> intentions) {
        super(id, path, estimatedTime, isReturning, robotID, robot, pathIndex, intentions);

        this.toDeliveryTask = intentions.get(0).deliveryTaskID != 0;
        this.toChargingStation = !this.toDeliveryTask;
    }

    public IntentionAnt(List<Point> path, long estimatedTime, boolean isReturning, int robotID, CommUser robot, int pathIndex, List<IntentionData> intentions) {
        super(path, estimatedTime, isReturning, robotID, robot, pathIndex, intentions);

        this.toDeliveryTask = intentions.get(0).deliveryTaskID != 0;
        this.toChargingStation = !this.toDeliveryTask;
    }

    @Override
    public String toString() {
        return "IntentionAnt {id: " + this.id +
                ", intentionSize: " + this.intentions.size() +
                ", toDeliveryTask: " + this.toDeliveryTask +
                ", toChargingStation: " + this.toChargingStation +
                ", isReturning: " + this.isReturning +
                ", estimatedTime: " + this.estimatedTime +
                ", intentions: " + this.intentions +
                ", path: " + this.path +
                "}";
    }

    public IntentionAnt copy(List<Point> path, boolean isReturning, List<IntentionData> intentions, int pathIndex) {
        return new IntentionAnt(this.id, path, this.estimatedTime, isReturning, this.robotID, this.robot, pathIndex, intentions);
    }

    public IntentionAnt copy(long estimatedTime, int pathIndex) {
        return new IntentionAnt(this.id, this.path, estimatedTime, this.isReturning, this.robotID, this.robot, pathIndex, this.intentions);
    }

}
