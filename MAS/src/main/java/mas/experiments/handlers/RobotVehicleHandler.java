package mas.experiments.handlers;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.agents.Battery;
import mas.agents.RobotAgent;

public class RobotVehicleHandler {

    static private int id = 1;
    static private int pathsToExplore = 3;
    static private Point pizzeriaPos;
    static private Point chargingStationPos;
    static private ListenableGraph<LengthData> staticMap;
    static private double batterySize;
    public static TimedEventHandler<AddVehicleEvent> defaultHandler(Point pizzeriaPosition,
                                                                    Point chargingStationPosition,
                                                                    ListenableGraph<LengthData> staticGraph,
                                                                    double batterySiz) {
        pizzeriaPos = pizzeriaPosition;
        chargingStationPos = chargingStationPosition;
        staticMap = staticGraph;
        batterySize = batterySiz;
        return RobotVehicleHandler.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddVehicleEvent> {
        INSTANCE {
            public void handleTimedEvent(AddVehicleEvent event, SimulatorAPI sim) {
                Battery b = new Battery(batterySize);
                sim.register(new RobotAgent(id, event.getVehicleDTO(), b, staticMap, pizzeriaPos,pathsToExplore, chargingStationPos));
                id++;
            }

            public String toString() {
                return AddVehicleEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }
    }
}
