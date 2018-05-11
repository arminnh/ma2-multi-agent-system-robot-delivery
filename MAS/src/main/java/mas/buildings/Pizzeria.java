package mas.buildings;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import mas.models.PizzeriaModel;
import mas.models.PizzeriaUser;
import mas.pizza.DeliveryTask;
import mas.pizza.PizzaParcel;
import mas.robot.Robot;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
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
    private Optional<RoadModel> roadModel;
    private Optional<PizzeriaModel> dtModel;

    public Pizzeria(Point position, Simulator sim) {
        this.position = position;
    }

    @Override
    public void initRoadUser(@NotNull RoadModel model) {
        this.roadModel = Optional.of(model);

        model.addObjectAt(this, position);
    }


    private List<Robot> getWaitingRobots() {
        List<Robot> robots = new LinkedList<>(this.roadModel.get().getObjectsAt(this, Robot.class));

        // Filter out any totalVehicles that are in the pizzeria but have a task...
        CollectionUtils.filter(robots, new Predicate<Robot>() {
            @Override
            public boolean evaluate(Robot o) {
                return !o.hasTask() && o.getCapacityLeft() > 0;
            }
        });

        return robots;
    }

    private List<DeliveryTask> getAvailableDeliveryTasks() {
        List<DeliveryTask> tasks = new LinkedList<>(this.roadModel.get().getObjectsOfType(DeliveryTask.class));

        // Filter out any tasks that are not fully served yet.
        // Served here meaning we sent out the required amount of pizzas
        CollectionUtils.filter(tasks, new Predicate<DeliveryTask>() {
            @Override
            public boolean evaluate(DeliveryTask o) {
                return o.getPizzasLeft() > 0;
            }
        });

        return tasks;
    }


    @Override
    public void tick(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        List<Robot> waitingRobots = getWaitingRobots();
        List<DeliveryTask> waitingTasks = getAvailableDeliveryTasks();

        for (final DeliveryTask task : waitingTasks) {
            // Task allocation
            //System.out.println("yey");
            for (final Robot robot : waitingRobots) {
                if (task.getPizzasLeft() == 0) {
                    break;
                }
                if (robot.hasTask()) {
                    continue;
                }
                int capacityLeft = robot.getCapacityLeft();
                int pizzaAmount = Math.min(task.getPizzasLeft(), capacityLeft);
                //System.out.println(pizzaAmount);

                PizzaParcel parcel = dtModel.get().newParcel(this.position, task, pizzaAmount, time);

                robot.setTask(parcel);
                task.addReadyPizzas(pizzaAmount);

                dtModel.get().newParcelForRobot(robot, parcel, time);
                //this.pdpModel.get().pickup(robot, parcel, time);
            }

        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) {
    }

    public Point getPosition() {
        return this.position;
    }

    @Override
    public void initPizzaUser(PizzeriaModel model) {
        this.dtModel = Optional.of(model);
    }
}
