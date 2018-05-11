package mas.models;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import com.github.rinde.rinsim.pdptw.common.StatsProvider;
import mas.pizza.DeliveryTask;
import mas.statistics.StatsTracker;

public class DeliveryTaskModelBuilder extends ModelBuilder.AbstractModelBuilder<DeliveryTaskModel, Object> {
    private static final long serialVersionUID = -4339759920383479477L;

    DeliveryTaskModelBuilder() {
        //setDependencies(ScenarioController.class, Clock.class, RoadModel.class, PDPModel.class);

        setDependencies(RoadModel.class, PDPModel.class);
    }

    @Override
    public DeliveryTaskModel build(DependencyProvider dependencyProvider) {
        // final ScenarioController ctrl = dependencyProvider.get(ScenarioController.class);
        //final Simulator sim = dependencyProvider.get(Simulator.class);
        final RoadModel rmModel = dependencyProvider.get(RoadModel.class);
        final PDPModel pdpModel = dependencyProvider.get(PDPModel.class);

        return new DeliveryTaskModel(rmModel, pdpModel);
    }
}
