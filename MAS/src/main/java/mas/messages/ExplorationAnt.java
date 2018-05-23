package mas.messages;

import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

/*
 * IMPORTANT: THE ANTS ARE MESSAGES THIS MEANS THEY SHOULD IMPLEMENT {@link MessageContents}
 *            THE ANTS SHOULD BE IMMUTABLE TOO, THIS MEANS THAT EVERYTIME YOU WANT TO SEND AN ANT TO A NEW DEVICE
 *            YOU NEED TO COPY THE DATA OF THE OLD ANT AND CREATE A NEW ONE
 */
public class ExplorationAnt extends Ant {

    public ExplorationAnt(List<Point> path, long estimatedTime, boolean isReturning, int id, Integer robotID, CommUser robot) {
        super(path, estimatedTime, isReturning, id, robotID, robot);
    }

    public ExplorationAnt copy(List<Point> p, Long eTime, Boolean returning) {
        p = (p != null) ? p : this.path;
        eTime = (eTime != null) ? eTime : this.estimatedTime;
        returning = (returning != null) ? returning : this.isReturning;

        return new ExplorationAnt(p, eTime, returning, this.id, this.robotID, this.robot);
    }

}
