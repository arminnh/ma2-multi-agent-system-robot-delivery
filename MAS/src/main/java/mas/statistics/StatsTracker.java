package mas.statistics;

import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import com.github.rinde.rinsim.scenario.ScenarioController;
import mas.models.PizzeriaModel;
import mas.pizza.DeliveryTask;
import org.apache.commons.collections4.CollectionUtils;

import java.util.stream.Collectors;

import static com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType.*;
import static com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType.MOVE;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STARTED;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STOPPED;
import static mas.models.PizzeriaEventType.*;

/**
 * This class tracks statistics in a simulation.
 * Provides: StatsProvider
 * Dependencies: ScenarioController, Clock, RoadModel, PDPModel
 *
 * @author Rinde van Lon
 */
public final class StatsTracker extends AbstractModelVoid implements StatsProvider {
    private final EventDispatcher eventDispatcher;
    private final TheListener theListener;
    private final Clock clock;
    private final RoadModel roadModel;

    StatsTracker(ScenarioController scenContr, Clock c, RoadModel rm, PDPModel pm) {
        clock = c;
        roadModel = rm;

        eventDispatcher = new EventDispatcher(StatsProvider.EventTypes.values());
        theListener = new TheListener(clock, eventDispatcher);
        //scenContr.getEventAPI().addListener(theListener, SCENARIO_STARTED, SCENARIO_FINISHED, SCENARIO_EVENT);

        roadModel.getEventAPI().addListener(theListener, MOVE);
        clock.getEventAPI().addListener(theListener, STARTED, STOPPED);
        pm.getEventAPI().addListener(theListener, START_PICKUP, END_PICKUP, START_DELIVERY, END_DELIVERY, NEW_PARCEL, NEW_VEHICLE);
    }

    /**
     * @return A new StatsTrackerBuilder instance.
     */
    public static StatsTrackerBuilder builder() {
        return new StatsTrackerBuilder();
    }

    public void addDeliveryTaskModelListener(PizzeriaModel dtModel) {
        dtModel.getEventAPI().addListener(theListener, ROBOT_AT_CHARGING_STATION, ROBOT_LEAVING_CHARGING_STATION,
                NEW_PIZZERIA, NEW_ROADWORK, NEW_TASK, END_TASK, CLOSE_PIZZERIA, FINISH_ROADWORK);
    }

    @Override
    public EventAPI getEventAPI() {
        return eventDispatcher.getPublicEventAPI();
    }

    /**
     * @return A StatisticsDTO with the current simulation stats.
     */
    @Override
    public StatisticsDTO getStatistics() {
        TheListener tl = theListener;

        final int vehicleBack = tl.lastArrivalTimeAtDepot.size();
        long overTime = 0;
        if (tl.simFinish) {
            for (final Long time : tl.lastArrivalTimeAtDepot.values()) {
                if (time - tl.scenarioEndTime > 0) {
                    overTime += time - tl.scenarioEndTime;
                }
            }
        }

        long compTime = tl.computationTime;
        if (compTime == 0) {
            compTime = System.currentTimeMillis() - tl.startTimeReal;
        }

        int movedVehicles = tl.distanceMap.size();
        double avgPizzasPerRobot = (double) tl.pizzas / (tl.totalVehicles - tl.vehiclesIdle);

        long tasksWaitingTime = this.roadModel.getObjectsOfType(DeliveryTask.class).stream()
                .mapToLong(t -> t.getWaitingTime(clock.getCurrentTime()))
                .sum();

        return new StatisticsDTO(
                tl.totalDistance,
                tl.totalTravelTime,
                tl.totalIdleTime,
                tl.totalChargingTime,
                tl.totalPickups,
                tl.totalDeliveries,
                tl.totalParcels,
                tl.acceptedParcels,
                tl.pickupTardiness,
                tl.deliveryTardiness,
                compTime,
                clock.getCurrentTime(),
                tl.simFinish,
                vehicleBack,
                overTime,
                tl.totalVehicles,
                movedVehicles,
                clock.getTimeUnit(),
                roadModel.getDistanceUnit(),
                roadModel.getSpeedUnit(),
                tasksWaitingTime,
                tl.totalTaskWaitingTime,
                tl.vehiclesIdle,
                tl.tasks,
                tl.totalTasks,
                tl.totalTasksFinished,
                tl.pizzas,
                tl.totalPizzas,
                avgPizzasPerRobot,
                tl.pizzerias,
                tl.roadWorks,
                tl.totalRoadWorks
        );
    }

    @Override
    public <U> U get(Class<U> clazz) {
        return clazz.cast(this);
    }
}
