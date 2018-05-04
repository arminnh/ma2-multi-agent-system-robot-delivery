import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class PizzaRobot extends Vehicle implements MovingRoadUser, TickListener, RandomUser, CommUser{

    private RandomGenerator rnd;
    private CommDevice comm;
    private static final double VEHICLE_SPEED_KMH = 50d;
    private String name;
    private Optional<Parcel> curr;


    PizzaRobot(Point startPosition, int capacity) {
        super(VehicleDTO.builder()
                .capacity(capacity)
                .startPosition(startPosition)
                .speed(VEHICLE_SPEED_KMH)
                .build());

        Random r2 = new Random(); // just create one and keep it around
        String alphabet = "abcdefghijklmnopqrstuvwxyz";

        final int N = 10;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < N; i++) {
            sb.append(alphabet.charAt(r2.nextInt(alphabet.length())));
        }
        name = sb.toString();
        curr = Optional.absent();

    }

    @Override
    public void tickImpl(@NotNull TimeLapse time) {

    }

    static class MyNameIs implements MessageContents {
        private final String name;

        MyNameIs(String nm) {
            name = nm;
        }

        String getName() {
            return name;
        }
    }

    @Override
    public void afterTick(@NotNull TimeLapse timeLapse) { }


    @Override
    public Optional<Point> getPosition() {
        return Optional.of(getRoadModel().getPosition(this));
    }

    @Override
    public void setCommDevice(@NotNull CommDeviceBuilder builder) {
        builder.setMaxRange(0.5);
        comm = builder.build();
    }

    @Override
    public void setRandomGenerator(@NotNull RandomProvider provider) {
        rnd = provider.newInstance();
    }
}