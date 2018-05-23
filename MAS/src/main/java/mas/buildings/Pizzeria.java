package mas.buildings;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import mas.agents.RobotAgent;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.tasks.DeliveryTask;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;


/**
 * Implementation of a mas.buildings.Pizzeria
 */
public class Pizzeria implements RoadUser, TickListener, PizzeriaUser {

    /**
     * For isRegistered implementation, see PDPObjectImpl
     */
    private Point position;
    private RoadModel roadModel;
    private PizzeriaModel pizzeriaModel;

    public Pizzeria(Point position) {
        this.position = position;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = model;

        model.addObjectAt(this, position);
    }

    public Point getPosition() {
        return this.position;
    }

    @Override
    public void initPizzaUser(PizzeriaModel model) {
        this.pizzeriaModel = model;
    }

    @Override
    public void tick(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }
}
