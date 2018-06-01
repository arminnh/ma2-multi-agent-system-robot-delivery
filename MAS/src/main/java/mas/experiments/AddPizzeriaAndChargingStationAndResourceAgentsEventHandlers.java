package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;


public class AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers {

    private static Point pizzeriaPosition;
    private static Point chargingStationPosition;
    static int chargingStationCapacity = 0;
    private static ListenableGraph<LengthData> graph;

    public static TimedEventHandler<AddDepotEvent> defaultHandler(
            ListenableGraph<LengthData> graph,
            Point pizzeriaPosition,
            Point chargingStationPosition,
            int chargingStationCapacity
    ) {
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.chargingStationCapacity = chargingStationCapacity;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.graph = graph;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.pizzeriaPosition = pizzeriaPosition;
        AddPizzeriaAndChargingStationAndResourceAgentsEventHandlers.chargingStationPosition = chargingStationPosition;

        return Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddDepotEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddDepotEvent event, @NotNull SimulatorAPI sim) {
                //sim.register(new Pizzeria(pizzeriaPosition));
                //sim.register(new ChargingStation(chargingStationPosition, chargingStationCapacity));

                PizzeriaModel pm = ((Simulator) sim).getModelProvider().getModel(PizzeriaModel.class);
                pm.openPizzeria();
                pm.openChargingStation();
                for (Point p : graph.getNodes()) {
                    pm.createResourceAgent(p);
                }
            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

