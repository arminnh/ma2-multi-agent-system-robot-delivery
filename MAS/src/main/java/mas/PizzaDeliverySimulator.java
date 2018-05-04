package mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.*;
import mas.buildings.CityGraphCreator;
import mas.maps.ChargingStation;
import mas.pizza.DeliveryTask;
import mas.buildings.Pizzeria;
import mas.renderers.RobotRenderer;
import mas.robot.Battery;
import mas.robot.Robot;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;
import mas.renderers.DeliveryTaskRenderer;

import javax.measure.unit.SI;

public class PizzaDeliverySimulator {

    private static final long TICK_LENGTH = 1000L;
    private static final long RANDOM_SEED = 123L;
    private static final int SIM_SPEEDUP = 2;

    private static final int NUM_ROBOTS = 1;
    private static final int ROBOT_CAPACITY = 5;
    private static final int BATTERY_CAPACITY = 100;
    private static final int VEHICLE_LENGTH = 1;
    private static final double VEHICLE_SPEED_KMH = 50d;
    private static final double PROB_NEW_PARCEL = .02;

    private static int ROBOT_ID = 1;
    private static int PIZZAPARCEL_ID = 1;


    /**
     * @param args - No args.
     */
    public static void main(String[] args) {
        run(false);
    }


    /**
     * Runs the example.
     * @param testing If <code>true</code> the example will run in testing mode, automatically
     *                starting and stopping itself such that it can be run from a unit test.
     */
    private static void run(boolean testing) {
        // Configure the GUI with separate mas.renderers for the road, robots, customers, ...
        View.Builder viewBuilder = View.builder()
            .withTitleAppendix("Pizza delivery multi agent system simulator")
            //.withAutoPlay()
            //.withSpeedUp(SIM_SPEEDUP)
            .with(GraphRoadModelRenderer.builder()
                .withMargin(VEHICLE_LENGTH)
            )
            .with(RoadUserRenderer.builder()
                .withImageAssociation(Robot.class, "/robot.png")
                .withImageAssociation(Pizzeria.class, "/pizzeria.png")
                .withImageAssociation(ChargingStation.class, "/charging_station.png")
                .withImageAssociation(DeliveryTask.class, "/graphics/flat/person-black-32.png")
            )
            .with(DeliveryTaskRenderer.builder())
            .with(RobotRenderer.builder());
        /*
         * Image sources:
         * https://www.shutterstock.com/image-vector/line-pixel-style-classic-robot-rectangle-488817829
         * https://www.shutterstock.com/image-vector/pizza-delivery-service-icon-set-pixel-1039221658
         * https://www.shutterstock.com/search/source+station
         */

        // initialize a new Simulator instance
        final Simulator sim = Simulator.builder()
            // set the length of a simulation 'tick'
            .setTickLength(TICK_LENGTH)
            // set the random seed we use in this 'experiment'
            //.setRandomSeed(RANDOM_SEED)
            .addModel(RoadModelBuilders.dynamicGraph(CityGraphCreator.createGraph(10, VEHICLE_LENGTH))
                .withDistanceUnit(SI.METER)
                .withModificationCheck(true))
            .addModel(DefaultPDPModel.builder())
            .addModel(CommModel.builder())
            // in case a GUI is not desired simply don't add it.
            .addModel(viewBuilder)
            .build();

        final RandomGenerator rng = sim.getRandomGenerator();
        final RoadModel roadModel = sim.getModelProvider().getModel(RoadModel.class);
        final PDPModel pdpModel = sim.getModelProvider().getModel(PDPModel.class);

        final Pizzeria pizzeria = new Pizzeria(roadModel.getRandomPosition(rng), pdpModel, sim);
        sim.register(pizzeria);

        Double charging_station_capacity = NUM_ROBOTS * 0.3;
        ChargingStation chargingStation = new ChargingStation(
            roadModel.getRandomPosition(sim.getRandomGenerator()),
            charging_station_capacity.intValue()
        );
        sim.register(chargingStation);

        for(int i = 0; i < NUM_ROBOTS; i++) {
            VehicleDTO vdto = VehicleDTO.builder()
                .capacity(ROBOT_CAPACITY)
                .startPosition(roadModel.getRandomPosition(rng))//pizzeria.getPosition())
                .speed(1)
                .build();

            Battery battery = new Battery(BATTERY_CAPACITY);

            // Robots start at the pizzeria
            sim.register(new Robot(vdto, battery, ROBOT_ID, pizzeria.getPosition()));
            ROBOT_ID += 1;
        }

        sim.addTickListener(new TickListener() {
            @Override
            public void tick(@NotNull TimeLapse time) {
                if (rng.nextDouble() < PROB_NEW_PARCEL) {

                    // ParcelDTO pdto = Parcel.builder(roadModel.getRandomPosition(rng), roadModel.getRandomPosition(rng))
                    //     .serviceDuration(10)
                    //     .neededCapacity(1)
                    //     .buildDTO();

                    sim.register(new DeliveryTask(roadModel.getRandomPosition(rng), 1));
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {}
        });

        sim.start();
    }
}
