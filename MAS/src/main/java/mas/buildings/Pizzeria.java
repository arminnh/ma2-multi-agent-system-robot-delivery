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

    private List<RobotAgent> getWaitingRobots() {
        List<RobotAgent> robotAgents = new LinkedList<>(this.roadModel.getObjectsAt(this, RobotAgent.class));

        // Filter out any totalVehicles that are in the pizzeria but have a deliveryTask...
        CollectionUtils.filter(robotAgents, o -> !o.hasPizzaParcel() && o.getCapacityLeft() > 0);

        return robotAgents;
    }

    private List<DeliveryTask> getAvailableDeliveryTasks() {
        List<DeliveryTask> tasks = new LinkedList<>(this.roadModel.getObjectsOfType(DeliveryTask.class));

        // Filter out any tasks that need more pizzas to be created and delivered.
        CollectionUtils.filter(tasks, o -> o.getPizzasLeft() > 0);

        return tasks;
    }

    @Override
    public void tick(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        List<RobotAgent> waitingRobotAgents = getWaitingRobots();
        List<DeliveryTask> waitingTasks = getAvailableDeliveryTasks();

        for (final DeliveryTask task : waitingTasks) {
            // Task allocation
            for (final RobotAgent robotAgent : waitingRobotAgents) {
                if (task.getPizzasLeft() == 0) {
                    break;
                }

                if (robotAgent.hasPizzaParcel()) {
                    continue;
                }
                int pizzaAmount = Math.min(task.getPizzasLeft(), robotAgent.getCapacityLeft());

                pizzeriaModel.newPizzaParcelForRobot(robotAgent, task, this.position, pizzaAmount, time);
            }

        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }
}
