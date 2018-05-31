package mas.experiments;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
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

public class DeliveryTaskGenerator implements Parcels.ParcelGenerator {
    private final double probNewDeliveryTask;
    long tickLength;

    DeliveryTaskGenerator(long tickLength, double probNewDeliveryTask) {
        this.tickLength = tickLength;
        this.probNewDeliveryTask = probNewDeliveryTask;
    }

    @Override
    public ImmutableList<AddParcelEvent> generate(long seed, @NotNull ScenarioGenerator.TravelTimes travelModel, long endTime) {
        com.google.common.collect.ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList.builder();

        // simTime = endTime
        // ticks = simTime / tickLength
        long ticks = endTime / tickLength;
        // amount of events = ticks * probability of delivery task at tick
        int numEvents = (int) (ticks * probNewDeliveryTask);

        TimeSeries.TimeSeriesGenerator announceTimeGenerator = TimeSeries.homogenousPoisson(endTime, numEvents);
        List<Double> eventTimes = announceTimeGenerator.generate(seed);

        for (Double time : eventTimes) {
            long ttime = DoubleMath.roundToLong(time, RoundingMode.FLOOR);

            if (ttime >= endTime) {
                break;
            }

            ParcelDTO parcelBuilder = Parcel
                    // Just put some random positions. Not that important as they will be chosen randomly in handler.
                    .builder(new Point(4, 2), new Point(0, 0))
                    .orderAnnounceTime(ttime)
                    .pickupTimeWindow(TimeWindow.create(ttime, 2*ttime))
                    .neededCapacity(1)
                    .buildDTO();

            eventList.add(AddParcelEvent.create(parcelBuilder));
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
