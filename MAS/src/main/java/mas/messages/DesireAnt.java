package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.tasks.DeliveryTask;

import java.util.List;

public class DesireAnt extends Ant {

    public final int deliveryID;
    public final int capacity;
    public int score;
    public DesireAnt(List<Point> path, long estimatedTime, boolean isReturning,
                     int id, Integer robotID, CommUser robot, int score, int deliveryID, int capacity) {
        super(path, estimatedTime, isReturning, id, robotID, robot);
        this.score = score;
        this.deliveryID = deliveryID;
        this.capacity = capacity;
    }

}
