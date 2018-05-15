package mas.ants;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Stack;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link MessageContents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class ExplorationAnt implements MessageContents {
    private final List<Point> path;
    private List<Point> returning_path;
    private final int robot_id;
    private final Point destination;

    public ExplorationAnt(List<Point> path, Integer robot_id, Point destination){
        this.path = path;
        this.robot_id = robot_id;
        this.destination = destination;
    }

    public ExplorationAnt(List<Point> path, Integer robot_id, Point destination, List<Point> returning_path){
        this.path = path;
        this.robot_id = robot_id;
        this.destination = destination;
        this.returning_path = returning_path;
    }

    public List<Point> getPath() {
        return path;
    }

    public Integer getRobot_id() {
        return robot_id;
    }

    public Point getDestination() {
        return destination;
    }

    public List<Point> getReturning_path() {
        return this.returning_path;
    }

    public boolean hasReachedDestination(){
        return this.returning_path != null;
    }

}
