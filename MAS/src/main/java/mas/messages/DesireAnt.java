package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

public class DesireAnt extends Ant {

    public final int deliveryTaskID;
    public final int pizzas;
    public Long score;

    public DesireAnt(List<Point> path, long estimatedTime, boolean isReturning, int id, Integer robotID, CommUser robot, Long score, int deliveryTaskID, int pizzas) {
        super(path, estimatedTime, isReturning, id, robotID, robot);
        this.score = score;
        this.deliveryTaskID = deliveryTaskID;
        this.pizzas = pizzas;
    }

    public DesireAnt copy(List<Point> p, Boolean returning, Long score, Integer capacity) {
        p = (p != null) ? p : this.path;
        returning = (returning != null) ? returning : this.isReturning;
        score = (score != null) ? score : this.score;
        capacity = (capacity != null) ? capacity : this.pizzas;

        return new DesireAnt(p, this.estimatedTime, returning, this.antID, this.robotID, this.robot, score, this.deliveryTaskID, capacity);
    }

    public DesireAnt copy(long estimatedTime) {
        return new DesireAnt(this.path, estimatedTime, this.isReturning, this.antID, this.robotID, this.robot, this.score, this.deliveryTaskID, this.pizzas);
    }
}
