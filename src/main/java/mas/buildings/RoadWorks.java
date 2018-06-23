package mas.buildings;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import org.jetbrains.annotations.NotNull;

public class RoadWorks implements RoadUser, TickListener, PizzeriaUser {
    private static int IDCounter = 1;

    public final int id = nextID();
    public final Point position;
    public final long endTimestamp;
    private PizzeriaModel pizzeriaModel;

    public RoadWorks(Point position, long endTimestamp) {
        this.position = position;
        this.endTimestamp = endTimestamp;
    }

    @Override
    public String toString() {
        return "RoadWorks{id: " + this.id +
                ", position: " + this.position +
                ", endTimestamp: " + this.endTimestamp +
                "}";
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        model.addObjectAt(this, this.position);
    }

    @Override
    public void initPizzaUser(PizzeriaModel model) {
        this.pizzeriaModel = model;
    }

    @Override
    public void tick(@NotNull TimeLapse timeLapse) {
        if (timeLapse.getEndTime() > this.endTimestamp) {
            this.pizzeriaModel.finishRoadWorks(this);
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {

    }

    private int nextID() {
        return IDCounter++;
    }
}
