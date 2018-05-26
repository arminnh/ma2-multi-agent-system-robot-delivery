package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.IntentionData;

import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link messagecontents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class IntentionAnt extends MultiDestinationAnt {

    public final boolean toChargingStation;
    public final boolean toDeliveryTask;

    public IntentionAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, List<IntentionData> intentions){
        super(id, path, estimatedTime, isReturning, robotID, robot, intentions);

        if(intentions != null){
            for(IntentionData d: intentions){
                if(d.position == null){
                    try {
                        throw new Exception("null poss");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        this.toDeliveryTask = intentions != null && intentions.get(0).deliveryTaskID != null;
        this.toChargingStation = !this.toDeliveryTask;
    }

    public IntentionAnt(List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, List<IntentionData> intentions) {
        super(path, estimatedTime, isReturning, robotID, robot, intentions);
        System.out.println("path = [" + path + "], estimatedTime = [" + estimatedTime + "], isReturning = [" + isReturning + "], robotID = [" + robotID + "], robot = [" + robot + "], intentions = [" + intentions + "]");
        System.out.println(intentions.get(0).deliveryTaskID);
        if(intentions != null){
            for(IntentionData d: intentions){
                if(d.position == null){
                    try {
                        throw new Exception("null poss");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        this.toDeliveryTask = intentions != null && intentions.get(0).deliveryTaskID != null;
        this.toChargingStation = !this.toDeliveryTask;
    }

    public IntentionAnt copy(List<Point> path, boolean isReturning, List<IntentionData> deliveries) {
        return new IntentionAnt(this.id, path, this.estimatedTime, isReturning, this.robotID, this.robot, deliveries);
    }

    public IntentionAnt copy(long estimatedTime) {
        return new IntentionAnt(this.id, this.path, estimatedTime, this.isReturning, this.robotID, this.robot, this.deliveries);
    }
}
