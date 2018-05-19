package mas.models;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import org.jetbrains.annotations.NotNull;


public class PizzeriaModelBuilder extends ModelBuilder.AbstractModelBuilder<PizzeriaModel, Object> {

    PizzeriaModelBuilder() {
        setDependencies(RoadModel.class, PDPModel.class);
    }

    @Override
    public PizzeriaModel build(@NotNull DependencyProvider dependencyProvider) {
        final RoadModel rmModel = dependencyProvider.get(RoadModel.class);
        final PDPModel pdpModel = dependencyProvider.get(PDPModel.class);

        return new PizzeriaModel(rmModel, pdpModel);
    }
}
