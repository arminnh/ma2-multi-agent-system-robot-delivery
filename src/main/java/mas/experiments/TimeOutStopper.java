package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEventHandler;
import org.jetbrains.annotations.NotNull;

abstract class TimeOutStopper implements TimedEvent {

    private static int id;
    private static int run = 0;
    private static long lastTime = System.currentTimeMillis();

    TimeOutStopper() {
    }


    static TimedEventHandler<TimeOutEvent> stopHandler(int id) {
        TimeOutStopper.id = id;

        return Handler.INSTANCE;
    }

    enum Handler implements TimedEventHandler<TimeOutEvent> {
        INSTANCE {
            @Override
            public void handleTimedEvent(@NotNull TimeOutEvent event, @NotNull SimulatorAPI simulator) {
                long timeElapsed = (System.currentTimeMillis() - lastTime) / 1000;
                lastTime = System.currentTimeMillis();
                run++;
                System.out.println("Stopping experiment " + id + ", run " + run + ", " + timeElapsed + "s");
                ((Simulator) simulator).stop();
            }

            @Override
            public String toString() {
                return TimeOutEvent.class.getSimpleName() + ".ignoreHandler()";
            }
        };
    }
}
