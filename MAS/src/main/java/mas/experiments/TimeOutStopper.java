package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;

public abstract class TimeOutStopper implements TimedEvent {

    TimeOutStopper() {}


    public static TimedEventHandler<TimeOutEvent> stopHandler() {
        return Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<TimeOutEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(TimeOutEvent event,
                                         SimulatorAPI simulator) {
                ((Simulator) simulator).stop();
            }

            @Override
            public String toString() {
                return TimeOutEvent.class.getSimpleName() + ".ignoreHandler()";
            }
        };
    }
}
