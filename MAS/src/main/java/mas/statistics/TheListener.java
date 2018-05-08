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
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.scenario.ScenarioController.ScenarioEvent;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import mas.pizza.DeliveryTask.DeliveryTaskEventType;
import mas.pizza.DeliveryTaskEvent;
import mas.pizza.PizzaParcel;

import java.util.Map;

import static com.google.common.base.Verify.verify;
import static com.google.common.collect.Maps.newLinkedHashMap;


public class TheListener implements Listener {

    private static final double MOVE_THRESHOLD = 0.0001;
    public final Map<MovingRoadUser, Double> distanceMap;
    public final Map<MovingRoadUser, Long> lastArrivalTimeAtDepot;
    // tasks
    public int totalTasks;
    public int totalTasksFinished;
    public int totalTaskWaitingTime;
    // parcels
    public int totalParcels;
    public int acceptedParcels;
    public int totalPizzaTravelTime;
    // vehicles
    public int totalVehicles;
    public double totalDistance;
    public long totalTime;
    public int totalPickups;
    public int totalDeliveries;
    public long pickupTardiness;
    public long deliveryTardiness;
    // simulation
    public long startTimeReal;
    public long startTimeSim;
    public long computationTime;
    public long simulationTime;
    public boolean simFinish;
    public long scenarioEndTime;
    private Clock clock;
    private EventDispatcher eventDispatcher;

    TheListener(Clock clock, EventDispatcher eventDispatcher) {
        this.clock = clock;
        this.eventDispatcher = eventDispatcher;

        totalParcels = 0;
        acceptedParcels = 0;

        totalVehicles = 0;
        distanceMap = newLinkedHashMap();
        totalDistance = 0d;
        totalTime = 0L;
        lastArrivalTimeAtDepot = newLinkedHashMap();

        totalPickups = 0;
        totalDeliveries = 0;
        pickupTardiness = 0;
        deliveryTardiness = 0;

        simFinish = false;
    }

    @Override
    public void handleEvent(Event e) {
        if (e.getEventType() == Clock.ClockEventType.STARTED) {
            startTimeReal = System.currentTimeMillis();
            startTimeSim = clock.getCurrentTime();
            computationTime = 0;

        } else if (e.getEventType() == Clock.ClockEventType.STOPPED) {
            computationTime = System.currentTimeMillis() - startTimeReal;
            simulationTime = clock.getCurrentTime() - startTimeSim;

        } else if (e.getEventType() == GenericRoadModel.RoadEventType.MOVE) {
            verify(e instanceof MoveEvent);
            final MoveEvent me = (MoveEvent) e;
            increment((MovingRoadUser) me.roadUser, me.pathProgress.distance().getValue().doubleValue());
            totalDistance += me.pathProgress.distance().getValue().doubleValue();
            totalTime += me.pathProgress.time().getValue();
            // if we are closer than 10 cm to the depot, we say we are 'at'
            // the depot
            if (Point.distance(me.roadModel.getPosition(me.roadUser),
                    ((Vehicle) me.roadUser).getStartPosition()) < MOVE_THRESHOLD) {
                // only override time if the vehicle did actually move
                if (me.pathProgress.distance().getValue().doubleValue() > MOVE_THRESHOLD) {
                    lastArrivalTimeAtDepot.put((MovingRoadUser) me.roadUser, clock.getCurrentTime());
                    if (totalVehicles == lastArrivalTimeAtDepot.size()) {
                        eventDispatcher.dispatchEvent(
                                new Event(StatsProvider.EventTypes.ALL_VEHICLES_AT_DEPOT, this)
                        );
                    }
                }
            } else {
                lastArrivalTimeAtDepot.remove(me.roadUser);
            }

        } else if (e.getEventType() == PDPModelEventType.START_PICKUP) {
            verify(e instanceof PDPModelEvent);
            final PDPModelEvent pme = (PDPModelEvent) e;
            final Parcel p = pme.parcel;
            final Vehicle v = pme.vehicle;
            assert p != null;
            assert v != null;

            final long latestBeginTime = p.getPickupTimeWindow().end()
                    - p.getPickupDuration();
            if (pme.time > latestBeginTime) {
                final long tardiness = pme.time - latestBeginTime;
                pickupTardiness += tardiness;
                /*eventDispatcher.dispatchEvent(new StatsEvent(
                        StatsProvider.EventTypes.PICKUP_TARDINESS, this, p, v, tardiness,
                        pme.time));*/
            }

        } else if (e.getEventType() == PDPModelEventType.END_PICKUP) {
            totalPickups++;

        } else if (e.getEventType() == PDPModelEventType.START_DELIVERY) {
            final PDPModelEvent pme = (PDPModelEvent) e;

            final Parcel p = pme.parcel;
            final Vehicle v = pme.vehicle;
            assert p != null;
            assert v != null;

            final long latestBeginTime = p.getDeliveryTimeWindow().end()
                    - p.getDeliveryDuration();
            if (pme.time > latestBeginTime) {
                final long tardiness = pme.time - latestBeginTime;
                deliveryTardiness += tardiness;
                /*eventDispatcher.dispatchEvent(new StatsEvent(
                        StatsProvider.EventTypes.DELIVERY_TARDINESS, this, p, v,
                        tardiness, pme.time));*/
            }

        } else if (e.getEventType() == PDPModelEventType.END_DELIVERY) {
            totalDeliveries++;
            final PDPModelEvent pme = (PDPModelEvent) e;
            final PizzaParcel p = (PizzaParcel) pme.parcel;
            //final Vehicle v = pme.vehicle;

            totalPizzaTravelTime += clock.getCurrentTime() - p.start_time;

        } else if (e.getEventType() == ScenarioController.EventType.SCENARIO_EVENT) {
            final ScenarioEvent se = (ScenarioEvent) e;
            if (se.getTimedEvent() instanceof AddParcelEvent) {
                totalParcels++;
            } else if (se.getTimedEvent() instanceof AddVehicleEvent) {
                totalVehicles++;
            } else if (se.getTimedEvent() instanceof TimeOutEvent) {
                simFinish = true;
                scenarioEndTime = se.getTimedEvent().getTime();
            }

        } else if (e.getEventType() == DeliveryTaskEventType.NEW_TASK) {
            totalTasks++;

        } else if (e.getEventType() == DeliveryTaskEventType.END_TASK) {
            final DeliveryTaskEvent ev = (DeliveryTaskEvent) e;

            totalTasksFinished++;
            totalTaskWaitingTime += clock.getCurrentTime() - ev.deliveryTask.start_time;

        } else if (e.getEventType() == PDPModelEventType.NEW_PARCEL) {
            // pdp model event
            acceptedParcels++;

        } else if (e.getEventType() == PDPModelEventType.NEW_VEHICLE) {
            verify(e instanceof PDPModelEvent);
            final PDPModelEvent ev = (PDPModelEvent) e;
            lastArrivalTimeAtDepot.put(ev.vehicle, clock.getCurrentTime());

        } else {
            // currently not handling fall throughs
        }

        System.out.println("Total tasks: " + totalTasks +
                ". Task waiting time: " + (totalTaskWaitingTime / 1000) + "s" +
                ". Total pizza travel time: " + (totalPizzaTravelTime / 1000) + "s");
    }

    private void increment(MovingRoadUser mru, double num) {
        if (!distanceMap.containsKey(mru)) {
            distanceMap.put(mru, num);
        } else {
            distanceMap.put(mru, distanceMap.get(mru) + num);
        }
    }
}
