package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;


public class AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers {

    private static ListenableGraph<LengthData> graph;
    private static int chargingStationCapacity;
    private static double rechargeCapacity;
    private static long reservationLifetime;
    private static int nodeDistance;
    private static double robotSpeed;

    public static TimedEventHandler<AddDepotEvent> defaultHandler(
            ListenableGraph<LengthData> graph,
            int chargingStationCapacity,
            double rechargeCapacity,
            long reservationLifetime,
            int nodeDistance,
            double robotSpeed
    ) {
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.graph = graph;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.chargingStationCapacity = chargingStationCapacity;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.rechargeCapacity = rechargeCapacity;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.reservationLifetime = reservationLifetime;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.nodeDistance = nodeDistance;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.robotSpeed = robotSpeed;

        return Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddDepotEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddDepotEvent event, @NotNull SimulatorAPI sim) {
                PizzeriaModel pm = ((Simulator) sim).getModelProvider().getModel(PizzeriaModel.class);

                pm.createPizzeria(sim.getRandomGenerator());

                pm.createChargingStation(
                        sim.getRandomGenerator(),
                        chargingStationCapacity,
                        rechargeCapacity
                );

                for (Point p : graph.getNodes()) {
                    pm.createResourceAgent(
                            p,
                            reservationLifetime,
                            nodeDistance,
                            robotSpeed
                    );
                }
            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

