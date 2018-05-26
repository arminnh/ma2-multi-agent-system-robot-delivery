package mas.models;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import mas.SimulatorSettings;
import mas.agents.ResourceAgent;
import mas.agents.RobotAgent;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.buildings.RoadWorks;
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

    public static PizzeriaModelBuilder builder() {
        return new PizzeriaModelBuilder();
    }

    public void setSimulator(Simulator sim, RandomGenerator rng) {
        this.sim = sim;
        this.rng = rng;
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
        this.sim.register(pizzeria);

        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.NEW_PIZZERIA, 0, null, null, null
        ));

        return pizzeria;
    }

    public void closePizzeria(Pizzeria pizzeria) {
        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.CLOSE_PIZZERIA, 0, null, null, null
        ));

        this.sim.unregister(pizzeria);
    }

    public List<DeliveryTask> getDeliveryTasks() {
        return new LinkedList<>(this.roadModel.getObjectsOfType(DeliveryTask.class));
    }

    public void createNewDeliveryTask(RandomGenerator rng, double pizzaMean, double pizzaStd, long time) {
        int pizzaAmount = 1; //(int) (rng.nextGaussian() * pizzaStd + pizzaMean);

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

    public void deliverPizzaParcel(RobotAgent vehicle, PizzaParcel parcel, long time) {
        DeliveryTask task = parcel.deliveryTask;

        task.deliverPizzas(parcel.amountOfPizzas);

        if (task.isFinished()) {
            // If all pizzas for a deliveryTask have been delivered, the deliveryTask can be removed from the RoadModel.
            Set<ResourceAgent> agents = this.roadModel.getObjectsAt(task, ResourceAgent.class);
            agents.iterator().next().removeDeliveryTask(task);

            eventDispatcher.dispatchEvent(new PizzeriaEvent(PizzeriaEventType.END_TASK, time, task, parcel, vehicle));
            this.roadModel.removeObject(task);
            this.deliveryTasks.remove(task.id);

            // Unregister the task from the simulator
            //
            //this.sim.unregister(task);
        }
    }

    public void dropPizzaParcel(RobotAgent robot, PizzaParcel parcel, long time) {
        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.DROP_PARCEL, time, null, parcel, robot
        ));

        // Unregister the parcel from the simulator
        this.sim.unregister(parcel);
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

    public void createResourceAgent(Point position) {
        sim.register(new ResourceAgent(position, this.sim.getRandomGenerator()));
    }

    public void newRoadWorks(long time) {
        // Road works can only be set on positions where there is no robot, building, or delivery task.
        // Try to create new road works up to 3 times.
        int attempts = 5;

        while (attempts-- > 0) {
            // Find a new random position.
            Point position = this.roadModel.getRandomPosition(this.rng);

            RoadWorks roadWorks = new RoadWorks(position, time + SimulatorSettings.TIME_ROAD_WORKS);

            // First, register the works on the road
            this.sim.register(roadWorks);

            // If there is is nothing else on the position, set link to the relevant resource agent and fire an event
            boolean noRobots = this.roadModel.getObjectsAt(roadWorks, RobotAgent.class).isEmpty();
            boolean noTasks = this.roadModel.getObjectsAt(roadWorks, DeliveryTask.class).isEmpty();
            boolean noOtherWorks = this.roadModel.getObjectsAt(roadWorks, RoadWorks.class).size() == 1;
            boolean noPizzeria = this.roadModel.getObjectsAt(roadWorks, Pizzeria.class).isEmpty();
            boolean noChargingStation = this.roadModel.getObjectsAt(roadWorks, ChargingStation.class).isEmpty();

            if (noRobots && noTasks && noOtherWorks && noPizzeria && noChargingStation) {
                // Link the works to the resource agents of they are on.
                ResourceAgent agent = this.roadModel.getObjectsAt(roadWorks, ResourceAgent.class).iterator().next();
                agent.setRoadWorks(roadWorks);

                // Remove the node the RoadWorks lie on from the GraphRoadModel
                this.removeGraphConnectionsForNode(agent);

                eventDispatcher.dispatchEvent(new PizzeriaEvent(
                        PizzeriaEventType.STARTED_ROADWORKS, 0, null, null, null
                ));

                return;
            } else {
                // The road works should not be added to the environment, unregister it
                this.sim.unregister(roadWorks);
            }
        }
    }

    private void removeGraphConnectionsForNode(ResourceAgent resourceAgent) {
        final DynamicGraphRoadModelImpl g = sim.getModelProvider().getModel(DynamicGraphRoadModelImpl.class);
        ListenableGraph graph = g.getGraph();

        // Remove all connections to neighbors for which the connections in both ways can be removed.
        for (ResourceAgent neighbor : resourceAgent.getNeighbors()) {
            // TODO: fetch all robots and check that their positions do not lie in (resourceAgent.position, neighbor.position)

            // Try to remove the connection in one way, then try to remove it in the other.
            // If the connection cannot be removed in both ways, make sure they both are still in the graph afterwards.
            try {
                Connection c1 = graph.getConnection(resourceAgent.position, neighbor.position);
                Connection c2 = graph.getConnection(neighbor.position, resourceAgent.position);

                System.out.println("road user: " + g.hasRoadUserOn(resourceAgent.position, neighbor.position) + ", " + g.hasRoadUserOn(neighbor.position, resourceAgent.position));

                graph.removeConnection(c1.from(), c1.to());

                try {
                    graph.removeConnection(c2.from(), c2.to());
                } catch (IllegalStateException | IllegalArgumentException e) {
                    // Removing connection c2 caused an exception, add c1 back to the graph.
                    graph.addConnection(c1.from(), c1.to());
                }

            } catch (IllegalStateException | IllegalArgumentException e) {
                // Removing connection c1 caused an exception, nothing to do.
            }
        }
    }

    private void addGraphConnectionsForNode(ResourceAgent resourceAgent) {
        final DynamicGraphRoadModelImpl g = sim.getModelProvider().getModel(DynamicGraphRoadModelImpl.class);
        ListenableGraph graph = g.getGraph();

        for (ResourceAgent neighbor : resourceAgent.getNeighbors()) {
            if (!neighbor.getRoadWorks().isPresent()) {
                try {
                    graph.addConnection(resourceAgent.position, neighbor.position);
                    graph.addConnection(neighbor.position, resourceAgent.position);
                } catch (IllegalArgumentException e) {
                    // Connection already exists
                }
            }
        }
    }

    public void finishRoadWorks(RoadWorks roadWorks) {
        ResourceAgent agent = this.roadModel.getObjectsAt(roadWorks, ResourceAgent.class).iterator().next();

        // Add the connections that were removed
        this.addGraphConnectionsForNode(agent);

        // Unlink the road works from the resource agent they are linked to.
        agent.removeRoadWorks();

        eventDispatcher.dispatchEvent(new PizzeriaEvent(
                PizzeriaEventType.FINISHED_ROADWORKS, 0, null, null, null
        ));

        // Unregister the works from the simulator
        this.sim.unregister(roadWorks);
    }

    public Long getCurrentTime() {
        return clock.getCurrentTime();
    }
}
