package mas.statistics;

import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.event.EventAPI;
import com.github.rinde.rinsim.event.EventDispatcher;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import com.github.rinde.rinsim.scenario.ScenarioController;

import static com.github.rinde.rinsim.core.model.pdp.PDPModel.PDPModelEventType.*;
import static com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType.MOVE;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STARTED;
import static com.github.rinde.rinsim.core.model.time.Clock.ClockEventType.STOPPED;
import static com.github.rinde.rinsim.scenario.ScenarioController.EventType.*;

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

        System.out.println("STATS TRACKER INIT");
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

        return new StatisticsDTO(theListener.totalDistance, theListener.totalTime,
                theListener.totalPickups, theListener.totalDeliveries,
                theListener.totalParcels, theListener.acceptedParcels,
                theListener.pickupTardiness, theListener.deliveryTardiness, compTime,
                clock.getCurrentTime(), theListener.simFinish, vehicleBack,
                overTime, theListener.totalVehicles, theListener.distanceMap.size(),
                clock.getTimeUnit(), roadModel.getDistanceUnit(),
                roadModel.getSpeedUnit());
    }

    @Override
    public <U> U get(Class<U> clazz) {
        return clazz.cast(this);
    }
}
