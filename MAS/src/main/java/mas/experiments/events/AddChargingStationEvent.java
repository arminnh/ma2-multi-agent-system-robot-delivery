package mas.experiments.events;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.buildings.ChargingStation;

public abstract class AddChargingStationEvent implements TimedEvent{
    @Override
    public long getTime() {
        return 0;
    }
    public abstract Point getPosition();

    private static int stationCap;

    public static TimedEventHandler<AddChargingStationEvent> defaultHandler(int capacity) {
        stationCap = capacity;
        return AddChargingStationEvent.Handler.INSTANCE;
    }

    static enum Handler implements TimedEventHandler<AddChargingStationEvent> {
        INSTANCE {
            public void handleTimedEvent(AddChargingStationEvent event, SimulatorAPI sim) {
                sim.register(new ChargingStation(event.getPosition(), stationCap));
            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

