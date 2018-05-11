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
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.pizza.DeliveryTask;
import mas.pizza.PizzaParcel;
import mas.robot.Robot;
import org.apache.commons.math3.random.RandomGenerator;

/* Needed:
 *  - openPizzeria() => opens a new pizzeria
 *  - closePizzeria() => Closes a specific pizzeria
 *  - newRoadWork() => Creates a new roadwork
 *  - finishRoadWork() => Finishes a roadwork
 *  - robotAtChargingStation(Robot, ChargingStation) => Notifying when a robot gets to a charging station
 *  - robotLeavingChargingStation(Robot, ChargingStation) => Notifying when a robot leaves a charging station
 */
public class DeliveryTaskModel extends Model.AbstractModel<PizzaUser> {

    private final EventDispatcher eventDispatcher;
    private Simulator sim;
    private RoadModel rmModel;
    private PDPModel pdpModel;
    private RandomGenerator rng;

    public DeliveryTaskModel(RoadModel rmModel, PDPModel pdpModel){
        eventDispatcher = new EventDispatcher(DeliveryTaskEventType.values());
        //this.sim = sim;
        this.rmModel = rmModel;
        this.pdpModel = pdpModel;
    }

    public void setSimulator(Simulator sim){
        this.sim = sim;
        this.rng = sim.getRandomGenerator();

    }

    public EventAPI getEventAPI(){
        return this.eventDispatcher.getPublicEventAPI();
    }

    public Pizzeria openPizzeria(){
        Pizzeria pizzeria = new Pizzeria(this.rmModel.getRandomPosition(rng), sim);
        sim.register(pizzeria);

        eventDispatcher.dispatchEvent(
                new DeliveryTaskEvent(DeliveryTaskEventType.NEW_PIZZERIA, null, 0, null,null)
        );

        return pizzeria;
    }

    public void closePizzeria(){

        eventDispatcher.dispatchEvent(
                new DeliveryTaskEvent(DeliveryTaskEventType.CLOSE_PIZZERIA, null, 0, null,null)
        );

    }

    public void newRoadWork(){
        eventDispatcher.dispatchEvent(
                new DeliveryTaskEvent(DeliveryTaskEventType.NEW_ROADWORK, null, 0, null,null)
        );

    }

    public void finishRoadWork(){
        eventDispatcher.dispatchEvent(
                new DeliveryTaskEvent(DeliveryTaskEventType.FINISH_ROADWORK, null, 0, null,null)
        );


    }

    public void robotAtChargingStation(Robot r, ChargingStation cs){
        if(cs.addRobot(r)){
            eventDispatcher.dispatchEvent(
                    new DeliveryTaskEvent(DeliveryTaskEventType.ROBOT_ENTERING_CHARGINGSTATION, null, 0, null,null)
            );
        }
    }

    public void robotLeavingChargingStation(Robot r, ChargingStation cs){
        cs.removeRobot(r);

        eventDispatcher.dispatchEvent(
                new DeliveryTaskEvent(DeliveryTaskEventType.ROBOT_LEAVING_CHARGINGSTATION, null, 0, null,null)
        );

    }

    public PizzaParcel newParcel(Point position, DeliveryTask task, int pizzaAmount, TimeLapse time){

        ParcelDTO pdto = Parcel.builder(position, task.getPosition().get())
                .neededCapacity(pizzaAmount)
                .buildDTO();

        PizzaParcel parcel = new PizzaParcel(pdto, task, pizzaAmount, time.getStartTime());
        sim.register(parcel);

        return parcel;
    }

    @Override
    public boolean register(PizzaUser element) {
        element.initPizzaUser(this);
        return true;
    }

    @Override
    public boolean unregister(PizzaUser element) {
        return false;
    }

    // Returns a builder
    public static DeliveryTaskModelBuilder builder() {
        return new DeliveryTaskModelBuilder();
    }

    public void newParcelForRobot(Robot robot, PizzaParcel parcel, TimeLapse time) {
        this.pdpModel.pickup(robot, parcel, time);
    }
}
