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
        //dtModel.getEventAPI().addListener(theListener, ROBOT_AT_CHARGING_STATION);

        System.out.println("STATS TRACKER INIT");
    }

    public void addDeliveryTaskModelListener(PizzeriaModel dtModel){
        dtModel.getEventAPI().addListener(theListener, ROBOT_AT_CHARGING_STATION, ROBOT_LEAVING_CHARGING_STATION,
                NEW_PIZZERIA, NEW_ROADWORK, NEW_TASK, END_TASK, CLOSE_PIZZERIA, FINISH_ROADWORK);
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

    /**
     * @return A StatisticsDTO with the current simulation stats.
     */
    @Override
    public StatisticsDTO getStatistics() {
        final int vehicleBack = theListener.lastArrivalTimeAtDepot.size();
        long overTime = 0;
        if (theListener.simFinish) {
            for (final Long time : theListener.lastArrivalTimeAtDepot.values()) {
                if (time - theListener.scenarioEndTime > 0) {
                    overTime += time - theListener.scenarioEndTime;
                }
            }
        }

        long compTime = theListener.computationTime;
        if (compTime == 0) {
            compTime = System.currentTimeMillis() - theListener.startTimeReal;
        }

        TheListener t = theListener;
        double avgPizzasPerRobot = 1;
//        double avgPizzasPerRobot = t.pizzas / (t.totalVehicles - t.vehiclesIdle);
        return new StatisticsDTO(
                t.totalDistance, t.totalTime, t.totalPickups, t.totalDeliveries, t.totalParcels, t.acceptedParcels,
                t.pickupTardiness, t.deliveryTardiness, compTime, clock.getCurrentTime(), t.simFinish, vehicleBack,
                overTime, t.totalVehicles, t.distanceMap.size(), clock.getTimeUnit(), roadModel.getDistanceUnit(),
                roadModel.getSpeedUnit(), t.tasksWaitingTime, t.totalTaskWaitingTime, t.vehiclesIdle,
                t.totalRobotsTimeDriving, t.totalRobotsTimeIdle, t.totalRobotsTimeCharging, t.tasks, t.totalTasks,
                t.totalTasksFinished, t.pizzas, t.totalPizzas, avgPizzasPerRobot, t.pizzerias, t.roadWorks
        );
    }

    @Override
    public <U> U get(Class<U> clazz) {
        return clazz.cast(this);
    }
}
