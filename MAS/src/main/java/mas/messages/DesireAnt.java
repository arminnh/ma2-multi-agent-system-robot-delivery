package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import mas.tasks.DeliveryTask;

import java.util.List;

public class DesireAnt extends Ant {

    public final int deliveryID;
    public final int capacity;
    public Long score;
    public DesireAnt(List<Point> path, long estimatedTime, boolean isReturning,
                     int id, Integer robotID, CommUser robot, Long score, int deliveryID, int capacity) {
        super(path, estimatedTime, isReturning, id, robotID, robot);
        this.score = score;
        this.deliveryID = deliveryID;
        this.capacity = capacity;
    }

    public DesireAnt copy(List<Point> p, Long eTime, Boolean returning, Long score, Integer capacity) {
        p = (p != null) ? p : this.path;
        eTime = (eTime != null) ? eTime : this.estimatedTime;
        returning = (returning != null) ? returning : this.isReturning;
        score = (score != null) ? score : this.score;
        capacity = (capacity != null) ? capacity : this.capacity;

        return new DesireAnt(p, eTime, returning, this.id, this.robotID, this.robot, score, this.deliveryID, capacity);
    }

}
