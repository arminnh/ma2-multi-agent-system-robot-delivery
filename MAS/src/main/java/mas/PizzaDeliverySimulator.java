package mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.StatsPanel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import mas.agents.Battery;
import mas.agents.RobotAgent;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.buildings.RoadWorks;
import mas.graphs.CityGraphCreator;
import mas.models.PizzeriaModel;
import mas.renderers.DeliveryTaskRenderer;
import mas.renderers.RobotRenderer;
import mas.statistics.StatsTracker;
import mas.tasks.DeliveryTask;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;

public class PizzaDeliverySimulator {

    private static int robotID = 1;

    /**
     * @param args - No args.
     */
    public static void main(String[] args) {
        run();
    }

    /**
     * Runs the example.
     */
    private static void run() {
        // Configure the GUI with separate mas.renderers for the road, totalVehicles, customers, ...
        View.Builder viewBuilder = View.builder()
                .withTitleAppendix("Pizza delivery multi agent system simulator")
                .withAutoPlay()
                .withSpeedUp(SimulatorSettings.SIM_SPEEDUP)
                .with(GraphRoadModelRenderer.builder()
                        .withMargin(SimulatorSettings.VEHICLE_LENGTH)
                )
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(RobotAgent.class, "/robot.png")
                        .withImageAssociation(Pizzeria.class, "/pizzeria.png")
                        .withImageAssociation(ChargingStation.class, "/charging_station.png")
                        .withImageAssociation(DeliveryTask.class, "/graphics/flat/person-black-32.png")
                        .withImageAssociation(RoadWorks.class, "/road_works.png")
                )
                .with(CommRenderer.builder().withMessageCount()
                        //.withReliabilityColors()
                        //.withToString()
                        //.withMessageCount()
                )
                .with(DeliveryTaskRenderer.builder())
                .with(RobotRenderer.builder())
                .with(StatsPanel.builder())
                .withResolution(SimulatorSettings.WINDOW_WIDTH, SimulatorSettings.WINDOW_HEIGHT);
        /*
         * Image sources:
         * https://www.shutterstock.com/image-vector/line-pixel-style-classic-robot-rectangle-488817829
         * https://www.shutterstock.com/image-vector/pizza-delivery-service-icon-set-pixel-1039221658
         * https://www.shutterstock.com/search/source+station
         * https://cdn1.iconfinder.com/data/icons/road-trip/90/work_in_progress-512.png
         */

        // Create the graphs for the virtual environment. Need to create them twice in order to keep a static one
        // on the vehicles. It was impossible to create a graph snapshot in the vehicles.
        ListenableGraph<LengthData> staticGraph = CityGraphCreator.createGraph(SimulatorSettings.CITY_SIZE, SimulatorSettings.VEHICLE_LENGTH);
        ListenableGraph<LengthData> dynamicGraph = CityGraphCreator.createGraph(SimulatorSettings.CITY_SIZE, SimulatorSettings.VEHICLE_LENGTH);

        // initialize a new Simulator instance
        final Simulator sim = Simulator.builder()
                // set the length of a simulation 'tick'
                .setTickLength(SimulatorSettings.TICK_LENGTH)
                .addModel(RandomModel.builder()
                        // set the random seed we use in this 'experiment'
                        //.withSeed(SimulatorSettings.RANDOM_SEED)
                )
                .addModel(RoadModelBuilders.dynamicGraph(dynamicGraph)
                        .withDistanceUnit(SI.METER)
                        .withModificationCheck(true)
                )
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(PizzeriaModel.builder())
                .addModel(StatsTracker.builder())
                // in case a GUI is not desired simply don't add it.
                .addModel(viewBuilder)
                .build();

        final RandomGenerator rng = sim.getRandomGenerator();
        final PizzeriaModel pizzeriaModel = sim.getModelProvider().getModel(PizzeriaModel.class);

        // Create pizzeria
        final Pizzeria pizzeria = pizzeriaModel.openPizzeria();

        // Create charging station
        final ChargingStation chargingStation = pizzeriaModel.openChargingStation();

        // Create robots
        for (int i = 0; i < SimulatorSettings.NUM_ROBOTS; i++) {
            VehicleDTO vdto = VehicleDTO.builder()
                    .capacity(SimulatorSettings.ROBOT_CAPACITY)
                    //.startPosition(pizzeria.getPosition())
                    .startPosition(pizzeria.getPosition())
                    .speed(SimulatorSettings.VEHICLE_SPEED_KMH)
                    .build();

            Battery battery = new Battery(SimulatorSettings.BATTERY_CAPACITY);

            // Robots start at the pizzeria
            sim.register(new RobotAgent(
                    getNextRobotID(), vdto, battery, staticGraph, pizzeria.getPosition(),
                    SimulatorSettings.ALTERNATIVE_PATHS_TO_EXPLORE, chargingStation.getPosition())
            );
        }

        // At every node, insert a ResourceAgent
        for (Point node : staticGraph.getNodes()) {
            pizzeriaModel.createResourceAgent(node);
        }

        // TickListener for creation of new delivery tasks
        sim.addTickListener(new TickListener() {
            @Override
            public void tick(@NotNull TimeLapse time) {
                if (rng.nextDouble() < SimulatorSettings.PROB_NEW_PARCEL) {
                    pizzeriaModel.createNewDeliveryTask(rng, SimulatorSettings.PIZZA_AMOUNT_MEAN, SimulatorSettings.PIZZA_AMOUNT_STD, time.getStartTime());
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {
            }
        });

        sim.addTickListener(new TickListener() {
            @Override
            public void tick(@NotNull TimeLapse timeLapse) {
                if (rng.nextDouble() < SimulatorSettings.PROB_NEW_ROAD_WORKS) {
                    pizzeriaModel.newRoadWorks(timeLapse.getEndTime());
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {

            }
        });

        // Start the simulation.
        sim.start();
    }

    public static int getNextRobotID() {
        return robotID++;
    }
}
