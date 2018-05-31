package mas.experiments.events;


import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.models.PizzeriaModel;
import mas.tasks.DeliveryTask;
import sun.plugin.dom.exception.InvalidStateException;

import java.util.LinkedList;
import java.util.List;

public abstract class AddDeliveryTaskEvent implements TimedEvent {
    @Override
    public long getTime() {
        return 0;
    }
    public abstract Point getPosition();

    private static List<Point> availablePos = new LinkedList<>();
    public static TimedEventHandler<AddParcelEvent> defaultHandler(ListenableGraph<LengthData> graph, Point pizzeriaPos, Point chargingPos) {
        for(Point node: graph.getNodes()){
            if(node.equals(pizzeriaPos) || node.equals(chargingPos)){
                continue;
            }
            availablePos.add(node);
        }
        return AddDeliveryTaskEvent.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddParcelEvent> {
        INSTANCE {
            public void handleTimedEvent(AddParcelEvent event, SimulatorAPI sim) {

                Simulator sim2 = (Simulator) sim;
                sim2.getModelProvider().getModel(PizzeriaModel.class).createNewDeliveryTask(sim.getRandomGenerator(),
                        4, 0.75, event.getTime());
                //System.out.println("ADDINGPARCEL");
                //throw new InvalidStateException("mhh");

                //int pos = sim.getRandomGenerator().nextInt(availablePos.size() - 1);
                //sim.register(new DeliveryTask(availablePos.get(pos), (int)event.getParcelDTO().getNeededCapacity(), event.getTime()));
            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

