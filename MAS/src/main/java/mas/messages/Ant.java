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
    public final int pathIndex;

    private static int IDCounter = 1;

    public Ant(int id, List<Point> path, long estimatedTime, boolean isReturning, int robotID, CommUser robot, int pathIndex) {
        this.id = id;
        this.robot = robot;
        this.isReturning = isReturning;
        this.robotID = robotID;
        this.path = path;
        this.estimatedTime = estimatedTime;
        this.pathIndex = pathIndex;
    }

    public Ant(List<Point> path, long estimatedTime, boolean isReturning, int robotID, CommUser robot, int pathIndex) {
        this.id = getNextID();
        this.robot = robot;
        this.isReturning = isReturning;
        this.robotID = robotID;
        this.path = path;
        this.estimatedTime = estimatedTime;
        this.pathIndex = pathIndex;
    }

    public Ant copy(long estimatedTime, int pathIndex) {
        return new Ant(this.id, this.path, estimatedTime, this.isReturning, this.robotID, this.robot, pathIndex);
    }

    public boolean hasReachedDestination() {
        return this.pathIndex == this.path.size() - 1;
    }

    private int getNextID() {
        return IDCounter++;
    }
}
