package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

public class Ant implements MessageContents {
    public final List<Point> path;
    public final long estimatedTime;
    public final int id;
    public final int robotID;
    public final CommUser robot;
    public final boolean isReturning;

    public Ant(List<Point> path, long estimatedTime, boolean isReturning, int id, Integer robotID, CommUser robot){
        this.robot = robot;
        this.isReturning = isReturning;
        this.id = id;
        this.robotID = robotID;
        this.path = path;
        this.estimatedTime = estimatedTime;
    }

    public boolean hasReachedDestination(Point p) {
        return this.path.get(this.path.size() - 1) == p;
    }
}
