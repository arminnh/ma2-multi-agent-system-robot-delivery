package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

public class MultiDestinationAnt extends Ant {
    public final List<IntentionData> intentions;

    public MultiDestinationAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, int robotID, CommUser robot, int pathIndex, List<IntentionData> intentions) {
        super(id, path, estimatedTime, isReturning, robotID, robot, pathIndex);

        this.intentions = intentions;
        this.checkIntentions();
    }

    public MultiDestinationAnt(List<Point> path, long estimatedTime, boolean isReturning,
                               int robotID, CommUser robot, int pathIndex, List<IntentionData> intentions) {
        super(path, estimatedTime, isReturning, robotID, robot, pathIndex);

        this.intentions = intentions;
        this.checkIntentions();
    }

    private void checkIntentions() {
        if (this.intentions == null) {
            throw new IllegalArgumentException("Intentions for ant cannot be null.");
        }
    }

    public boolean hasReachedDestination(Point currentPosition) {
        // If returning, only reached destination if at last point of path.
        if (this.isReturning) {
            return this.hasReachedFinalDestination();
        }

        if (this.intentions == null) {
            throw new IllegalStateException("Must have at least one intention in MultiDestinationAnt.");
        }

        // Otherwise, check if one of the intention's destination has been reached.
        for (IntentionData intention : this.intentions) {
            if (intention.position.equals(currentPosition)) {
                return true;
            }
        }

        // Finally, check if the end of the path has been reached. This is a fallback check.
        return this.hasReachedFinalDestination();

    }

    public boolean hasReachedFinalDestination() {
        return super.hasReachedDestination();
    }
}
