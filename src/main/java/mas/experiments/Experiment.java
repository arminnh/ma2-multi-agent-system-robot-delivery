package mas.experiments;

import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
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
import mas.statistics.StatsWriter;
import mas.tasks.DeliveryTask;
import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;
import java.util.List;

public class Experiment {
    public int id;
    private ExperimentParameters p;

    public Experiment(int id, ExperimentParameters params) {
        this.id = id;
        this.p = params;
    }

    private static Vehicles.VehicleGenerator getVehicleGenerator(
            int vehiclesAm, int vehicleCap, double vehicleSpeed
    ) {
        return Vehicles.builder()
                .numberOfVehicles(StochasticSuppliers.constant(vehiclesAm))
                .capacities(StochasticSuppliers.constant(vehicleCap))
                .speeds(StochasticSuppliers.constant(vehicleSpeed))
                .startPositions(StochasticSuppliers.constant(new Point(2, 2)))
                .build();
    }

    private static Depots.DepotGenerator getDepotGenerator() {
        return Depots.builder()
                .numerOfDepots(StochasticSuppliers.constant(1))
                .build();
    }

    private static Parcels.ParcelGenerator getDeliveryTaskAndRoadWorksGenerator(
            long tickLength, double probNewDeliveryTask, double probNewRoadWorks
    ) {
        return new DeliveryTaskAndRoadWorksGenerator(
                tickLength,
                probNewDeliveryTask,
                probNewRoadWorks
        );
    }

    void run() {
        final ListenableGraph<LengthData> staticGraph = CityGraphCreator.createGraph(p.citySize, p.robotLength);

//        if (p.probNewRoadWorks != 0 && p.repeat > 1) {
//            throw new IllegalArgumentException("Cannot have a probability for road works if repeat > 1 because " +
//                    "the RoadModel does not work as expected in later iterations.");
//        }

        View.Builder viewBuilder = View.builder()
                .withTitleAppendix("Pizza delivery multi agent system simulator")
                .withAutoPlay()
                .withSimulatorEndTime(p.simulationLength)
                .withAutoClose()
                .withSpeedUp(p.simSpeedUp)
                .with(GraphRoadModelRenderer.builder()
                        .withMargin(p.robotLength)
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
                .scenarioLength(p.simulationLength)
                .setStopCondition(StatsStopConditions.timeOutEvent())
                .vehicles(getVehicleGenerator(p.numRobots, p.robotCapacity, p.robotSpeed))
                .parcels(getDeliveryTaskAndRoadWorksGenerator(p.tickLength, p.probNewDeliveryTask, p.probNewRoadWorks))
                .depots(getDepotGenerator())
                .addModel(RoadModelBuilders.dynamicGraph(CityGraphCreator.createGraph(p.citySize, p.robotLength))
                        .withDistanceUnit(p.distanceUnit)
                        .withSpeedUnit(p.speedUnit)
                        .withModificationCheck(true)
                )
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

        // Starts the experiment builder.
        final Optional<ExperimentResults> results = com.github.rinde.rinsim.experiment.Experiment.builder()
                // Adds a configuration to the experiment. A configuration configures an algorithm that is supposed to
                // handle or 'solve' a problem specified by a scenario. A configuration can handle a scenario if it
                // contains an event handler for all events that occur in the scenario. The scenario in this example
                // contains four different events and registers an event handler for each of them.
                .addConfiguration(MASConfiguration.builder()
                        // NOTE: this example uses 'namedHandler's for Depots and Parcels, while very useful for
                        // debugging these should not be used in production as these are not thread safe.
                        // Use the 'defaultHandler()' instead.
                        .addEventHandler(AddDepotEvent.class, AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.defaultHandler(staticGraph, p.chargingStationCapacity, p.batteryRechargeCapacity, p.intentionReservationLifetime, p.nodeDistance, p.robotSpeed))

                        .addEventHandler(AddParcelEvent.class, AddDeliveryTaskAndRoadWorksEventHandlers.defaultHandler(p.pizzaAmountMean, p.pizzaAmountStd, p.timeRoadWorks))
                        // There is no default handle for vehicle events, here a non functioning handler is added,
                        // it can be changed to add a custom vehicle to the simulator.
                        .addEventHandler(AddVehicleEvent.class, AddRobotAgentEventHandlers.defaultHandler(p.robotCapacity, p.robotSpeed, p.batteryCapacity, p.batteryRescueDelay, staticGraph, p.alternativePathsToExplore, p.explorationRefreshTime, p.intentionRefreshTime))
                        .addEventHandler(TimeOutEvent.class, TimeOutStopper.stopHandler())
                        // Note: if your multi-agent system requires the aid of a model (e.g. CommModel) it can be added
                        // directly in the configuration. Models that are only used for the solution side should not
                        // be added in the scenario as they are not part of the problem.
                        .addModel(DefaultPDPModel.builder())
                        .addModel(CommModel.builder())
                        .addModel(PizzeriaModel.builder(p.tickLength, p.verbose))
                        .addModel(StatsTracker.builder())
                        .build()
                )

                // Adds the newly constructed scenario to the experiment.
                // Every configuration will be run on every scenario.
                .addScenarios(scenarios)

                // The number of repetitions for each simulation.
                // Each repetition will have a unique random seed that is given to the simulator.
                .repeat(p.repeat)

                // The master random seed from which all random seeds for the simulations will be drawn.
                .withRandomSeed(randomSeed)

                // The number of threads the experiment will use, this allows to run several simulations in parallel.
                // Note that when the GUI is used the number of threads must be set to 1.
                .withThreads(p.threads)

                // We add a post processor to the experiment. A post processor can read the state of the simulator
                // after it has finished. It can be used to gather simulation results. The objects created by the
                // post processor end up in the ExperimentResults object that is returned by the perform(..) method
                .usePostProcessor(new PizzaPostProcessor(this.id))

                .showGui(viewBuilder)
                .showGui(p.showGUI)

                // Starts the experiment, but first reads the command-line arguments that are specified for this
                // application. By supplying the '-h' option you can see an overview of the supported options.
                .perform(System.out);

        if (results.isPresent()) {
            StatsWriter.writeToJson(this.id, this.p, results.get().getResults());
        } else {
            throw new IllegalStateException("Experiment did not complete.");
        }
    }
}
