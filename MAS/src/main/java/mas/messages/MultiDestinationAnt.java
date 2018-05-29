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

    public boolean hasReachedDestination(Point p) {
        System.out.println("MultiDestinationAnt.hasReachedDestination, estimatedTime: " + this.estimatedTime + ", path: " + this.path + ", intentions: " + this.intentions + " returning: "+ isReturning);
        if (this.isReturning) {
            return super.hasReachedDestination(p);
        } else {
            if(this.intentions == null){
                return false;
            }
            for (IntentionData delivery : this.intentions) {
                System.out.println("Position: " + p + ", Destination: " + delivery.position + ", equal: " + delivery.position.equals(p));

                if (delivery.position.equals(p)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean hasReachedFinalDestination(Point p) {
        return super.hasReachedDestination(p);
    }

    public List<IntentionData> getDeliveriesDataForPosition(Point p) {
        return this.intentions.stream()
                .filter(data -> data.position == p)
                .collect(Collectors.toList());
    }
}
