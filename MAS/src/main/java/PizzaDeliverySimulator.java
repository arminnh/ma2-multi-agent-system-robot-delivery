import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.*;
import org.apache.commons.math3.random.RandomGenerator;

import javax.measure.unit.SI;

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
    private static void run(boolean testing) {
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
                )
                .with(DeliveryTaskRenderer.builder());

        viewBuilder = viewBuilder.withTitleAppendix("Pizza delivery multi agent system simulator").withAutoPlay();

        CityGraphCreator graphCreator = new CityGraphCreator(VEHICLE_LENGTH);

        final Simulator sim = Simulator.builder()
                .addModel(RoadModelBuilders.dynamicGraph(graphCreator.createGraph(10))
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
                        Parcel.builder(roadModel.getRandomPosition(rng), roadModel.getRandomPosition(rng))
                            .serviceDuration(10)
                            .neededCapacity(1)
                            .buildDTO()
                    ));
                }
            }

            @Override
            public void afterTick(TimeLapse timeLapse) {}
        });

        sim.start();
    }
}
