package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.DeliveryTaskData;

import java.util.List;
import java.util.stream.Collectors;

public class MultiDestinationAnt extends Ant {
    public final List<DeliveryTaskData> deliveries;

    public MultiDestinationAnt(List<Point> path, long estimatedTime, boolean isReturning, int antID, Integer robotID, CommUser robot, List<DeliveryTaskData> deliveries) {
        super(path, estimatedTime, isReturning, antID, robotID, robot);

        this.deliveries = deliveries;
    }

    public boolean hasReachedDestination(Point p) {
        if (this.isReturning) {
            return super.hasReachedDestination(p);
        } else {
            for (DeliveryTaskData delivery : this.deliveries) {
                if (delivery.position == p) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean hasReachedFinalDestination(Point p) {
        return super.hasReachedDestination(p);
    }

    public List<DeliveryTaskData> getDeliveriesDataForPosition(Point p) {
        return this.deliveries.stream()
                .filter(data -> data.position == p)
                .collect(Collectors.toList());
    }
}
