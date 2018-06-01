package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.agents.Battery;
import mas.agents.RobotAgent;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;

public class AddRobotAgentEventHandlers {

    static private int id = 1;
    static private int pathsToExplore;
    static private Point pizzeriaPosition;
    static private Point chargingStationPosition;
    static private ListenableGraph<LengthData> staticGraph;
    static private double batterySize;
    private static int robotCapacity;
    private static double robotSpeed;

    public static TimedEventHandler<AddVehicleEvent> defaultHandler(
            ListenableGraph<LengthData> graph,
            double batterySize,
            double robotSpeed,
            int robotCapacity,
            int pathsToExplore
    ) {
        AddRobotAgentEventHandlers.staticGraph = graph;
        AddRobotAgentEventHandlers.batterySize = batterySize;
        AddRobotAgentEventHandlers.pathsToExplore = pathsToExplore;
        AddRobotAgentEventHandlers.robotCapacity = robotCapacity;
        AddRobotAgentEventHandlers.robotSpeed= robotSpeed;
        return AddRobotAgentEventHandlers.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddVehicleEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddVehicleEvent event, @NotNull SimulatorAPI sim) {
                PizzeriaModel pm = ((Simulator) sim).getModelProvider().getModel(PizzeriaModel.class);
                pm.newRobot(staticGraph, robotCapacity, robotSpeed,
                       batterySize, pathsToExplore);

            }

            public String toString() {
                return AddVehicleEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }
    }
}
