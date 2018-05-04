package mas.renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;

public class DeliveryTaskRendererBuilder extends ModelBuilder.AbstractModelBuilder<DeliveryTaskRenderer, Void> {

    private static final long serialVersionUID = -1772420262312399129L;

    DeliveryTaskRendererBuilder() {
        setDependencies(RoadModel.class, PDPModel.class);
    }


    @Override
    public DeliveryTaskRenderer build(DependencyProvider dependencyProvider) {
        final RoadModel rm = dependencyProvider.get(RoadModel.class);
        final PDPModel pm = dependencyProvider.get(PDPModel.class);
        return new DeliveryTaskRenderer(rm, pm);
    }


    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else {
            // mas.DeliveryTaskRendererBuilderlder that = (mas.DeliveryTaskRendererBuilderlder)o;
            return o instanceof DeliveryTaskRendererBuilder;
        }
    }


    public int hashCode() {
        int h = 1;
        h = h * 1000003;
        return h;
    }
}
