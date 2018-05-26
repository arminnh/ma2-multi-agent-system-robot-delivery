package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.IntentionData;

import java.util.List;
import java.util.stream.Collectors;

public class MultiDestinationAnt extends Ant {
    public final List<IntentionData> deliveries;

    public MultiDestinationAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, Integer robotID, CommUser robot, List<IntentionData> intentions) {
        super(id, path, estimatedTime, isReturning, robotID, robot);

        this.deliveries = intentions;
    }

    public MultiDestinationAnt(List<Point> path, long estimatedTime, boolean isReturning,
                               Integer robotID, CommUser robot, List<IntentionData> intentions) {
        super(path, estimatedTime, isReturning, robotID, robot);

        this.deliveries = intentions;
    }

    public boolean hasReachedDestination(Point p) {
        System.out.println("MultiDestinationAnt.hasReachedDestination");
        if (this.isReturning) {
            return super.hasReachedDestination(p);
        } else {
            if(this.deliveries == null){
                return false;
            }
            for (IntentionData delivery : this.deliveries) {
                System.out.println("delivery.position + \" \" + p = " + delivery.position + " " +p);
                System.out.println(" "+delivery.position.equals(p));

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
        return this.deliveries.stream()
                .filter(data -> data.position == p)
                .collect(Collectors.toList());
    }
}
