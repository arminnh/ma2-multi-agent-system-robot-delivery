package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

public class Ant implements MessageContents {
    public final List<Point> path;
    public final long estimatedTime;
    public final int antID;
    public final int robotID;
    public final CommUser robot;
    public final boolean isReturning;

    public Ant(List<Point> path, long estimatedTime, boolean isReturning, int antID, Integer robotID, CommUser robot) {
        this.robot = robot;
        this.isReturning = isReturning;
        this.antID = antID;
        this.robotID = robotID;
        this.path = path;
        this.estimatedTime = estimatedTime;
    }

    public Ant copy(long estimatedTime) {
        return new Ant(this.path, estimatedTime, this.isReturning, this.antID, this.robotID, this.robot);
    }

    public boolean hasReachedDestination(Point p) {
        return this.path.get(this.path.size() - 1) == p;
    }
}
