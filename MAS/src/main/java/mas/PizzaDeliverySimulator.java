package mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.StatsPanel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import mas.agents.Battery;
import mas.agents.ResourceAgent;
import mas.agents.RobotAgent;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.graphs.CityGraphCreator;
import mas.models.PizzeriaModel;
import mas.renderers.DeliveryTaskRenderer;
import mas.renderers.RobotRenderer;
import mas.statistics.StatsTracker;
import mas.tasks.DeliveryTask;
import org.apache.commons.math3.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

import javax.measure.unit.SI;

public class PizzaDeliverySimulator{

    private static int robotID = 1;
    private static int pizzaParcelID = 1;


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
                .withSpeedUp(SimulatorSettings.SIM_SPEEDUP)
                .with(GraphRoadModelRenderer.builder()
                        .withMargin(SimulatorSettings.VEHICLE_LENGTH)
                )
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(RobotAgent.class, "/robot.png")
                        .withImageAssociation(Pizzeria.class, "/pizzeria.png")
                        .withImageAssociation(ChargingStation.class, "/charging_station.png")
                        .withImageAssociation(DeliveryTask.class, "/graphics/flat/person-black-32.png")
                )
                .with(CommRenderer.builder().withMessageCount()
                        //.withReliabilityColors()
                        //.withToString()
                        //.withMessageCount())
                )
                .with(DeliveryTaskRenderer.builder())
                .with(RobotRenderer.builder())
                .with(StatsPanel.builder())
                .withResolution(800, 600);
        /*
         * Image sources:
         * https://www.shutterstock.com/image-vector/line-pixel-style-classic-robot-rectangle-488817829
         * https://www.shutterstock.com/image-vector/pizza-delivery-service-icon-set-pixel-1039221658
         * https://www.shutterstock.com/search/source+station
         */

        // initialize a new Simulator instance
        final Simulator sim = Simulator.builder()
                // set the length of a simulation 'tick'
                .setTickLength(SimulatorSettings.TICK_LENGTH)
                // set the random seed we use in this 'experiment'
                //.setRandomSeed(RANDOM_SEED)
                .addModel(RoadModelBuilders.dynamicGraph(CityGraphCreator.createGraph(5, SimulatorSettings.VEHICLE_LENGTH))
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
        final GraphRoadModel graph = sim.getModelProvider().getModel(GraphRoadModel.class);
        final PizzeriaModel pizzeriaModel = sim.getModelProvider().getModel(PizzeriaModel.class);
        pizzeriaModel.setSimulator(sim, rng);

        final StatsTracker statsTracker = sim.getModelProvider().getModel(StatsTracker.class);
        statsTracker.addDeliveryTaskModelListener(pizzeriaModel);

        // Create pizzeria
        final Pizzeria pizzeria = pizzeriaModel.openPizzeria();

        // Create charging station
        ChargingStation chargingStation = new ChargingStation(
                roadModel.getRandomPosition(sim.getRandomGenerator()),
                new Double(SimulatorSettings.NUM_ROBOTS * 0.3).intValue()
        );
        sim.register(chargingStation);

        // Create robots
        for (int i = 0; i < SimulatorSettings.NUM_ROBOTS; i++) {
            VehicleDTO vdto = VehicleDTO.builder()
                    .capacity(SimulatorSettings.ROBOT_CAPACITY)
                    //.startPosition(pizzeria.getPosition())
                    .startPosition(pizzeria.getPosition())//roadModel.getRandomPosition(rng))
                    .speed(SimulatorSettings.VEHICLE_SPEED_KMH)
                    .build();

            Battery battery = new Battery(SimulatorSettings.BATTERY_CAPACITY);

            // Robots start at the pizzeria
            sim.register(new RobotAgent(
                    vdto, battery, getNextRobotID(), pizzeria.getPosition(),
                    SimulatorSettings.ALTERNATIVE_PATHS_TO_EXPLORE, chargingStation.getPosition()
            ));
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

        // At every node, insert a ResourceAgent
        for (Point node : graph.getGraph().getNodes()) {
            sim.register(new ResourceAgent(node, sim.getRandomGenerator()));
        }

        // Start the simulation.
        sim.start();
    }

    public static int getNextRobotID() {
        return robotID++;
    }
}
