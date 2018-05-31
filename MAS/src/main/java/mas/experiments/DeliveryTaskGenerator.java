package mas.experiments;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.TimeSeries;
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
            if (time >= endTime) {
                System.out.println("time after endTime = " + time);
                break;
            }

            Parcel.Builder parcelBuilder = Parcel
                    // Just put some random positions. Not that important as they will be chosen randomly in handler.
                    .builder(new Point(4, 2), new Point(0, 0))
                    .orderAnnounceTime(DoubleMath.roundToLong(time, RoundingMode.FLOOR))
                    .pickupDuration(0)
                    .deliveryDuration(0)
                    .neededCapacity(1);

            eventList.add(AddParcelEvent.create(parcelBuilder.buildDTO()));
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
