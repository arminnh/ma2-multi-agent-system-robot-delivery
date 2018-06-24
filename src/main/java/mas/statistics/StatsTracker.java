package mas.statistics;

import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;

import static com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType.*;
import static com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType.MOVE;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STARTED;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STOPPED;
import static mas.models.PizzeriaEventType.*;

/**
 * This class tracks statistics in a simulation.
 */
public final class StatsTracker extends AbstractModelVoid implements StatsProvider {
    private final EventDispatcher eventDispatcher;
    private final TheListener theListener;
    private final Clock clock;
    private final RoadModel roadModel;
    private final PizzeriaModel pizzeriaModel;

    StatsTracker(Clock c, PDPModel pm, PizzeriaModel pizzeriaModel, RoadModel rm) {
        this.clock = c;
        this.pizzeriaModel = pizzeriaModel;
        this.roadModel = rm;

        this.eventDispatcher = new EventDispatcher(StatsProvider.EventTypes.values());
        this.theListener = new TheListener(clock, eventDispatcher);

        this.clock.getEventAPI().addListener(theListener, STARTED, STOPPED);

        pm.getEventAPI().addListener(theListener, START_PICKUP, END_PICKUP, START_DELIVERY, END_DELIVERY, NEW_PARCEL,
                NEW_VEHICLE);

        this.pizzeriaModel.getEventAPI().addListener(theListener, ROBOT_AT_CHARGING_STATION,
                ROBOT_LEAVING_CHARGING_STATION, STARTED_ROADWORKS, NEW_TASK, END_TASK,
                FINISHED_ROADWORKS, DROP_PARCEL);

        this.roadModel.getEventAPI().addListener(theListener, MOVE);
    }

    /**
     * @return A new StatsTrackerBuilder instance.
     */
    public static StatsTrackerBuilder builder() {
        return new StatsTrackerBuilder();
    }

    @Override
    public EventAPI getEventAPI() {
        return eventDispatcher.getPublicEventAPI();
    }

    public TheListener getTheListener() {
        return theListener;
    }

    /**
     * @return A StatisticsDTO with the current simulation stats.
     */
    @Override
    public StatisticsDTO getStatistics() {
        TheListener tl = theListener;

        final int vehicleBack = tl.lastArrivalTimeAtDepot.size();

        long compTime = tl.computationTime;
        if (compTime == 0) {
            compTime = System.currentTimeMillis() - tl.startTimeReal;
        }

        int movedVehicles = tl.distanceMap.size();
        double avgPizzasPerRobot = (double) tl.pizzas / tl.totalVehicles;

        long tasksWaitingTime = this.pizzeriaModel.getDeliveryTasks().stream()
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
                tl.droppedParcels,
                tl.pickupTardiness,
                tl.deliveryTardiness,
                compTime,
                clock.getCurrentTime(),
                false,
                vehicleBack,
                0,
                tl.totalVehicles,
                movedVehicles,
                clock.getTimeUnit(),
                roadModel.getDistanceUnit(),
                roadModel.getSpeedUnit(),
                tasksWaitingTime,
                tl.totalTaskWaitingTime,
                tl.tasks,
                tl.totalTasks,
                tl.totalTasksFinished,
                tl.pizzas,
                tl.totalPizzas,
                avgPizzasPerRobot,
                tl.roadWorks,
                tl.totalRoadWorks
        );
    }

    @NotNull
    @Override
    public <U> U get(Class<U> type) {
        return type.cast(this);
    }
}
