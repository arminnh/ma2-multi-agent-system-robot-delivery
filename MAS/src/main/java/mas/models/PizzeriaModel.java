package mas.models;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.geom.Point;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.pizza.DeliveryTask;
import mas.pizza.PizzaParcel;
import mas.robot.Robot;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

public class PizzeriaModel extends Model.AbstractModel<PizzeriaUser> {

    private final EventDispatcher eventDispatcher;
    private Simulator sim;
    private RoadModel rModel;
    private PDPModel pdpModel;
    private RandomGenerator rng;

    public PizzeriaModel(RoadModel rModel, PDPModel pdpModel) {
        eventDispatcher = new EventDispatcher(PizzeriaEventType.values());
        //this.sim = sim;
        this.rModel = rModel;
        this.pdpModel = pdpModel;
    }

    public void setSimulator(Simulator sim, RandomGenerator rng) {
        this.sim = sim;
        this.rng = rng;
    }

    public static DeliveryTaskModelBuilder builder() {
        return new DeliveryTaskModelBuilder();
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
        Pizzeria pizzeria = new Pizzeria(this.rModel.getRandomPosition(rng), sim);
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

    public void createNewDeliveryTask(RandomGenerator rng, double pizzaMean, double pizzaStd, long time) {
        int pizzaAmount = (int) (rng.nextGaussian() * pizzaStd + pizzaMean);

        DeliveryTask task = new DeliveryTask(rModel.getRandomPosition(rng), pizzaAmount, time);
        sim.register(task);

        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.NEW_TASK, time, task, null, null
        ));
    }

    public PizzaParcel newParcel(Point position, DeliveryTask task, int pizzaAmount, TimeLapse time) {
        ParcelDTO pdto = Parcel.builder(position, task.getPosition().get())
                .neededCapacity(pizzaAmount)
                .buildDTO();

        PizzaParcel parcel = new PizzaParcel(pdto, task, pizzaAmount, time.getStartTime());
        sim.register(parcel);

        return parcel;
    }

    public void newParcelForRobot(Robot robot, PizzaParcel parcel, TimeLapse time) {
        this.pdpModel.pickup(robot, parcel, time);
    }

    public void deliverPizzas(Robot vehicle, PizzaParcel parcel, long time) {
        DeliveryTask task = parcel.getDeliveryTask();

        task.deliverPizzas(parcel.getAmountPizzas());

        if (task.isFinished()) {
            eventDispatcher.dispatchEvent(new PizzeriaEvent(
                    PizzeriaEventType.END_TASK, time, task, parcel, vehicle
            ));
        }
    }

    public void robotAtChargingStation(Robot r, ChargingStation cs) {
        if (cs.addRobot(r)) {
            eventDispatcher.dispatchEvent(new PizzeriaEvent(
                    PizzeriaEventType.ROBOT_AT_CHARGING_STATION, 0, null, null, null
            ));
        }
    }

    public void robotLeavingChargingStation(Robot r, ChargingStation cs) {
        cs.removeRobot(r);

        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.ROBOT_LEAVING_CHARGING_STATION, 0, null, null, null
        ));

    }

    public void newRoadWork() {
        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.NEW_ROADWORK, 0, null, null, null
        ));

    }

    public void finishRoadWork() {
        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.FINISH_ROADWORK, 0, null,  null, null
        ));
    }
}
