package mas.experiments;


import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;

public abstract class AddDeliveryTaskAndRoadWorksEventHandlers {
    private static double pizzaMean;
    private static double pizzaStd;
    private static long timeRoadWorks;

    public static TimedEventHandler<AddParcelEvent> defaultHandler(double pizzaMean, double pizzaStd, long timeRoadWorks) {
        AddDeliveryTaskAndRoadWorksEventHandlers.pizzaMean = pizzaMean;
        AddDeliveryTaskAndRoadWorksEventHandlers.pizzaStd = pizzaStd;
        AddDeliveryTaskAndRoadWorksEventHandlers.timeRoadWorks = timeRoadWorks;

        return AddDeliveryTaskAndRoadWorksEventHandlers.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddParcelEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddParcelEvent event, @NotNull SimulatorAPI sim) {
                PizzeriaModel pizzeriaModel = ((Simulator) sim).getModelProvider().getModel(PizzeriaModel.class);
                double capacity = event.getParcelDTO().getNeededCapacity();

                if (capacity == DeliveryTaskAndRoadWorksGenerator.DELIVERY_TASK_EVENT) {
                    pizzeriaModel.createDeliveryTask(sim.getRandomGenerator(), pizzaMean, pizzaStd, event.getTime());

                } else if (capacity == DeliveryTaskAndRoadWorksGenerator.ROAD_WORKS_EVENT) {
                    pizzeriaModel.createRoadWorks(sim.getRandomGenerator(), event.getParcelDTO().getOrderAnnounceTime() + timeRoadWorks);
                }

            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        }

    }
}

