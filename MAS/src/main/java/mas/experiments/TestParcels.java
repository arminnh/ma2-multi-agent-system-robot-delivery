package mas.experiments;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.google.common.collect.ImmutableList;
import sun.plugin.dom.exception.InvalidStateException;

import java.util.List;

public class TestParcels implements Parcels.ParcelGenerator {
    List<Point> possiblePoints;
    TestParcels(List<Point> p){
        possiblePoints = p;
    }

    @Override
    public ImmutableList<AddParcelEvent> generate(long seed, ScenarioGenerator.TravelTimes travelModel, long endTime) {
        com.google.common.collect.ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList.builder();

        com.github.rinde.rinsim.core.model.pdp.Parcel.Builder parcelBuilder = Parcel.builder(new Point(4, 2),new Point(0,0)).
                orderAnnounceTime(-1)
                .pickupDuration(0)
                .deliveryDuration(0)
                .neededCapacity(1);

        eventList.add(AddParcelEvent.create(parcelBuilder.buildDTO()));

        System.out.println("CREATED PARCEL");
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
