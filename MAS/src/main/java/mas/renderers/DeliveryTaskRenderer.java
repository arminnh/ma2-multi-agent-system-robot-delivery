package mas.renderers;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import mas.tasks.DeliveryTask;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import java.util.Map;
import java.util.Map.Entry;


public class DeliveryTaskRenderer extends AbstractCanvasRenderer {

    private static final int ROUND_RECT_ARC_HEIGHT = 5;
    private static final int X_OFFSET = -5;
    private static final int Y_OFFSET = -30;

    private final RoadModel roadModel;

    DeliveryTaskRenderer(RoadModel r) {
        roadModel = r;
    }

    public static DeliveryTaskRendererBuilder builder() {
        return new DeliveryTaskRendererBuilder();
    }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        final Map<RoadUser, Point> map = Maps.filterEntries(roadModel.getObjectsAndPositions(), Filter.DELIVERY_TASKS);

        for (final Entry<RoadUser, Point> parcel : map.entrySet()) {
            final DeliveryTask task = (DeliveryTask) parcel.getKey();
            final Point p = parcel.getValue();
            final double pizzaAmount = task.getPizzasRequested();
            final int x = vp.toCoordX(p.x) + X_OFFSET;
            final int y = vp.toCoordY(p.y) + Y_OFFSET;

            final org.eclipse.swt.graphics.Point extent = gc.textExtent(Double.toString(pizzaAmount));

            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_BLUE));
            gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2,
                    extent.x + 2, extent.y + 2, ROUND_RECT_ARC_HEIGHT,
                    ROUND_RECT_ARC_HEIGHT);
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

            gc.drawText(Double.toString(pizzaAmount), x - extent.x / 2 + 1, y - extent.y / 2 + 1,
                    true);
        }
    }

    private enum Filter implements Predicate<Entry<RoadUser, Point>> {
        DELIVERY_TASKS {
            @Override
            public boolean apply(Entry<RoadUser, Point> input) {
                return input.getKey() instanceof DeliveryTask;
            }

        }
    }
}

