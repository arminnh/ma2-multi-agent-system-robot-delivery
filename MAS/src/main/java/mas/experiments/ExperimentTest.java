package mas.experiments;

import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
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
import com.github.rinde.rinsim.scenario.generator.*;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.base.Optional;
import mas.SimulatorSettings;
import mas.experiments.events.AddDeliveryTaskEvent;
import mas.experiments.events.AddPizzeriaEvent;
import mas.graphs.CityGraphCreator;
import mas.experiments.handlers.RobotVehicleHandler;
import mas.models.PizzeriaModel;
import mas.statistics.StatsTracker;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import javax.measure.unit.SI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExperimentTest {
    private static final double VEHICLE_SPEED = 1;
    private static final Point PIZZERIA_LOC = new Point(4, 2);
    private static final Point CHARGING_STATION_LOC = new Point(2, 2);
    private static final int CHARGING_STATION_CAP = 3;
    // TODO: CHANGE THESE VALUES
    private static final ListenableGraph<LengthData> STATIC_GRAPH = CityGraphCreator.createGraph(SimulatorSettings.CITY_SIZE, SimulatorSettings.VEHICLE_LENGTH);
    private static final ListenableGraph<LengthData> dynamicGraph = CityGraphCreator.createGraph(SimulatorSettings.CITY_SIZE, SimulatorSettings.VEHICLE_LENGTH);
    private static final double BATTERY_SIZE = SimulatorSettings.BATTERY_CAPACITY;

    public static final double PIZZA_AMOUNT_STD = 0.75;
    public static final double PIZZA_AMOUNT_MEAN = 4;

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

        Vehicles.VehicleGenerator vehicleGenerator = getVehicleGenerator(1,
                SimulatorSettings.ROBOT_CAPACITY, SimulatorSettings.VEHICLE_SPEED, PIZZERIA_LOC);

        LinkedList<Point> positions = new LinkedList<>();
        positions.add(PIZZERIA_LOC);
        positions.add(CHARGING_STATION_LOC);
        Depots.DepotGenerator depotGenerator = getDepotGenerator(positions);

        List<Point> availablePos = new LinkedList<>();
        for(Point node: STATIC_GRAPH.getNodes()){
            if(node.equals(PIZZERIA_LOC) || node.equals(CHARGING_STATION_LOC)){
                continue;
            }
            availablePos.add(node);
        }

        Parcels.ParcelGenerator parcelGenerator = getParcelGenerator(availablePos);

        ScenarioGenerator generator = ScenarioGenerator.builder()
                .scenarioLength(1000)
                .setStopCondition(StatsStopConditions.timeOutEvent())
                .vehicles(vehicleGenerator)
                .parcels(parcelGenerator)
                .depots(depotGenerator)
                .addModel(TimeModel.builder().withTickLength(SimulatorSettings.TICK_LENGTH))
                .addModel(PizzeriaModel.builder())
                .addModel(RoadModelBuilders.dynamicGraph(dynamicGraph)
                        .withDistanceUnit(SI.METER)
                        .withModificationCheck(true))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .build();

        List<Scenario> scenarios = new ArrayList<>();
        int numberOfDesiredScenarios = 1;

        RandomGenerator rng = new MersenneTwister(123L);

        for(int i = 0; i < numberOfDesiredScenarios; i++){
            scenarios.add(generator.generate(rng, "?"));
        }

        final Optional<ExperimentResults> results;
        // Starts the experiment builder.
        results = Experiment.builder()
                // Adds a configuration to the experiment. A configuration configures an
                // algorithm that is supposed to handle or 'solve' a problem specified by
                // a scenario. A configuration can handle a scenario if it contains an
                // event handler for all events that occur in the scenario. The scenario
                // in this example contains four different events and registers an event
                // handler for each of them.
                .addConfiguration(MASConfiguration.builder()
                        // NOTE: this example uses 'namedHandler's for Depots and Parcels, while
                        // very useful for debugging these should not be used in production as
                        // these are not thread safe. Use the 'defaultHandler()' instead.
                        .addEventHandler(AddDepotEvent.class, AddPizzeriaEvent.defaultHandler(CHARGING_STATION_CAP, STATIC_GRAPH, PIZZERIA_LOC, CHARGING_STATION_LOC))

                        .addEventHandler(AddParcelEvent.class, AddDeliveryTaskEvent.defaultHandler(STATIC_GRAPH, PIZZERIA_LOC, CHARGING_STATION_LOC))
                        // There is no default handle for vehicle events, here a non functioning
                        // handler is added, it can be changed to add a custom vehicle to the
                        // simulator.
                        .addEventHandler(AddVehicleEvent.class,
                                RobotVehicleHandler.defaultHandler(PIZZERIA_LOC, CHARGING_STATION_LOC, STATIC_GRAPH, BATTERY_SIZE)
                        )
                        .addEventHandler(TimeOutEvent.class, TimeOutEvent.ignoreHandler())
                        // Note: if you multi-agent system requires the aid of a model (e.g.
                        // CommModel) it can be added directly in the configuration. Models that
                        // are only used for the solution side should not be added in the
                        // scenario as they are not part of the problem.
                        .addModel(StatsTracker.builder())
                        .build())

                // Adds the newly constructed scenario to the experiment. Every
                // configuration will be run on every scenario.
                .addScenarios(scenarios)
                // The number of repetitions for each simulation. Each repetition will
                // have a unique random seed that is given to the simulator.
                .repeat(5)

                // The master random seed from which all random seeds for the
                // simulations will be drawn.
                .withRandomSeed(0)

                // The number of threads the experiment will use, this allows to run
                // several simulations in parallel. Note that when the GUI is used the
                // number of threads must be set to 1.
                .withThreads(1)

                // We add a post processor to the experiment. A post processor can read
                // the state of the simulator after it has finished. It can be used to
                // gather simulation results. The objects created by the post processor
                // end up in the ExperimentResults object that is returned by the
                // perform(..) method of Experiment.
                //.usePostProcessor(new ExamplePostProcessor())

                // Starts the experiment, but first reads the command-line arguments
                // that are specified for this application. By supplying the '-h' option
                // you can see an overview of the supported options.
                .perform(System.out, arguments);

        if (results.isPresent()) {
            for (final Experiment.SimulationResult sr : results.get().getResults()) {
                // The SimulationResult contains all information about a specific
                // simulation, the result object is the object created by the post
                // processor, a String in this case.
                System.out.println(
                        sr.getSimArgs().getRandomSeed() + " " + sr.getResultObject());
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
     * @return A newly constructed scenario.
     */
    public static Vehicles.VehicleGenerator getVehicleGenerator(Integer vehiclesAm,
                                                                Integer vehicleCap,
                                                                Double vehicleSpeed,
                                                                Point pizzeriaPos) {
        return Vehicles.builder()
                .numberOfVehicles(StochasticSuppliers.constant(vehiclesAm))
                .capacities(StochasticSuppliers.constant(vehicleCap))
                .speeds(StochasticSuppliers.constant(vehicleSpeed))
                .startPositions(StochasticSuppliers.constant(pizzeriaPos)).build();
    }

    public static Depots.DepotGenerator getDepotGenerator(LinkedList<Point> points) {

        return Depots.builder()
                .numerOfDepots(StochasticSuppliers.constant(1))
                .positions(StochasticSuppliers.fromIterable(points))
                .build();
    }

    public static Parcels.ParcelGenerator getParcelGenerator(List<Point> p) {
        return new TestParcels(p);

        /*return Parcels.builder()
               .locations(new PizzaLocations(p))
               .neededCapacities(StochasticSuppliers.normal().mean(PIZZA_AMOUNT_MEAN).std(PIZZA_AMOUNT_STD).buildInteger())
               .build();*/
    }
}


