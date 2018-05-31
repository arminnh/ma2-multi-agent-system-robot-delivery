package mas.experiments.events;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.buildings.ChargingStation;
import mas.buildings.Pizzeria;

public abstract class AddPizzeriaEvent implements TimedEvent {

    AddPizzeriaEvent() {
    }

    static boolean createdPizzeria = false;
    static int chargingStationCap = 0;
    @Override
    public long getTime() {
        return 0;
    }
    public abstract Point getPosition();

    public static TimedEventHandler<AddDepotEvent> defaultHandler(int cap) {
        chargingStationCap = cap;
        return AddPizzeriaEvent.Handler.INSTANCE;
    }

    static enum Handler implements TimedEventHandler<AddDepotEvent> {
        INSTANCE {
            public void handleTimedEvent(AddDepotEvent event, SimulatorAPI sim) {
                if(!createdPizzeria){
                    sim.register(new Pizzeria(event.getPosition()));
                    createdPizzeria = true;
                }else{
                    sim.register(new ChargingStation(event.getPosition(), chargingStationCap));
                }
            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

