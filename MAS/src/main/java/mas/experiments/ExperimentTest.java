package mas.experiments;

import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import com.github.rinde.rinsim.examples.experiment.ExamplePostProcessor;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.ExperimentResults;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.pdptw.common.StatsStopConditions;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.generator.Depots;
import com.github.rinde.rinsim.scenario.generator.Parcels;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator;
import com.github.rinde.rinsim.scenario.generator.Vehicles;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Optional;
import mas.SimulatorSettings;
import mas.graphs.CityGraphCreator;
import mas.models.PizzeriaModel;
import mas.statistics.StatsTracker;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.Unit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ExperimentTest {
    // TODO: CHANGE THESE VALUES

    private static long tickLength = SimulatorSettings.TICK_LENGTH;

    private static int citySize = SimulatorSettings.CITY_SIZE;
    private static int numRobots = SimulatorSettings.NUM_ROBOTS;
    private static int robotLength = SimulatorSettings.ROBOT_LENGTH;
    private static final double robotSpeed = SimulatorSettings.ROBOT_SPEED;
    private static Unit<Length> distanceUnit = SimulatorSettings.DISTANCE_UNIT;
    private static Unit<Velocity> speedUnit = SimulatorSettings.SPEED_UNIT;
    private static final int robotCapacity = SimulatorSettings.ROBOT_CAPACITY;
    private static final int chargingStationCapacity = SimulatorSettings.CHARGING_STATION_CAPACITY;
    private static final double batteryCapacity = SimulatorSettings.BATTERY_CAPACITY;
    private static final double batteryChargeCapacity = SimulatorSettings.BATTERY_CHARGE_CAPACITY;
    private static final int alternativePathsToExplore = SimulatorSettings.ALTERNATIVE_PATHS_TO_EXPLORE;

    private static final double timeRoadWorks = SimulatorSettings.TIME_ROAD_WORKS;
    private static final double batteryRescueDelay = SimulatorSettings.BATTERY_RESCUE_DELAY;
    private static final double intentionReservationLifetime = SimulatorSettings.INTENTION_RESERVATION_LIFETIME;
    private static final double refreshIntentions = SimulatorSettings.REFRESH_INTENTIONS;
    private static final double refreshExplorations = SimulatorSettings.REFRESH_EXPLORATIONS;

    private static final double probNewDeliveryTask = SimulatorSettings.PROB_NEW_DELIVERY_TASK;
    private static final double probNewRoadWorks = SimulatorSettings.PROB_NEW_ROAD_WORKS;
    private static final double pizzaAmountStd = SimulatorSettings.PIZZA_AMOUNT_STD;
    private static final double pizzaAmountMean = SimulatorSettings.PIZZA_AMOUNT_MEAN;

    private static final Point pizzeriaPosition = new Point(4, 2);
    private static final Point chargingStationPosition = new Point(2, 2);
    private static final ListenableGraph<LengthData> staticGraph = CityGraphCreator.createGraph(citySize, robotLength);
    private static final ListenableGraph<LengthData> dynamicGraph = CityGraphCreator.createGraph(citySize, robotLength);

    private static final Point P1_PICKUP = new Point(1, 2);
    private static final Point P1_DELIVERY = new Point(4, 2);
    private static final Point P2_PICKUP = new Point(1, 1);
    private static final Point P2_DELIVERY = new Point(4, 1);
    private static final Point P3_PICKUP = new Point(1, 3);
    private static final Point P3_DELIVERY = new Point(4, 3);

    private static final long M1 = 60 * 1000L;
    private static final long M4 = 4 * 60 * 1000L;
    private static final long M5 = 5 * 60 * 1000L;
    private static final long M7 = 7 * 60 * 1000L;
    private static final long M10 = 10 * 60 * 1000L;
    private static final long M12 = 12 * 60 * 1000L;
    private static final long M13 = 13 * 60 * 1000L;
    private static final long M18 = 18 * 60 * 1000L;
    private static final long M20 = 20 * 60 * 1000L;
    private static final long M25 = 25 * 60 * 1000L;
    private static final long M30 = 30 * 60 * 1000L;
    private static final long M40 = 40 * 60 * 1000L;
    private static final long END_TIME = 60 * 60 * 1000L;

    public static void main(String[] args) {
        int uiSpeedUp = 1;
        String[] arguments = args;

        LinkedList<Point> positions = new LinkedList<>(Arrays.asList(pizzeriaPosition, chargingStationPosition));

        List<Point> availablePos = staticGraph.getNodes().stream()
                .filter(n -> !n.equals(pizzeriaPosition) && !n.equals(chargingStationPosition))
                .collect(Collectors.toList());

        ScenarioGenerator generator = ScenarioGenerator.builder()
                .scenarioLength(1000 * 1000)
                .setStopCondition(StatsStopConditions.timeOutEvent())
                .vehicles(getVehicleGenerator(
                        1, robotCapacity, robotSpeed, pizzeriaPosition
                ))
                .parcels(getParcelGenerator(availablePos))
                .depots(getDepotGenerator(positions))
                .addModel(TimeModel.builder().withTickLength(tickLength))
                .addModel(RandomModel.builder())
                .addModel(RoadModelBuilders.dynamicGraph(dynamicGraph)
                        .withDistanceUnit(distanceUnit)
                        .withSpeedUnit(speedUnit)
                        .withModificationCheck(true))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(PizzeriaModel.builder())
                .build();

        List<Scenario> scenarios = new ArrayList<>();
        int numberOfDesiredScenarios = 1;

        int scenarioID = 1;
        RandomGenerator rng = new MersenneTwister(123L);
        for (int i = 0; i < numberOfDesiredScenarios; i++) {
            scenarios.add(generator.generate(
                    new MersenneTwister(rng.nextLong()),
                    "Scenario " + Integer.toString(scenarioID++)
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
                        .addEventHandler(AddDepotEvent.class, AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.defaultHandler(staticGraph, pizzeriaPosition, chargingStationPosition, chargingStationCapacity))

                        .addEventHandler(AddParcelEvent.class, AddDeliveryTaskEventHandlers.defaultHandler(pizzaAmountMean, pizzaAmountStd))
                        // There is no default handle for vehicle events, here a non functioning handler is added,
                        // it can be changed to add a custom vehicle to the simulator.
                        .addEventHandler(AddVehicleEvent.class, AddRobotAgentEventHandlers.defaultHandler(dynamicGraph, pizzeriaPosition, chargingStationPosition, batteryCapacity, alternativePathsToExplore))
                        .addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
                        // Note: if youe multi-agent system requires the aid of a model (e.g. CommModel) it can be added
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
                .repeat(1)

                // The master random seed from which all random seeds for the simulations will be drawn.
                .withRandomSeed(1234567890)

                // The number of threads the experiment will use, this allows to run several simulations in parallel.
                // Note that when the GUI is used the number of threads must be set to 1.
                .withThreads(1)

                // We add a post processor to the experiment. A post processor can read the state of the simulator
                // after it has finished. It can be used to gather simulation results. The objects created by the
                // post processor end up in the ExperimentResults object that is returned by the perform(..) method
                .usePostProcessor(new PizzaPostProcessor())
                // Starts the experiment, but first reads the command-line arguments that are specified for this
                // application. By supplying the '-h' option you can see an overview of the supported options.
                .perform(System.out, arguments);

        if (results.isPresent()) {
            for (final Experiment.SimulationResult sr : results.get().getResults()) {
                // The SimulationResult contains all information about a specific simulation,
                // the result object is the object created by the post processor, a String in this case.
                System.out.println(sr.getSimArgs().getRandomSeed() + " " + sr.getResultObject());
            }
        } else {
            throw new IllegalStateException("Experiment did not complete.");
        }
    }

    /**
     * Defines a simple scenario with one depot, one vehicle and three parcels.
     * Note that a scenario is supposed to only contain problem specific
     * information it should (generally) not make any assumptions about the
     * algorithm(s) that are used to solve the problem.
     *
     * @return A newly constructed scenario.
     */
    public static Vehicles.VehicleGenerator getVehicleGenerator(int vehiclesAm, int vehicleCap,
                                                                double vehicleSpeed, Point pizzeriaPos) {
        return Vehicles.builder()
                .numberOfVehicles(StochasticSuppliers.constant(vehiclesAm))
                .capacities(StochasticSuppliers.constant(vehicleCap))
                .speeds(StochasticSuppliers.constant(vehicleSpeed))
                .startPositions(StochasticSuppliers.constant(pizzeriaPos))
                .build();
    }

    public static Depots.DepotGenerator getDepotGenerator(LinkedList<Point> points) {

        return Depots.builder()
                .numerOfDepots(StochasticSuppliers.constant(1))
                .positions(StochasticSuppliers.fromIterable(points))
                .build();
    }

    public static Parcels.ParcelGenerator getParcelGenerator(List<Point> p) {
        return new DeliveryTaskGenerator(tickLength, probNewDeliveryTask);
    }
}


