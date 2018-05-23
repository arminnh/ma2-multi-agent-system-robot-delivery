package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.DeliveryTaskData;
import mas.tasks.DeliveryTask;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link MessageContents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class ExplorationAnt extends Ant {
    public List<DeliveryTaskData> deliveries;
    public ExplorationAnt(List<Point> path, long estimatedTime, boolean isReturning, int id, Integer robotID, CommUser robot,List<DeliveryTaskData> deliveries) {
        super(path, estimatedTime, isReturning, id, robotID, robot);
        this.deliveries = deliveries;
    }

    public ExplorationAnt copy(List<Point> p, Long eTime, Boolean returning, List<DeliveryTaskData> deliveries) {
        p = (p != null) ? p : this.path;
        eTime = (eTime != null) ? eTime : this.estimatedTime;
        returning = (returning != null) ? returning : this.isReturning;
        deliveries = (deliveries != null) ? deliveries : this.deliveries;

        return new ExplorationAnt(p, eTime, returning, this.id, this.robotID, this.robot, deliveries);
    }

    public boolean hasReachedDestination(Point p) {
        if(this.isReturning){
            return super.hasReachedDestination(p);
        }else{
            for(DeliveryTaskData delivery: this.deliveries) {
                if (delivery.location == p) {
                    return true;
                }
            }
            return false;
        }
    }

    public DeliveryTaskData getDeliveryDataForLoc(Point p){
        for(DeliveryTaskData delivery: this.deliveries) {
            if (delivery.location == p) {
                return delivery;
            }
        }
        return null;
    }

    public boolean hasReachedFinalDestination(Point p){
        return super.hasReachedDestination(p);
    }

}
