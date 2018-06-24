package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import org.jetbrains.annotations.NotNull;

abstract class TimeOutStopper implements TimedEvent {

    TimeOutStopper() {
    }


    static TimedEventHandler<TimeOutEvent> stopHandler() {
        return Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<TimeOutEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(@NotNull TimeOutEvent event, @NotNull SimulatorAPI simulator) {
                ((Simulator) simulator).stop();
            }

            @Override
            public String toString() {
                return TimeOutEvent.class.getSimpleName() + ".ignoreHandler()";
            }
        };
    }
}
