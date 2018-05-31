package mas.experiments;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.agents.Battery;
import mas.agents.RobotAgent;
import org.jetbrains.annotations.NotNull;

public class AddRobotAgentEventHandlers {

    static private int id = 1;
    static private int pathsToExplore;
    static private Point pizzeriaPosition;
    static private Point chargingStationPosition;
    static private ListenableGraph<LengthData> staticGraph;
    static private double batterySize;

    public static TimedEventHandler<AddVehicleEvent> defaultHandler(
            ListenableGraph<LengthData> graph,
            Point pizzeriaPosition,
            Point chargingStationPosition,
            double batterySize,
            int pathsToExplore
    ) {
        AddRobotAgentEventHandlers.pizzeriaPosition = pizzeriaPosition;
        AddRobotAgentEventHandlers.chargingStationPosition = chargingStationPosition;
        AddRobotAgentEventHandlers.staticGraph = graph;
        AddRobotAgentEventHandlers.batterySize = batterySize;
        AddRobotAgentEventHandlers.pathsToExplore = pathsToExplore;

        return AddRobotAgentEventHandlers.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddVehicleEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddVehicleEvent event, @NotNull SimulatorAPI sim) {
                sim.register(new RobotAgent(
                        id++,
                        event.getVehicleDTO(),
                        new Battery(batterySize),
                        staticGraph,
                        pizzeriaPosition,
                        pathsToExplore,
                        chargingStationPosition
                ));
            }

            public String toString() {
                return AddVehicleEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }
    }
}
