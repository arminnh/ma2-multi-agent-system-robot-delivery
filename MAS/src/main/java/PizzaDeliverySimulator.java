import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.*;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.*;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.apache.commons.math3.random.RandomGenerator;

import javax.measure.unit.SI;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class PizzaDeliverySimulator {

    private static final int NUM_AGVS = 10;
    private static final int MAX_CAPACITY = 5;
    private static final int VEHICLE_LENGTH = 1;
    private static int robot_id = 1;

    private static final double NEW_PARCEL = .02;


    private PizzaDeliverySimulator() {}

    /**
     * @param args - No args.
     */
    public static void main(String[] args) {
        run(false);
    }


    /**
     * Runs the example.
     * @param testing If <code>true</code> the example will run in testing mode,
     *          automatically starting and stopping itself such that it can be run
     *          from a unit test.
     */
    public static void run(boolean testing) {
        View.Builder viewBuilder = View.builder()
                .with(GraphRoadModelRenderer.builder()
                        .withMargin(VEHICLE_LENGTH))
                .with(RoadUserRenderer.builder()
                        //https://www.shutterstock.com/image-vector/line-pixel-style-classic-robot-rectangle-488817829
                        .withImageAssociation(Robot.class, "/robot.png")
                        //https://www.shutterstock.com/image-vector/pizza-delivery-service-icon-set-pixel-1039221658
                        .withImageAssociation(Pizzeria.class, "/pizzeria.png")
                        // https://www.shutterstock.com/search/source+station
                        .withImageAssociation(ChargingStation.class, "/charging_station.png")
                        //.withImageAssociation(DeliveryTask.class, "/graphics/flat/person-black-32.png")
                );

        viewBuilder = viewBuilder.withTitleAppendix("Pizza delivery multi agent system simulator").withAutoPlay();

        final Simulator sim = Simulator.builder()
                .addModel(RoadModelBuilders.dynamicGraph(GraphCreator.createGraph(10))
                                .withDistanceUnit(SI.METER)
                                .withModificationCheck(true))
                .addModel(viewBuilder)
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .build();

        final RoadModel roadModel = sim.getModelProvider().getModel(RoadModel.class);

        final Pizzeria pizzeria = new Pizzeria(roadModel.getRandomPosition(sim.getRandomGenerator()), NUM_AGVS);
        ChargingStation chargingStation = new ChargingStation(
                roadModel.getRandomPosition(sim.getRandomGenerator()),
                NUM_AGVS * 0.3
        );

        sim.register(pizzeria);
        sim.register(chargingStation);

        for(int i = 0; i < NUM_AGVS; i++) {
            // Robots start at the pizzeria
            sim.register(new Robot(pizzeria.getLocation(), MAX_CAPACITY, robot_id));
            robot_id += 1;
        }

        final RandomGenerator rng = sim.getRandomGenerator();

        sim.addTickListener(new TickListener() {
            @Override
            public void tick(TimeLapse time) {
                if (rng.nextDouble() < NEW_PARCEL) {
                    sim.register(new DeliveryTask(
                            Parcel.builder(pizzeria.getLocation(),
                                    roadModel.getRandomPosition(rng))
                                    .serviceDuration(10)
                                    .neededCapacity(1)
                                    .buildDTO()));
                }
            }

            @Override
            public void afterTick(TimeLapse timeLapse) {}
        });

        sim.start();
    }

    static class GraphCreator {

        GraphCreator() {}

        static ImmutableTable<Integer, Integer, Point> createMatrix(int cols, int rows, Point offset) {
            final ImmutableTable.Builder<Integer, Integer, Point> builder =
                    ImmutableTable.builder();
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    builder.put(r, c, new Point(
                            offset.x + c * VEHICLE_LENGTH * 2,
                            offset.y + r * VEHICLE_LENGTH * 2));
                }
            }
            return builder.build();
        }

        static ListenableGraph<LengthData> createGraph(Integer size) {
            final Graph<LengthData> g = new TableGraph<>();

            final Table<Integer, Integer, Point> leftMatrix = createMatrix(size, size,
                    new Point(0, 0));
            for (final Map<Integer, Point> column : leftMatrix.columnMap().values()) {
                Graphs.addBiPath(g, column.values());
            }
            for (final Map<Integer, Point> row : leftMatrix.rowMap().values()) {
                Graphs.addBiPath(g, row.values());
            }

            return new ListenableGraph<>(g);
        }
    }
}
