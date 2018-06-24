package mas.statistics;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import mas.models.PizzeriaModel;
import org.jetbrains.annotations.NotNull;

/**
 * StatsTrackerBuilder for creating {@link StatsTrackerBuilder} instance.
 */
public class StatsTrackerBuilder extends AbstractModelBuilder<StatsTracker, Object> {

    StatsTrackerBuilder() {
        setDependencies(Clock.class, RoadModel.class, PDPModel.class, PizzeriaModel.class);

        setProvidingTypes(StatsProvider.class);
    }

    @Override
    public StatsTracker build(@NotNull DependencyProvider dependencyProvider) {
        return new StatsTracker(
                dependencyProvider.get(Clock.class),
                dependencyProvider.get(PDPModel.class),
                dependencyProvider.get(PizzeriaModel.class),
                dependencyProvider.get(RoadModel.class)
        );
    }
}
