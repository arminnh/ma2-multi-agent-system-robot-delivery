package mas.statistics;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import com.github.rinde.rinsim.scenario.ScenarioController;

/**
 * StatsTrackerBuilder for creating {@link StatsTrackerBuilder} instance.
 */
public class StatsTrackerBuilder extends AbstractModelBuilder<StatsTracker, Object> {
    private static final long serialVersionUID = -4339759920383479477L;

    StatsTrackerBuilder() {
        //setDependencies(ScenarioController.class, Clock.class, RoadModel.class, PDPModel.class);

        setDependencies(Clock.class, RoadModel.class, PDPModel.class);

        setProvidingTypes(StatsProvider.class);
    }

    @Override
    public StatsTracker build(DependencyProvider dependencyProvider) {
        // final ScenarioController ctrl = dependencyProvider.get(ScenarioController.class);

        final Clock clck = dependencyProvider.get(Clock.class);
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        final PDPModel pm = dependencyProvider.get(PDPModel.class);

        return new StatsTracker(null, clck, rm, pm);
    }
}
