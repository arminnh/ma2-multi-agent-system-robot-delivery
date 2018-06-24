package mas.statistics;

import com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType;
import com.github.rinde.rinsim.core.model.pdp.PDPModelEvent;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveEvent;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import mas.agents.RobotAgent;
import mas.models.PizzeriaEvent;
import mas.models.PizzeriaEventType;
import mas.tasks.PizzaParcel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.google.common.collect.Maps.newLinkedHashMap;


public class TheListener implements Listener {
    private static final double MOVE_THRESHOLD = 0.0001;
    public final Map<MovingRoadUser, Double> distanceMap;
    public final Map<MovingRoadUser, Long> lastArrivalTimeAtDepot;
    private final Clock clock;
    private final EventDispatcher eventDispatcher;
    // tasks
    public int tasks;
    public int totalTasks;
    public int totalTasksFinished;
    public long tasksWaitingTime;
    public long totalTaskWaitingTime;
    // pizzas & parcels
    public int pizzas;
    public int totalPizzas;
    public int totalParcels; // The total number of parcels
    public int acceptedParcels; // Same as totalPickups. The total number of parcels that were actually added in the model.
    public int droppedParcels;
    public long totalPizzaTravelTime;
    // vehicles
    public int totalVehicles;
    public int vehiclesCharging;
    public int totalPickups; // same as acceptedParcels
    public int totalDeliveries;
    public long pickupTardiness;
    public long deliveryTardiness;
    public double totalDistance;
    public long totalTravelTime;
    public long totalIdleTime;
    public long totalChargingTime;
    // road works
    public int roadWorks;
    public int totalRoadWorks;
    // simulation
    public long simulationStartTime = System.currentTimeMillis();
    public long startTimeReal;
    public long startTimeSim;
    public long computationTime;
    public long simulationTime;

    TheListener(Clock clock, EventDispatcher eventDispatcher) {
        this.clock = clock;
        this.eventDispatcher = eventDispatcher;

        tasks = 0;
        totalTasks = 0;
        totalTasksFinished = 0;
        tasksWaitingTime = 0L;
        totalTaskWaitingTime = 0L;

        pizzas = 0;
        totalPizzas = 0;
        totalParcels = 0;
        acceptedParcels = 0;
        totalPizzaTravelTime = 0L;

        totalVehicles = 0;
        vehiclesCharging = 0;
        totalDistance = 0;
        totalTravelTime = 0L;

        totalPickups = 0;
        totalDeliveries = 0;
        pickupTardiness = 0L;
        deliveryTardiness = 0L;
        totalIdleTime = 0L;
        totalChargingTime = 0L;
        distanceMap = newLinkedHashMap();
        lastArrivalTimeAtDepot = newLinkedHashMap();

        roadWorks = 0;

        startTimeReal = 0L;
        startTimeSim = 0L;
        computationTime = 0L;
        simulationTime = 0L;
    }

    @Override
    public void handleEvent(@NotNull Event e) {
        if (e.getEventType() == Clock.ClockEventType.STARTED) {
            startTimeReal = System.currentTimeMillis();
            startTimeSim = clock.getCurrentTime();
            computationTime = 0;

        } else if (e.getEventType() == Clock.ClockEventType.STOPPED) {
            computationTime = System.currentTimeMillis() - startTimeReal;
            simulationTime = clock.getCurrentTime() - startTimeSim;

        } else if (e.getEventType() == GenericRoadModel.RoadEventType.MOVE) {
            final MoveEvent me = (MoveEvent) e;
            RobotAgent robot = (RobotAgent) me.roadUser;

            double moveDistance = me.pathProgress.distance().getValue();
            if (!distanceMap.containsKey(robot)) {
                distanceMap.put(robot, moveDistance);
            } else {
                distanceMap.put(robot, distanceMap.get(robot) + moveDistance);
            }

            totalDistance += moveDistance;
            totalTravelTime += me.pathProgress.time().getValue();

            double distanceFromDepot = Point.distance(
                    me.roadModel.getPosition(me.roadUser),
                    ((Vehicle) me.roadUser).getStartPosition()
            );

            // if we are closer than 10 cm to the depot, we say we are 'at' the depot
            if (distanceFromDepot < MOVE_THRESHOLD) {
                // only override time if the vehicle did actually move
                if (moveDistance > MOVE_THRESHOLD) {
                    lastArrivalTimeAtDepot.put((MovingRoadUser) me.roadUser, clock.getCurrentTime());

                    if (totalVehicles == lastArrivalTimeAtDepot.size()) {
                        eventDispatcher.dispatchEvent(new Event(
                                StatsProvider.EventTypes.ALL_VEHICLES_AT_DEPOT, this
                        ));
                    }
                }
            } else {
                lastArrivalTimeAtDepot.remove(me.roadUser);
            }

            totalIdleTime += robot.getAndResetIdleTime();

        } else if (e.getEventType() == PDPModelEventType.START_PICKUP) {
            final PDPModelEvent pme = (PDPModelEvent) e;
            final Parcel p = pme.parcel;
            final Vehicle v = pme.vehicle;
            assert p != null;
            assert v != null;

            final long latestBeginTime = p.getPickupTimeWindow().end() - p.getPickupDuration();
            if (pme.time > latestBeginTime) {
                final long tardiness = pme.time - latestBeginTime;
                pickupTardiness += tardiness;
            }

        } else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
            final PDPModelEvent pme = (PDPModelEvent) e;
            RobotAgent r = (RobotAgent) pme.vehicle;
            assert r != null;

            totalPickups++;

        } else if (e.getEventType() == PDPModelEventType.START_DELIVERY) {
            final PDPModelEvent pme = (PDPModelEvent) e;
            final Parcel p = pme.parcel;
            assert p != null;

            final long latestBeginTime = p.getDeliveryTimeWindow().end() - p.getDeliveryDuration();
            if (pme.time > latestBeginTime) {
                final long tardiness = pme.time - latestBeginTime;
                deliveryTardiness += tardiness;
            }

        } else if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
            final PDPModelEvent pme = (PDPModelEvent) e;
            final PizzaParcel p = (PizzaParcel) pme.parcel;
            assert p != null;

            totalDeliveries++;
            totalPizzaTravelTime += clock.getCurrentTime() - p.startTime;
            pizzas -= p.amountOfPizzas;

        } else if (e.getEventType() == PDPModelEventType.NEW_PARCEL) {
            PDPModelEvent pme = (PDPModelEvent) e;
            PizzaParcel p = (PizzaParcel) pme.parcel;
            assert p != null;

            acceptedParcels++;
            pizzas += p.amountOfPizzas;
            totalPizzas += pizzas;

        } else if (e.getEventType() == PizzeriaEventType.DROP_PARCEL) {
            PizzeriaEvent pe = (PizzeriaEvent) e;
            PizzaParcel p = (PizzaParcel) pe.parcel;
            assert p != null;

            pizzas -= p.amountOfPizzas;
            droppedParcels += p.amountOfPizzas;

        } else if (e.getEventType() == PDPModelEventType.NEW_VEHICLE) {
            final PDPModelEvent ev = (PDPModelEvent) e;

            lastArrivalTimeAtDepot.put(ev.vehicle, clock.getCurrentTime());
            totalVehicles++;

        } else if (e.getEventType() == PizzeriaEventType.NEW_TASK) {
            tasks++;
            totalTasks++;

        } else if (e.getEventType() == PizzeriaEventType.END_TASK) {
            final PizzeriaEvent ev = (PizzeriaEvent) e;

            tasks--;
            totalTasksFinished++;
            totalTaskWaitingTime += clock.getCurrentTime() - ev.deliveryTask.startTime;

        } else if (e.getEventType() == PizzeriaEventType.STARTED_ROADWORKS) {
            roadWorks++;
            totalRoadWorks++;

        } else if (e.getEventType() == PizzeriaEventType.FINISHED_ROADWORKS) {
            roadWorks--;

        } else if (e.getEventType() == PizzeriaEventType.ROBOT_AT_CHARGING_STATION) {
            vehiclesCharging++;

        } else if (e.getEventType() == PizzeriaEventType.ROBOT_LEAVING_CHARGING_STATION) {
            final PizzeriaEvent ev = (PizzeriaEvent) e;
            final RobotAgent robot = (RobotAgent) ev.vehicle;
            assert robot != null;

            vehiclesCharging--;
            totalChargingTime += robot.getAndResetChargeTime();

        } else {
            try {
                throw new Exception("Unknown Event Type: " + e.toString());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
