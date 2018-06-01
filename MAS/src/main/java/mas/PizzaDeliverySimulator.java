package mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
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
import com.google.devtools.common.options.OptionsParser;
import mas.agents.RobotAgent;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.buildings.RoadWorks;
import mas.experiments.ExperimentsOptions;
import mas.graphs.CityGraphCreator;
import mas.models.PizzeriaModel;
import mas.renderers.DeliveryTaskRenderer;
import mas.renderers.RobotRenderer;
import mas.statistics.StatsTracker;
import mas.tasks.DeliveryTask;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class PizzaDeliverySimulator {


    private static int alternativePathsToExplore;
    private static int citySize;
    private static int chargingStationCapacity;
    private static int repeat;
    private static int numRobots;
    private static double probNewDeliveryTask;
    private static double probNewRoadWorks;
    private static boolean showGUI;
    private static int simSpeedUp;

    private static void printUsage(OptionsParser parser) {
        System.out.println("Usage: java -jar MAR.jar OPTIONS");
        System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }

    private static void assignOptions(ExperimentsOptions options){
        alternativePathsToExplore = options.alternativePaths;
        chargingStationCapacity = options.chargingStationCapacity;
        citySize = options.citySize;
        repeat = options.repeat;
        numRobots = options.numRobots;
        probNewDeliveryTask = options.probNewDeliveryTask;
        probNewRoadWorks = options.probNewRoadWorks;
        showGUI = options.showGUI;
        simSpeedUp = options.simSpeedUp;
    }

    /**
     * @param args - No args.
     */
    public static void main(String[] args) {

        OptionsParser parser = OptionsParser.newOptionsParser(ExperimentsOptions.class);
        parser.parseAndExitUponError(args);
        ExperimentsOptions options = parser.getOptions(ExperimentsOptions.class);
        if(options.help){
            printUsage(parser);
            return;
        }
        assignOptions(options);

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
                .withSpeedUp(simSpeedUp)
                .with(GraphRoadModelRenderer.builder()
                        .withMargin(SimulatorSettings.ROBOT_LENGTH)
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
        ListenableGraph<LengthData> staticGraph = CityGraphCreator.createGraph(citySize, SimulatorSettings.ROBOT_LENGTH);
        ListenableGraph<LengthData> dynamicGraph = CityGraphCreator.createGraph(citySize, SimulatorSettings.ROBOT_LENGTH);
        Simulator sim = null;
        // initialize a new Simulator instance
        if(showGUI){
            sim = Simulator.builder()
                    // set the length of a simulation 'tick'
                    .setTickLength(SimulatorSettings.TICK_LENGTH)
                    .addModel(RandomModel.builder()
                            // set the random seed we use in this 'experiment'
                            .withSeed(System.currentTimeMillis())
                    )
                    .addModel(RoadModelBuilders.dynamicGraph(dynamicGraph)
                            .withDistanceUnit(SimulatorSettings.DISTANCE_UNIT)
                            .withSpeedUnit(SimulatorSettings.SPEED_UNIT)
                            .withModificationCheck(true)
                    )
                    .addModel(DefaultPDPModel.builder())
                    .addModel(CommModel.builder())
                    .addModel(PizzeriaModel.builder())
                    .addModel(StatsTracker.builder())
                    // in case a GUI is not desired simply don't add it.
                    .addModel(viewBuilder)
                    .build();
        }else{
            Simulator.builder()
                    // set the length of a simulation 'tick'
                    .setTickLength(SimulatorSettings.TICK_LENGTH)
                    .addModel(RandomModel.builder()
                            // set the random seed we use in this 'experiment'
                            .withSeed(System.currentTimeMillis())
                    )
                    .addModel(RoadModelBuilders.dynamicGraph(dynamicGraph)
                            .withDistanceUnit(SimulatorSettings.DISTANCE_UNIT)
                            .withSpeedUnit(SimulatorSettings.SPEED_UNIT)
                            .withModificationCheck(true)
                    )
                    .addModel(DefaultPDPModel.builder())
                    .addModel(CommModel.builder())
                    .addModel(PizzeriaModel.builder())
                    .addModel(StatsTracker.builder())
                    // in case a GUI is not desired simply don't add it.
                    .build();
        }

        final RandomGenerator rng = sim.getRandomGenerator();
        final PizzeriaModel pizzeriaModel = sim.getModelProvider().getModel(PizzeriaModel.class);

        // Create pizzeria and charging station
        pizzeriaModel.createPizzeria(rng);
        pizzeriaModel.createChargingStation(
                rng,
                chargingStationCapacity,
                SimulatorSettings.BATTERY_RECHARGE_CAPACITY
        );

        // At every node in the graph, insert a ResourceAgent
        for (Point node : staticGraph.getNodes()) {
            pizzeriaModel.createResourceAgent(
                    node,
                    SimulatorSettings.INTENTION_RESERVATION_LIFETIME,
                    SimulatorSettings.NODE_DISTANCE,
                    SimulatorSettings.ROBOT_SPEED
            );
        }

        // Create robots
        for (int i = 0; i < numRobots; i++) {
            // Robots start at the pizzeria
            pizzeriaModel.createRobot(
                    SimulatorSettings.ROBOT_CAPACITY,
                    SimulatorSettings.ROBOT_SPEED,
                    SimulatorSettings.BATTERY_CAPACITY,
                    SimulatorSettings.BATTERY_RESCUE_DELAY,
                    staticGraph,
                    alternativePathsToExplore,
                    SimulatorSettings.EXPLORATION_REFRESH_TIME,
                    SimulatorSettings.INTENTION_REFRESH_TIME
            );
        }

        // TickListener for creation of new delivery tasks
        sim.addTickListener(new TickListener() {
            @Override
            public void tick(@NotNull TimeLapse time) {
                if (rng.nextDouble() < probNewDeliveryTask) {
                    pizzeriaModel.createDeliveryTask(
                            rng,
                            SimulatorSettings.PIZZA_AMOUNT_MEAN,
                            SimulatorSettings.PIZZA_AMOUNT_STD,
                            time.getStartTime()
                    );
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {
            }
        });

        sim.addTickListener(new TickListener() {
            @Override
            public void tick(@NotNull TimeLapse timeLapse) {
                if (rng.nextDouble() < probNewRoadWorks) {
                    pizzeriaModel.createRoadWorks(
                            rng,
                            timeLapse.getEndTime() + SimulatorSettings.TIME_ROAD_WORKS
                    );
                }
            }

            @Override
            public void afterTick(@NotNull TimeLapse timeLapse) {

            }
        });

        // Start the simulation.
        sim.start();
    }
}
