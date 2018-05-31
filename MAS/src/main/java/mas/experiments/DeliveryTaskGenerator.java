package mas.experiments;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;
import org.jetbrains.annotations.NotNull;

import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;

public class DeliveryTaskGenerator implements Parcels.ParcelGenerator {
    List<Point> possiblePositions;
    long tickLength;

    DeliveryTaskGenerator(List<Point> p, long tickLength) {
        this.possiblePositions = p;
        this.tickLength = tickLength;
        //TimeSeries.TimeSeriesGenerator a = TimeSeries.uniform()
    }

    @Override
    public ImmutableList<AddParcelEvent> generate(long seed, @NotNull ScenarioGenerator.TravelTimes travelModel, long endTime) {
        com.google.common.collect.ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList.builder();

        //List<Double> times = this.announceTimeGenerator.generate(this.rng.nextLong());
        Iterator waIsDees = times.iterator();

        while (waIsDees.hasNext()) {
            double time = (Double) waIsDees.next();
            if (time >= endTime) {
                break;
            }
            long arrivalTime = DoubleMath.roundToLong(time, RoundingMode.FLOOR);

            com.github.rinde.rinsim.core.model.pdp.Parcel.Builder parcelBuilder = Parcel
                    .builder(new Point(4, 2), new Point(0, 0))
                    .orderAnnounceTime(arrivalTime)
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
