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
import com.github.rinde.rinsim.pdptw.common.StatsPanel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.maps.CityGraphCreator;
import mas.models.PizzeriaModel;
import mas.pizza.DeliveryTask;
import mas.renderers.DeliveryTaskRenderer;
import mas.renderers.RobotRenderer;
import mas.robot.Battery;
import mas.robot.Robot;
import mas.statistics.StatsTracker;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;

public class PizzaDeliverySimulator {

    private static final long TICK_LENGTH = 1000L;
    private static final long RANDOM_SEED = 123L;
    private static final int SIM_SPEEDUP = 1;

    private static final int NUM_ROBOTS = 3;
    private static final int ROBOT_CAPACITY = 5;
    private static final int BATTERY_CAPACITY = 100;
    private static final int VEHICLE_LENGTH = 1;
    private static final double VEHICLE_SPEED_KMH = 0.5;

    private static final double PROB_NEW_PARCEL = .02;
    private static final double PROB_PIZZERIA_OPEN = .002;
    private static final double PROB_PIZZERIA_CLOSE = .002;
    private static final double PROB_ROAD_WORKS_START = .005;
    private static final double PROB_ROAD_WORKS_END = .005;
    private static final double PIZZA_AMOUNT_STD = 0.75;
    private static final double PIZZA_AMOUNT_MEAN = 4;

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
     *
     * @param testing If <code>true</code> the example will run in testing mode, automatically
     *                starting and stopping itself such that it can be run from a unit test.
     */
    private static void run(boolean testing) {
        // Configure the GUI with separate mas.renderers for the road, totalVehicles, customers, ...
        View.Builder viewBuilder = View.builder()
                .withTitleAppendix("Pizza delivery multi agent system simulator")
                .withAutoPlay()
                .withSpeedUp(SIM_SPEEDUP)
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
                .with(RobotRenderer.builder())
                .with(StatsPanel.builder())
                .withResolution(1920, 1080);
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
                .addModel(PizzeriaModel.builder())
                .addModel(mas.statistics.StatsTracker.builder())
                // in case a GUI is not desired simply don't add it.
                .addModel(viewBuilder)
                .build();

        final RandomGenerator rng = sim.getRandomGenerator();
        final RoadModel roadModel = sim.getModelProvider().getModel(RoadModel.class);
        final PDPModel pdpModel = sim.getModelProvider().getModel(PDPModel.class);
        final PizzeriaModel dtModel = sim.getModelProvider().getModel(PizzeriaModel.class);
        dtModel.setSimulator(sim, rng);
        final StatsTracker statsTracker = sim.getModelProvider().getModel(StatsTracker.class);
        statsTracker.addDeliveryTaskModelListener(dtModel);

        final Pizzeria pizzeria = dtModel.openPizzeria();

        ChargingStation chargingStation = new ChargingStation(
                roadModel.getRandomPosition(sim.getRandomGenerator()),
                new Double(NUM_ROBOTS * 0.3).intValue()
        );
        sim.register(chargingStation);

        for (int i = 0; i < NUM_ROBOTS; i++) {
            VehicleDTO vdto = VehicleDTO.builder()
                    .capacity(ROBOT_CAPACITY)
                    //.startPosition(pizzeria.getPosition())
                    .startPosition(roadModel.getRandomPosition(rng))
                    .speed(VEHICLE_SPEED_KMH)
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
                    dtModel.createNewDeliveryTask(rng, PIZZA_AMOUNT_MEAN, PIZZA_AMOUNT_STD, time.getStartTime());
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {
            }
        });

        sim.start();
    }
}
