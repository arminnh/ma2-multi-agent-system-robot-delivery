package mas.ants;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;

public class ExplorationAnt implements RoadUser {
    public ExplorationAnt(){}

    private RoadModel rmModel;

    @Override
    public void initRoadUser(RoadModel model) {
        this.rmModel = model;
    }


}
