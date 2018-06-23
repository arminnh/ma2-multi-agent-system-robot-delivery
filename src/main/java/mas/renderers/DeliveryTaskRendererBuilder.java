package mas.renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import org.jetbrains.annotations.NotNull;

public class DeliveryTaskRendererBuilder extends ModelBuilder.AbstractModelBuilder<DeliveryTaskRenderer, Void> {

    private static final long serialVersionUID = -1772420262312399129L;

    DeliveryTaskRendererBuilder() {
        setDependencies(RoadModel.class);
    }

    @Override
    public DeliveryTaskRenderer build(@NotNull DependencyProvider dependencyProvider) {
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        return new DeliveryTaskRenderer(rm);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            //DeliveryTaskRendererBuilder that = (DeliveryTaskRendererBuilder)o;
            return o instanceof DeliveryTaskRendererBuilder;
        }
    }

    public int hashCode() {
        int h = 1;
        h = h * 1000003;
        return h;
    }
}
