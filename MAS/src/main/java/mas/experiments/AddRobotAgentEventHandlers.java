package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;

public class AddRobotAgentEventHandlers {

    private static int robotCapacity;
    private static double robotSpeed;
    private static double batteryCapacity;
    private static long batteryRescueDelay;
    private static ListenableGraph<LengthData> staticGraph;
    private static int alternativePathsToExplore;
    private static long explorationRefreshTime;
    private static long intentionRefreshTime;

    public static TimedEventHandler<AddVehicleEvent> defaultHandler(
            int robotCapacity,
            double robotSpeed,
            double batteryCapacity,
            long batteryRescueDelay,
            ListenableGraph<LengthData> staticGraph,
            int alternativePathsToExplore,
            long explorationRefreshTime,
            long intentionRefreshTime
    ) {
        AddRobotAgentEventHandlers.robotCapacity = robotCapacity;
        AddRobotAgentEventHandlers.robotSpeed = robotSpeed;
        AddRobotAgentEventHandlers.batteryCapacity = batteryCapacity;
        AddRobotAgentEventHandlers.batteryRescueDelay = batteryRescueDelay;
        AddRobotAgentEventHandlers.staticGraph = staticGraph;
        AddRobotAgentEventHandlers.alternativePathsToExplore = alternativePathsToExplore;
        AddRobotAgentEventHandlers.explorationRefreshTime = explorationRefreshTime;
        AddRobotAgentEventHandlers.intentionRefreshTime = intentionRefreshTime;

        return AddRobotAgentEventHandlers.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddVehicleEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddVehicleEvent event, @NotNull SimulatorAPI sim) {
                PizzeriaModel pm = ((Simulator) sim).getModelProvider().getModel(PizzeriaModel.class);

                pm.createRobot(
                        robotCapacity,
                        robotSpeed,
                        batteryCapacity,
                        batteryRescueDelay,
                        staticGraph,
                        alternativePathsToExplore,
                        explorationRefreshTime,
                        intentionRefreshTime
                );

            }

            public String toString() {
                return AddVehicleEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }
    }
}
