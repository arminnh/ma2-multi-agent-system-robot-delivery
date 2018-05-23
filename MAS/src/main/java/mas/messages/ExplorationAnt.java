package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.DeliveryTaskData;

import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link MessageContents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class ExplorationAnt extends MultiDestinationAnt {

    public ExplorationAnt(List<Point> path, long estimatedTime, boolean isReturning, int antID, Integer robotID, CommUser robot, List<DeliveryTaskData> deliveries) {
        super(path, estimatedTime, isReturning, antID, robotID, robot, deliveries);
    }

    public ExplorationAnt copy(List<Point> p, Boolean returning, List<DeliveryTaskData> deliveries) {
        p = (p != null) ? p : this.path;
        returning = (returning != null) ? returning : this.isReturning;
        deliveries = (deliveries != null) ? deliveries : this.deliveries;

        return new ExplorationAnt(p, this.estimatedTime, returning, this.antID, this.robotID, this.robot, deliveries);
    }

    public ExplorationAnt copy(long estimatedTime) {
        return new ExplorationAnt(this.path, estimatedTime, this.isReturning, this.antID, this.robotID, this.robot, this.deliveries);
    }
}
