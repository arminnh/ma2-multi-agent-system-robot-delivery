package mas.robot;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import mas.pizza.DeliveryTask;
import mas.pizza.PizzaParcel;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Implementation of a very simple delivery robot.
 */
public class Robot extends Vehicle implements MovingRoadUser, TickListener, RandomUser, CommUser {

    private Battery battery;
    private int id;
    private Optional<PizzaParcel> current_task;

    private Optional<RandomGenerator> rnd;
    private Optional<CommDevice> comm;
    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;
    private Optional<Queue<Point>> current_path;
    private Point pizzeriaPos;
    private int current_capacity;

    public Robot(VehicleDTO vdto, Battery battery, int id, Point pizzeriaPos) {
        super(vdto);

        this.battery = battery;
        this.id = id;
        current_task = Optional.absent();
        this.current_path = Optional.absent();
        this.pizzeriaPos = pizzeriaPos;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.roadModel = Optional.of(pRoadModel);
        this.pdpModel = Optional.of(pPdpModel);
    }

    @Override
    public Optional<Point> getPosition() {
        if (roadModel.isPresent()) {
            return Optional.of(roadModel.get().getPosition(this));
        }
        return Optional.absent();
    }

    @Override
    public void setRandomGenerator(@NotNull RandomProvider provider) {
        rnd = Optional.of(provider.newInstance());
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(0.5);
        comm = Optional.of(builder.build());
    }

    @Override
    public void tickImpl(@NotNull TimeLapse time) {
        if (!time.hasTimeLeft()) {
            return;
        }

        // No parcel so move to pizzeria
        if (!current_task.isPresent()) {
            if(!this.current_path.isPresent()){
                Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.pizzeriaPos));
                this.current_path = Optional.of(path);
            }
        }else{
            // We have a parcel
            Queue<Point> path = new LinkedList<>(roadModel.get().getShortestPathTo(this, this.current_task.get().getDeliveryLocation()));
            this.current_path = Optional.of(path);
        }

        if(this.current_path.get().size() > 0){

            roadModel.get().followPath(this, this.current_path.get(), time);
        }

        if(current_task.isPresent()){
            DeliveryTask current_task = this.current_task.get().getDeliveryTask();
            if (roadModel.get().equalPosition(this, current_task)){
                // Deliver the pizzas
                pdpModel.get().deliver(this, this.current_task.get(), time);
                this.current_task.get().getDeliveryTask().deliverPizzas(this.current_task.get().getAmountPizzas());

                // Unload pizza's
                System.out.println("pre");
                System.out.println(this.current_capacity);

                this.current_capacity -= this.current_task.get().getAmountPizzas();
                System.out.println("post");
                System.out.println(this.current_capacity);

                if(this.current_task.get().getDeliveryTask().receivedAllPizzas()){
                    // All pizza's have been delivered, now we have to delete the task.
                    roadModel.get().removeObject(this.current_task.get().getDeliveryTask());
                }

                // Remove current task
                this.current_task = Optional.absent();

                // Remove current path
                this.current_path = Optional.absent();
            }
        }

    }

    public int getCurrentBatteryCapacity(){
        return this.battery.getRemainingCapacity();
    }

    public boolean hasTask(){
        return current_task.isPresent();
    }

    public int getCapacityLeft(){
        return new Double(this.getCapacity()).intValue() - this.current_capacity;
    }

    public void setTask(PizzaParcel task){
        this.current_task = Optional.of(task);
        this.current_capacity += task.getAmountPizzas();
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) { }
}