package mas.models;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import mas.SimulatorSettings;
import mas.agents.ResourceAgent;
import mas.agents.RobotAgent;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.buildings.RoadWorks;
import mas.tasks.DeliveryTask;
import mas.tasks.PizzaParcel;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class PizzeriaModel extends AbstractModel<PizzeriaUser> {

    private final EventDispatcher eventDispatcher;
    private Simulator sim;
    private RoadModel roadModel;
    private ListenableGraph dynamicGraph;
    private DynamicGraphRoadModelImpl dynamicGraphRoadModel;
    private RandomGenerator rng;
    private Clock clock;
    private HashMap<Point, Pizzeria> pizzerias = new HashMap<>();
    private HashMap<Point, ChargingStation> chargingStations = new HashMap<>();
    private HashMap<Integer, DeliveryTask> deliveryTasks = new HashMap<>();
    private HashMap<Point, ResourceAgent> resourceAgents = new HashMap<>();

    public PizzeriaModel(Clock clock, RandomProvider provider, RoadModel roadModel, SimulatorAPI simAPI) {
        this.clock = clock;
        this.rng = provider.masterInstance();
        this.roadModel = roadModel;
        this.sim = (Simulator) simAPI;

        this.dynamicGraphRoadModel = (DynamicGraphRoadModelImpl) this.roadModel;
        this.dynamicGraph = this.dynamicGraphRoadModel.getGraph();

        this.eventDispatcher = new EventDispatcher(PizzeriaEventType.values());
    }

    public static PizzeriaModelBuilder builder() {
        return new PizzeriaModelBuilder();
    }

    @Nonnull
    @Override
    public <U> U get(Class<U> type) {
        if (type == PizzeriaModel.class) {
            return type.cast(this);
        }
        throw new IllegalArgumentException();
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
        Point position = this.roadModel.getRandomPosition(rng);

        Pizzeria pizzeria = new Pizzeria(position);

        this.pizzerias.put(position, pizzeria);
        this.sim.register(pizzeria);

        return pizzeria;
    }

    public ChargingStation openChargingStation() {
        Point position = roadModel.getRandomPosition(sim.getRandomGenerator());

        ChargingStation chargingStation = new ChargingStation(position, SimulatorSettings.CHARGING_STATION_CAPACITY);

        this.chargingStations.put(position, chargingStation);
        this.sim.register(chargingStation);


        return chargingStation;
    }

    public Optional<ChargingStation> getChargingStationAtPosition(Point position) {
        return Optional.fromNullable(this.chargingStations.get(position));
    }

    public List<DeliveryTask> getDeliveryTasks() {
        return new LinkedList<>(this.deliveryTasks.values());
    }

    public void createNewDeliveryTask(RandomGenerator rng, double pizzaMean, double pizzaStd, long time) {
        int pizzaAmount = 1; //(int) (rng.nextGaussian() * pizzaStd + pizzaMean);

        // Try to place the task on the graph up to 3 times.
        int attempts = 3;

        while (attempts-- > 0) {
            // Create a random position on the graph.
            Point position = roadModel.getRandomPosition(rng);

            // Can only create a DeliveryTask on an empty node or a node that contains only a robot.
            boolean canCreate = true;
            for (RoadUser roadUser : this.dynamicGraphRoadModel.getRoadUsersOnNode(position)) {
                if (roadUser.getClass() != RobotAgent.class) {
                    canCreate = false;
                    break;
                }
            }

            if (canCreate) {
                DeliveryTask task = new DeliveryTask(position, pizzaAmount, time, clock);

                this.deliveryTasks.put(task.id, task);
                // Link the task to the resource agent it was put on.
                this.resourceAgents.get(position).addDeliveryTask(task);
                this.sim.register(task);

                this.eventDispatcher.dispatchEvent(new PizzeriaEvent(PizzeriaEventType.NEW_TASK, time, task, null, null));
                return;
            }
        }

    }

    public PizzaParcel newPizzaParcel(int deliveryTaskID, Point startPosition, int pizzaAmount, long time) {
        DeliveryTask task = this.deliveryTasks.get(deliveryTaskID);

        System.out.println("task " + deliveryTaskID + ", time " + time);

        ParcelDTO pdto = Parcel.builder(startPosition, task.position)
                .neededCapacity(pizzaAmount)
                .buildDTO();

        PizzaParcel parcel = new PizzaParcel(pdto, task, pizzaAmount, time);
        this.sim.register(parcel);

        return parcel;
    }

    /**
     * True if the robot can deliver the PizzaParcel.
     * 1. If the robot has a reservation for the DeliveryTask
     * 2. If there is an unreserved amount of pizzas for the DeliveryTask.
     */
    public boolean canDeliverPizzaParcel(RobotAgent robot, PizzaParcel pizzaParcel) {
        Point position = robot.getPosition().get();
        DeliveryTask task = pizzaParcel.deliveryTask;

        // If robot is not on the location of the task, cannot deliver.
        if (!position.equals(task.position)) {
            return false;
        }

        ResourceAgent resourceAgent = this.resourceAgents.get(position);

        System.out.println("pizzaParcel = " + pizzaParcel);

        // The two alternatives mentioned in the comment.
        if (resourceAgent.robotHasReservation(robot.id, task.id) ||
                resourceAgent.getPizzasLeftForDeliveryTask(task.id) >= pizzaParcel.amountOfPizzas) {
            return true;
        }

        return false;
    }

    public void deliverPizzaParcel(RobotAgent vehicle, PizzaParcel parcel, long time) {
        DeliveryTask task = parcel.deliveryTask;

        task.deliverPizzas(parcel.amountOfPizzas);

        if (task.isFinished()) {
            // If all pizzas for a deliveryTask have been delivered, the deliveryTask can be removed from lists that hold it.
            this.eventDispatcher.dispatchEvent(new PizzeriaEvent(PizzeriaEventType.END_TASK, time, task, parcel, vehicle));
            this.resourceAgents.get(task.position).removeDeliveryTask(task);
            this.deliveryTasks.remove(task.id);
            this.roadModel.removeObject(task);

            // Unregister the task from the simulator
            //this.sim.unregister(task);
        }
    }

    public void dropPizzaParcel(RobotAgent robot, PizzaParcel parcel, long time) {
        this.eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.DROP_PARCEL, time, null, parcel, robot
        ));

        // Unregister the parcel from the simulator
        this.sim.unregister(parcel);
    }

    public void robotArrivedAtChargingStation(RobotAgent r, ChargingStation cs) {
        if (cs.addRobot(r)) {
            this.eventDispatcher.dispatchEvent(new PizzeriaEvent(
                    PizzeriaEventType.ROBOT_AT_CHARGING_STATION, 0, null, null, r
            ));
        }
    }

    public void robotLeftChargingStation(RobotAgent r, ChargingStation cs) {
        this.eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.ROBOT_LEAVING_CHARGING_STATION, 0, null, null, r
        ));

        cs.removeRobot(r);
        this.resourceAgents.get(cs.position).dropReservation(r);
    }

    public void createResourceAgent(Point position) {
        ResourceAgent agent = new ResourceAgent(position);

        if (this.chargingStations.containsKey(position)) {
            agent.setChargingStation(this.chargingStations.get(position));
        }

        this.resourceAgents.put(position, agent);
        this.sim.register(agent);
    }

    synchronized public void newRoadWorks(long time) {
        // Road works can only be set on positions where there is no robot, building, or delivery task.
        // Try to create new road works up to 5 times.
        int attempts = 5;

        while (attempts-- > 0) {
            // Find a new random position.
            Point position = this.roadModel.getRandomPosition(this.rng);

            // Check if the position is free.
            if (this.dynamicGraphRoadModel.getRoadUsersOnNode(position).isEmpty()) {

                RoadWorks roadWorks = new RoadWorks(position, time + SimulatorSettings.TIME_ROAD_WORKS);

                // First, register the works on the road.
                this.sim.register(roadWorks);

                // Link the works to the resource agent they are on.
                ResourceAgent agent = this.resourceAgents.get(position);
                agent.setRoadWorks(roadWorks);

                // Remove the all graph connections the node is in that can be removed.
                this.removeGraphConnectionsForNode(agent);

                this.eventDispatcher.dispatchEvent(new PizzeriaEvent(
                        PizzeriaEventType.STARTED_ROADWORKS, 0, null, null, null
                ));

                return;
            }
        }
    }

    private void removeGraphConnectionsForNode(ResourceAgent resourceAgent) {
        // Remove all connections to neighbors for which the connections in both ways can be removed.
        for (ResourceAgent neighbor : resourceAgent.getNeighbors()) {
            // Try to remove the connection in one direction, then try to remove it in the other.
            // If the connection cannot be removed in both ways, make sure they both are still in the graph afterwards.
            if (this.dynamicGraph.hasConnection(resourceAgent.position, neighbor.position)) {
                try {
                    this.dynamicGraph.removeConnection(resourceAgent.position, neighbor.position);

                    try {
                        this.dynamicGraph.removeConnection(neighbor.position, resourceAgent.position);
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        // Removing connection c2 caused an exception, add c1 back to the graph.
                        this.dynamicGraph.addConnection(resourceAgent.position, neighbor.position);
                        this.dynamicGraph.addConnection(neighbor.position, resourceAgent.position);
                    }
                } catch (IllegalStateException | IllegalArgumentException e) {
                    // Removing connection c1 caused an exception, nothing to do.
                    this.dynamicGraph.addConnection(resourceAgent.position, neighbor.position);
                }
            }
        }
    }

    private void addGraphConnectionsForNode(ResourceAgent resourceAgent) {
        for (ResourceAgent neighbor : resourceAgent.getNeighbors()) {
            if (!neighbor.getRoadWorks().isPresent()) {
                try {
                    this.dynamicGraph.addConnection(resourceAgent.position, neighbor.position);
                    this.dynamicGraph.addConnection(neighbor.position, resourceAgent.position);
                } catch (IllegalArgumentException e) {
                    // Connection already exists
                }
            }
        }
    }

    public void finishRoadWorks(RoadWorks roadWorks) {
        this.eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.FINISHED_ROADWORKS, 0, null, null, null
        ));

        ResourceAgent agent = this.resourceAgents.get(roadWorks.position);

        // Add the connections that were removed
        this.addGraphConnectionsForNode(agent);

        // Unlink the road works from the resource agent they are linked to.
        agent.removeRoadWorks();

        // Unregister the works from the simulator
        this.sim.unregister(roadWorks);
    }
}
