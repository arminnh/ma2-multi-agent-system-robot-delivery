package mas.renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;

public class RobotRendererBuilder extends ModelBuilder.AbstractModelBuilder<RobotRenderer, Void> {

    private static final long serialVersionUID = -1772420262312399130L;

    RobotRendererBuilder() {
        setDependencies(RoadModel.class, PDPModel.class);
    }


    @Override
    public RobotRenderer build(DependencyProvider dependencyProvider) {
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        final PDPModel pm = dependencyProvider.get(PDPModel.class);
        return new RobotRenderer(rm, pm);
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
