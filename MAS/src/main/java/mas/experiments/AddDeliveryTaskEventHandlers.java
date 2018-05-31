package mas.experiments;


import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;

public abstract class AddDeliveryTaskEventHandlers {
    private static double pizzaMean;
    private static double pizzaStd;

    public static TimedEventHandler<AddParcelEvent> defaultHandler(double pizzaMean, double pizzaStd) {
        AddDeliveryTaskEventHandlers.pizzaMean = pizzaMean;
        AddDeliveryTaskEventHandlers.pizzaStd = pizzaStd;

        return AddDeliveryTaskEventHandlers.Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<AddParcelEvent> {
        INSTANCE {
            public void handleTimedEvent(@NotNull AddParcelEvent event, @NotNull SimulatorAPI sim) {
                Simulator sim2 = (Simulator) sim;

                sim2.getModelProvider().getModel(PizzeriaModel.class).createNewDeliveryTask(
                        sim.getRandomGenerator(), pizzaMean, pizzaStd, event.getTime()
                );
            }

            public String toString() {
                return AddDepotEvent.class.getSimpleName() + ".defaultHandler()";
            }
        };

    }
}

