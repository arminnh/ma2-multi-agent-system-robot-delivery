package mas.experiments;

import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.*;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.generator.Depots;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.Vehicles;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Optional;
import com.google.devtools.common.options.Option;
import mas.SimulatorSettings;
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
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;
import java.util.*;

import com.google.devtools.common.options.OptionsParser;

public class Experiments {
    private static double robotSpeed = SimulatorSettings.ROBOT_SPEED;
    private static int robotCapacity = SimulatorSettings.ROBOT_CAPACITY;
    private static int chargingStationCapacity = SimulatorSettings.CHARGING_STATION_ROBOT_CAPACITY;
    private static double batteryCapacity = SimulatorSettings.BATTERY_CAPACITY;
    private static double batteryRechargeCapacity = SimulatorSettings.BATTERY_RECHARGE_CAPACITY;
    private static int alternativePathsToExplore = SimulatorSettings.ALTERNATIVE_PATHS_TO_EXPLORE;
    private static long timeRoadWorks = SimulatorSettings.TIME_ROAD_WORKS;
    private static long batteryRescueDelay = SimulatorSettings.BATTERY_RESCUE_DELAY;
    private static long intentionReservationLifetime = SimulatorSettings.INTENTION_RESERVATION_LIFETIME;
    private static long explorationRefreshTime = SimulatorSettings.EXPLORATION_REFRESH_TIME;
    private static long intentionRefreshTime = SimulatorSettings.INTENTION_REFRESH_TIME;
    private static double probNewDeliveryTask = SimulatorSettings.PROB_NEW_DELIVERY_TASK;
    private static double probNewRoadWorks = SimulatorSettings.PROB_NEW_ROAD_WORKS;
    private static double pizzaAmountStd = SimulatorSettings.PIZZA_AMOUNT_STD;
    private static double pizzaAmountMean = SimulatorSettings.PIZZA_AMOUNT_MEAN;

    private static long tickLength = SimulatorSettings.TICK_LENGTH;
    private static int citySize = SimulatorSettings.CITY_SIZE;
    private static int numRobots = SimulatorSettings.NUM_ROBOTS;
    private static int robotLength = SimulatorSettings.ROBOT_LENGTH;
    private static final ListenableGraph<LengthData> staticGraph = CityGraphCreator.createGraph(citySize, robotLength);
    private static final ListenableGraph<LengthData> dynamicGraph = CityGraphCreator.createGraph(citySize, robotLength);
    private static int nodeDistance = SimulatorSettings.NODE_DISTANCE;
    private static Unit<Length> distanceUnit = SimulatorSettings.DISTANCE_UNIT;
    private static Unit<Velocity> speedUnit = SimulatorSettings.SPEED_UNIT;
    private static int repeat;
    private static int simSpeedUp;
    private static boolean showGUI;
    
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

    private static void printUsage(OptionsParser parser) {
        System.out.println("Usage: java -jar experiments.jar OPTIONS");
        System.out.println(parser.describeOptions(Collections.<String, String>emptyMap(),
                OptionsParser.HelpVerbosity.LONG));
    }
    public static void main(String[] args) {

        OptionsParser parser = OptionsParser.newOptionsParser(ExperimentsOptions.class);
        parser.parseAndExitUponError(args);
        ExperimentsOptions options = parser.getOptions(ExperimentsOptions.class);
        if(options.help){
            printUsage(parser);
            return;
        }

        assignOptions(options);
        System.out.println("numRobots = " + numRobots);

        //int uiSpeedUp = 1;
        //String[] arguments = args;


        // SOME MEMBERS CAN BE SET USING ARGUMENTS
        // numRobots
        // citySize
        // chargingStationCapacity
        // probNewDeliveryTask
        // probNewRoadWorks
        // alternativePathsToExplore
//        probNewRoadWorks = 0;

        long simulationLength = 30 * 60 * 1000;

        View.Builder viewBuilder = View.builder()
                .withTitleAppendix("Pizza delivery multi agent system simulator")
                .withAutoPlay()
                .withSimulatorEndTime(simulationLength)
                .withAutoClose()
                .withSpeedUp(simSpeedUp)
                .with(GraphRoadModelRenderer.builder()
                        .withMargin(robotLength)
                )
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(RobotAgent.class, "/robot.png")
                        .withImageAssociation(Pizzeria.class, "/pizzeria.png")
                        .withImageAssociation(ChargingStation.class, "/charging_station.png")
                        .withImageAssociation(DeliveryTask.class, "/graphics/flat/person-black-32.png")
                        .withImageAssociation(RoadWorks.class, "/road_works.png")
                )
                .with(CommRenderer.builder().withMessageCount())
                .with(DeliveryTaskRenderer.builder())
                .with(RobotRenderer.builder())
                .with(StatsPanel.builder())
                .withResolution(SimulatorSettings.WINDOW_WIDTH, SimulatorSettings.WINDOW_HEIGHT);

        ScenarioGenerator generator = ScenarioGenerator.builder()
                .scenarioLength(simulationLength)
                .setStopCondition(StatsStopConditions.timeOutEvent())
                .vehicles(getVehicleGenerator(numRobots, robotCapacity, robotSpeed))
                .parcels(getDeliveryTaskAndRoadWorksGenerator())
                .depots(getDepotGenerator())
                .addModel(TimeModel.builder().withTickLength(tickLength))
                .addModel(RoadModelBuilders.dynamicGraph(dynamicGraph)
                        .withDistanceUnit(distanceUnit)
                        .withSpeedUnit(speedUnit)
                        .withModificationCheck(true)
                )
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(PizzeriaModel.builder())
                .build();

        // The seed is strong
        long randomSeed = System.currentTimeMillis();
        List<Scenario> scenarios = new ArrayList<>();
        int numberOfDesiredScenarios = 1;
        for (int i = 0; i < numberOfDesiredScenarios; i++) {
            scenarios.add(generator.generate(
                    new MersenneTwister(randomSeed),
                    "Scenario " + Integer.toString(i + 1)
            ));
        }

        final Optional<ExperimentResults> results;

        // Starts the experiment builder.
        results = Experiment.builder()
                // Adds a configuration to the experiment. A configuration configures an algorithm that is supposed to
                // handle or 'solve' a problem specified by a scenario. A configuration can handle a scenario if it
                // contains an event handler for all events that occur in the scenario. The scenario in this example
                // contains four different events and registers an event handler for each of them.
                .addConfiguration(MASConfiguration.builder()
                        // NOTE: this example uses 'namedHandler's for Depots and Parcels, while very useful for
                        // debugging these should not be used in production as these are not thread safe.
                        // Use the 'defaultHandler()' instead.
                        .addEventHandler(AddDepotEvent.class, AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.defaultHandler(staticGraph, chargingStationCapacity, batteryRechargeCapacity, intentionReservationLifetime, nodeDistance, robotSpeed))

                        .addEventHandler(AddParcelEvent.class, AddDeliveryTaskAndRoadWorksEventHandlers.defaultHandler(pizzaAmountMean, pizzaAmountStd, timeRoadWorks))
                        // There is no default handle for vehicle events, here a non functioning handler is added,
                        // it can be changed to add a custom vehicle to the simulator.
                        .addEventHandler(AddVehicleEvent.class, AddRobotAgentEventHandlers.defaultHandler(robotCapacity, robotSpeed, batteryCapacity, batteryRescueDelay, staticGraph, alternativePathsToExplore, explorationRefreshTime, intentionRefreshTime))
                        .addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
                        // Note: if your multi-agent system requires the aid of a model (e.g. CommModel) it can be added
                        // directly in the configuration. Models that are only used for the solution side should not
                        // be added in the scenario as they are not part of the problem.
                        .addModel(StatsTracker.builder())
                        .build()
                )

                // Adds the newly constructed scenario to the experiment.
                // Every configuration will be run on every scenario.
                .addScenarios(scenarios)

                // The number of repetitions for each simulation.
                // Each repetition will have a unique random seed that is given to the simulator.
                .repeat(repeat)

                // The master random seed from which all random seeds for the simulations will be drawn.
                .withRandomSeed(randomSeed)

                // The number of threads the experiment will use, this allows to run several simulations in parallel.
                // Note that when the GUI is used the number of threads must be set to 1.
                .withThreads(1)

                // We add a post processor to the experiment. A post processor can read the state of the simulator
                // after it has finished. It can be used to gather simulation results. The objects created by the
                // post processor end up in the ExperimentResults object that is returned by the perform(..) method
                .usePostProcessor(new PizzaPostProcessor())

                .showGui(viewBuilder)
                .showGui(showGUI)

                // Starts the experiment, but first reads the command-line arguments that are specified for this
                // application. By supplying the '-h' option you can see an overview of the supported options.
                .perform(System.out);

        if (results.isPresent()) {
            for (final Experiment.SimulationResult sr : results.get().getResults()) {
                // The SimulationResult contains all information about a specific simulation,
                // the result object is the object created by the post processor, a String in this case.
                System.out.println(sr.getSimArgs().getScenario().getProblemInstanceId() + " " + sr.getResultObject());
            }
        } else {
            throw new IllegalStateException("Experiment did not complete.");
        }
    }

    public static Vehicles.VehicleGenerator getVehicleGenerator(int vehiclesAm, int vehicleCap, double vehicleSpeed) {
        return Vehicles.builder()
                .numberOfVehicles(StochasticSuppliers.constant(vehiclesAm))
                .capacities(StochasticSuppliers.constant(vehicleCap))
                .speeds(StochasticSuppliers.constant(vehicleSpeed))
                .startPositions(StochasticSuppliers.constant(new Point(2, 2)))
                .build();
    }

    public static Depots.DepotGenerator getDepotGenerator() {
        return Depots.builder()
                .numerOfDepots(StochasticSuppliers.constant(1))
                .build();
    }

    public static Parcels.ParcelGenerator getDeliveryTaskAndRoadWorksGenerator() {
        return new DeliveryTaskAndRoadWorksGenerator(
                tickLength,
                probNewDeliveryTask,
                probNewRoadWorks
        );
    }
}


