package mas.models;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.Clock;
import org.jetbrains.annotations.NotNull;


public class PizzeriaModelBuilder extends AbstractModelBuilder<PizzeriaModel, PizzeriaUser> {

    PizzeriaModelBuilder() {
        setProvidingTypes(PizzeriaModel.class);

        setDependencies(RoadModel.class, SimulatorAPI.class);
    }

    @Override
    public PizzeriaModel build(@NotNull DependencyProvider dependencyProvider) {
        return new PizzeriaModel(
                dependencyProvider.get(RoadModel.class),
                dependencyProvider.get(SimulatorAPI.class)
        );
    }
}
