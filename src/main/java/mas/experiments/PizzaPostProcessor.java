package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.PostProcessor;
import mas.statistics.StatisticsDTO;
import mas.statistics.StatsTracker;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class PizzaPostProcessor implements PostProcessor<String> {
    private int id;
    private int run = 0;

    PizzaPostProcessor(int id) {
        this.id = id;
    }

    public String collectResults(@NotNull Simulator sim, @NotNull Experiment.SimArgs args) {
        StatsTracker statsTracker = sim.getModelProvider().getModel(StatsTracker.class);

        long timeElapsed = (System.currentTimeMillis() - statsTracker.getTheListener().simulationStartTime) / 1000;
        run++;
        System.out.println("Stopping experiment " + id + ", run " + run + ", " + timeElapsed + "s");

        return statsTracker.getStatistics().toString();
    }

    public FailureStrategy handleFailure(@NotNull Exception e, @NotNull Simulator sim, @NotNull Experiment.SimArgs args) {
        return FailureStrategy.ABORT_EXPERIMENT_RUN;
    }
}
