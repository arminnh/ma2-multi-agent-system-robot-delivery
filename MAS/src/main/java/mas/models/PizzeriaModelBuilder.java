package mas.models;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import org.jetbrains.annotations.NotNull;



public class PizzeriaModelBuilder extends ModelBuilder.AbstractModelBuilder<PizzeriaModel, Object> {

    PizzeriaModelBuilder() {
        setDependencies(RoadModel.class, PDPModel.class, Clock.class);
    }

    @Override
    public PizzeriaModel build(@NotNull DependencyProvider dependencyProvider) {
        final RoadModel rmModel = dependencyProvider.get(RoadModel.class);
        final PDPModel pdpModel = dependencyProvider.get(PDPModel.class);
        final Clock clock = dependencyProvider.get(Clock.class);

        return new PizzeriaModel(rmModel, pdpModel, clock);
    }
}
