package mas.models;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.geom.Point;
import mas.agents.ResourceAgent;
import mas.agents.RobotAgent;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.tasks.DeliveryTask;
import mas.tasks.PizzaParcel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class PizzeriaModel extends Model.AbstractModel<PizzeriaUser> {

    private final EventDispatcher eventDispatcher;
    private Simulator sim;
    private RoadModel roadModel;
    private RandomGenerator rng;
    private Clock clock;
    private HashMap<Integer, DeliveryTask> deliveryTasks = new HashMap<>();

    public PizzeriaModel(RoadModel roadModel, Clock clock) {
        this.roadModel = roadModel;
        eventDispatcher = new EventDispatcher(PizzeriaEventType.values());
        this.clock = clock;
    }

    public void setSimulator(Simulator sim, RandomGenerator rng) {
        this.sim = sim;
        this.rng = rng;
    }

    public static PizzeriaModelBuilder builder() {
        return new PizzeriaModelBuilder();
    }

    public EventAPI getEventAPI() {
        return this.eventDispatcher.getPublicEventAPI();
    }

    @Override
    public boolean register(@NotNull PizzeriaUser element) {
        element.initPizzaUser(this);
        return true;
    }

    @Override
    public boolean unregister(@NotNull PizzeriaUser element) {
        return false;
    }

    public Pizzeria openPizzeria() {
        Pizzeria pizzeria = new Pizzeria(this.roadModel.getRandomPosition(rng));
        sim.register(pizzeria);

        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.NEW_PIZZERIA, 0, null, null, null
        ));

        return pizzeria;
    }

    public void closePizzeria() {
        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.CLOSE_PIZZERIA, 0, null, null, null
        ));
    }

    public List<DeliveryTask> getDeliveryTasks() {
        List<DeliveryTask> tasks = new LinkedList<>(this.roadModel.getObjectsOfType(DeliveryTask.class));

        // Filter out any tasks that need more pizzas to be created and delivered.
        CollectionUtils.filter(tasks, o -> !o.isFinished());

        return tasks;
    }

    public void createNewDeliveryTask(RandomGenerator rng, double pizzaMean, double pizzaStd, long time) {
        int pizzaAmount = (int) (rng.nextGaussian() * pizzaStd + pizzaMean);

        DeliveryTask task = new DeliveryTask(roadModel.getRandomPosition(rng), pizzaAmount, time, clock);
        this.deliveryTasks.put(task.id, task);
        sim.register(task);

        Set<ResourceAgent> agents = this.roadModel.getObjectsAt(task, ResourceAgent.class);
        agents.iterator().next().addDeliveryTask(task);

        eventDispatcher.dispatchEvent(new PizzeriaEvent(PizzeriaEventType.NEW_TASK, time, task, null, null));
    }

    public PizzaParcel newPizzaParcel(int deliveryTaskID, Point startPosition, int pizzaAmount, long time) {
        DeliveryTask task = this.deliveryTasks.get(deliveryTaskID);

        ParcelDTO pdto = Parcel.builder(startPosition, task.getPosition().get())
                .neededCapacity(pizzaAmount)
                .buildDTO();

        PizzaParcel parcel = new PizzaParcel(pdto, task, pizzaAmount, time);
        sim.register(parcel);

        return parcel;
    }

    public void deliverPizzas(RobotAgent vehicle, PizzaParcel parcel, long time) {
        DeliveryTask task = parcel.deliveryTask;

        task.deliverPizzas(parcel.amountOfPizzas);

        if (task.isFinished()) {
            // If all pizzas for a deliveryTask have been delivered, the deliveryTask can be removed from the RoadModel.
            Set<ResourceAgent> agents = this.roadModel.getObjectsAt(task, ResourceAgent.class);
            agents.iterator().next().removeDeliveryTask(task);

            eventDispatcher.dispatchEvent(new PizzeriaEvent(PizzeriaEventType.END_TASK, time, task, parcel, vehicle));
            this.roadModel.removeObject(task);
            this.deliveryTasks.remove(task.id);
        }
    }

    public void robotArrivedAtChargingStation(RobotAgent r, ChargingStation cs) {
        if (cs.addRobot(r)) {
            eventDispatcher.dispatchEvent(new PizzeriaEvent(
                    PizzeriaEventType.ROBOT_AT_CHARGING_STATION, 0, null, null, null
            ));
        }
    }

    public void robotLeftChargingStation(RobotAgent r, ChargingStation cs) {
        cs.removeRobot(r);

        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.ROBOT_LEAVING_CHARGING_STATION, 0, null, null, null
        ));

    }

    public void newRoadWorks() {
        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.NEW_ROADWORKS, 0, null, null, null
        ));

    }

    public void finishRoadWorks() {
        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.FINISHED_ROADWORKS, 0, null,  null, null
        ));
    }

    public Long getCurrentTime(){
        return clock.getCurrentTime();
    }
}
