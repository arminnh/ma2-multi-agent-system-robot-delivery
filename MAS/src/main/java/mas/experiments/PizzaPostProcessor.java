package mas.experiments;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.PostProcessor;
import mas.statistics.StatisticsDTO;
import mas.statistics.StatsTracker;

import java.util.Set;

public final class PizzaPostProcessor implements PostProcessor<String> {
    PizzaPostProcessor() {
    }

    public String collectResults(Simulator sim, Experiment.SimArgs args) {
        /*
        Set<Vehicle> vehicles = ((RoadModel)sim.getModelProvider().getModel(RoadModel.class)).getObjectsOfType(Vehicle.class);
        StringBuilder sb = new StringBuilder();
        if(vehicles.isEmpty()) {
            sb.append("No vehicles were added");
        } else {
            sb.append(vehicles.size()).append(" vehicles were added");
        }

        if(sim.getCurrentTime() >= args.getScenario().getTimeWindow().end()) {
            sb.append(", simulation has completed.");
        } else {
            sb.append(", simulation was stopped prematurely.");
        }
        */
        StringBuilder sb = new StringBuilder();

        StatsTracker statsTracker = sim.getModelProvider().getModel(StatsTracker.class);
        StatisticsDTO stats = statsTracker.getStatistics();
        sb.append(stats);

        return sb.toString();
    }

    public FailureStrategy handleFailure(Exception e, Simulator sim, Experiment.SimArgs args) {
        return FailureStrategy.ABORT_EXPERIMENT_RUN;
    }
}
