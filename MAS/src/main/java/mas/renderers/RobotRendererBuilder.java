package mas.renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import org.jetbrains.annotations.NotNull;

public class RobotRendererBuilder extends ModelBuilder.AbstractModelBuilder<RobotRenderer, Void> {

    private static final long serialVersionUID = -1772420262312399130L;

    RobotRendererBuilder() {
        setDependencies(RoadModel.class);
    }

    @Override
    public RobotRenderer build(@NotNull DependencyProvider dependencyProvider) {
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        return new RobotRenderer(rm);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            //RobotRendererBuilder that = (RobotRendererBuilder)o;
            return o instanceof RobotRendererBuilder;
        }
    }

    public int hashCode() {
        int h = 1;
        h = h * 1000003;
        return h;
    }
}
