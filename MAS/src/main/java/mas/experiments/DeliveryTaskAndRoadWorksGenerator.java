package mas.experiments;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import org.jetbrains.annotations.NotNull;

import java.math.RoundingMode;
import java.util.List;

public class DeliveryTaskAndRoadWorksGenerator implements Parcels.ParcelGenerator {
    private final long tickLength;
    private final double probNewDeliveryTask;
    private final double probNewRoadWorks;

    public static final double DELIVERY_TASK_EVENT = 1;
    public static final double ROAD_WORKS_EVENT = 2;

    DeliveryTaskAndRoadWorksGenerator(long tickLength, double probNewDeliveryTask, double probNewRoadWorks) {
        this.tickLength = tickLength;
        this.probNewDeliveryTask = probNewDeliveryTask;
        this.probNewRoadWorks = probNewRoadWorks;
    }

    @Override
    public ImmutableList<AddParcelEvent> generate(long seed, @NotNull ScenarioGenerator.TravelTimes travelModel, long endTime) {
        com.google.common.collect.ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList.builder();

        long ticks = endTime / tickLength;

        int numDeliveryTasks = (int) (ticks * probNewDeliveryTask);
        int numRoadWorks = (int) (ticks * probNewRoadWorks);

        List<Double> deliveryTaskTimes = TimeSeries.homogenousPoisson(endTime, numDeliveryTasks).generate(seed);

        for (Double time : deliveryTaskTimes) {
            long ttime = DoubleMath.roundToLong(time, RoundingMode.FLOOR);

            if (ttime >= endTime) {
                break;
            }

            eventList.add(AddParcelEvent.create(Parcel
                    // Just put some random positions. Not that important as they will be chosen randomly in handler.
                    .builder(new Point(4, 2), new Point(0, 0))
                    .orderAnnounceTime(ttime)
                    .pickupTimeWindow(TimeWindow.create(ttime, 2 * ttime))
                    .neededCapacity(DELIVERY_TASK_EVENT)
                    .buildDTO()
            ));
        }

        if(numRoadWorks > 0) {
            List<Double> roadWorksTimes = TimeSeries.homogenousPoisson(endTime, numRoadWorks).generate(seed);


            for (Double time : roadWorksTimes) {
                long ttime = DoubleMath.roundToLong(time, RoundingMode.FLOOR);

                if (ttime >= endTime) {
                    break;
                }

                eventList.add(AddParcelEvent.create(Parcel
                        // Just put some random positions. Not that important as they will be chosen randomly in handler.
                        .builder(new Point(6, 4), new Point(2, 2))
                        .orderAnnounceTime(ttime)
                        .pickupTimeWindow(TimeWindow.create(ttime, 2 * ttime))
                        .neededCapacity(ROAD_WORKS_EVENT)
                        .buildDTO()
                ));
            }
        }

        return eventList.build();
    }

    @Override
    public Point getCenter() {
        return null;
    }

    @Override
    public Point getMin() {
        return null;
    }

    @Override
    public Point getMax() {
        return null;
    }
}
