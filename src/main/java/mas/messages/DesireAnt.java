package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

public class DesireAnt extends Ant {

    public final int deliveryTaskID;
    public final int pizzas;
    public long score;

    public DesireAnt(int id, List<Point> path, long estimatedTime, boolean isReturning, int robotID, CommUser robot, int pathIndex, long score, int deliveryTaskID, int pizzas) {
        super(id, path, estimatedTime, isReturning, robotID, robot, pathIndex);
        this.score = score;
        this.deliveryTaskID = deliveryTaskID;
        this.pizzas = pizzas;
    }

    public DesireAnt(List<Point> path, long estimatedTime, boolean isReturning, int robotID, CommUser robot, int pathIndex, long score, int deliveryTaskID, int pizzas) {
        super(path, estimatedTime, isReturning, robotID, robot, pathIndex);
        this.score = score;
        this.deliveryTaskID = deliveryTaskID;
        this.pizzas = pizzas;
    }

    @Override
    public String toString() {
        return "DesireAnt {id: " + this.id +
                ", deliveryTaskID: " + this.deliveryTaskID +
                ", pizzas: " + this.pizzas +
                ", isReturning: " + this.isReturning +
                ", estimatedTime: " + this.estimatedTime +
                ", score: " + this.score +
                ", path: " + this.path +
                "}";
    }

    public DesireAnt copy(List<Point> p, Boolean returning, Long score, Integer capacity, Integer pathIndex) {
        p = (p != null) ? p : this.path;
        returning = (returning != null) ? returning : this.isReturning;
        score = (score != null) ? score : this.score;
        capacity = (capacity != null) ? capacity : this.pizzas;
        pathIndex = (pathIndex != null) ? pathIndex : this.pathIndex;

        return new DesireAnt(this.id, p, this.estimatedTime, returning, this.robotID, this.robot, pathIndex, score, this.deliveryTaskID, capacity);
    }

    public DesireAnt copy(long estimatedTime, int pathIndex) {
        return new DesireAnt(this.id, this.path, estimatedTime, this.isReturning, this.robotID, this.robot, pathIndex, this.score, this.deliveryTaskID, this.pizzas);
    }
}