package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link MessageContents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class ExplorationAnt extends MultiDestinationAnt {

    public ExplorationAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, Integer pathIndex, List<IntentionData> deliveries) {
        super(id, path, estimatedTime, isReturning, robotID, robot,pathIndex, deliveries);
    }

    public ExplorationAnt(List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, Integer pathIndex, List<IntentionData> deliveries) {
        super(path, estimatedTime, isReturning, robotID, robot, pathIndex, deliveries);
    }

    public ExplorationAnt copy(List<Point> p, Boolean returning, List<IntentionData> deliveries, Integer pathIndex) {
        p = (p != null) ? p : this.path;
        returning = (returning != null) ? returning : this.isReturning;
        deliveries = (deliveries != null) ? deliveries : this.deliveries;
        pathIndex = (pathIndex != null) ? pathIndex : this.pathIndex;

        return new ExplorationAnt(this.id, p, this.estimatedTime, returning, this.robotID, this.robot, pathIndex, deliveries);
    }

    public ExplorationAnt copy(long estimatedTime, Integer pathIndex) {
        return new ExplorationAnt(this.id, this.path, estimatedTime, this.isReturning, this.robotID, this.robot, pathIndex, this.deliveries);
    }
}
