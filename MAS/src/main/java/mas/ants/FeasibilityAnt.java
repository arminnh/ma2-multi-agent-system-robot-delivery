package mas.ants;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

public class FeasibilityAnt implements RoadUser, TickListener {

    private RoadModel rmModel;

    public FeasibilityAnt(){}

    @Override
    public void initRoadUser(RoadModel model) {
        this.rmModel = model;
    }


    @Override
    public void tick(TimeLapse timeLapse) {

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }
}
