package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;
import java.util.stream.Collectors;

public class MultiDestinationAnt extends Ant {
    public final List<IntentionData> intentions;

    public MultiDestinationAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, Integer pathIndex, List<IntentionData> intentions) {
        super(id, path, estimatedTime, isReturning, robotID, robot, pathIndex);

        this.intentions = intentions;
    }

    public MultiDestinationAnt(List<Point> path, long estimatedTime, boolean isReturning,
                               Integer robotID, CommUser robot, Integer pathIndex, List<IntentionData> intentions) {
        super(path, estimatedTime, isReturning, robotID, robot, pathIndex);

        this.intentions = intentions;
    }

    public boolean hasReachedDestination(Point currentPosition) {
        if (this.isReturning) {
            return super.hasReachedDestination();
        } else {
            if(this.intentions == null){
                throw new IllegalStateException("Must have at least one intention in MultiDestinationAnt.");
            }

            for (IntentionData intention : this.intentions) {
                if (intention.position.equals(currentPosition)) {
                    return true;
                }
            }

            return false;
        }
    }
}
