package mas.experiments.events;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;
import mas.models.PizzeriaModel;

public abstract class AddPizzeriaEvent implements TimedEvent {

    private static Point chargingPos;
    private static Point pizzeriaPos;

    AddPizzeriaEvent() {
    }

    static boolean createdPizzeria = false;
    static int chargingStationCap = 0;
    @Override
    public long getTime() {
        return 0;
    }
    public abstract Point getPosition();
    private static ListenableGraph<LengthData> graph;

    public static TimedEventHandler<AddDepotEvent> defaultHandler(int cap, ListenableGraph<LengthData> g,
                                                                  Point PizzeriaPos, Point ChargingPoint) {
        chargingStationCap = cap;
        graph = g;
        pizzeriaPos = PizzeriaPos;
        chargingPos = ChargingPoint;
        return AddPizzeriaEvent.Handler.INSTANCE;
    }

    static enum Handler implements TimedEventHandler<AddDepotEvent> {
        INSTANCE {
            public void handleTimedEvent(AddDepotEvent event, SimulatorAPI sim) {
                sim.register(new Pizzeria(pizzeriaPos));
                PizzeriaModel pm = ((Simulator) sim).getModelProvider().getModel(PizzeriaModel.class);
                for(Point p: graph.getNodes()){
                    pm.createResourceAgent(p);
                }
                sim.register(new ChargingStation(chargingPos, chargingStationCap));
            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

