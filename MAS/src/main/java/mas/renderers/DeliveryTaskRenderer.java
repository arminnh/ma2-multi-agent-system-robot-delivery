package mas.renderers;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import mas.tasks.DeliveryTask;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.jetbrains.annotations.NotNull;


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
    public void renderStatic(@NotNull GC gc, @NotNull ViewPort vp) {
    }

    @Override
    public void renderDynamic(@NotNull GC gc, @NotNull ViewPort vp, long time) {
        for (DeliveryTask task : this.roadModel.getObjectsOfType(DeliveryTask.class)) {
            final Point p = task.position;
            final int pizzaAmount = task.getPizzasRemaining();
            final int x = vp.toCoordX(p.x) + X_OFFSET;
            final int y = vp.toCoordY(p.y) + Y_OFFSET;

            final org.eclipse.swt.graphics.Point extent = gc.textExtent(Double.toString(pizzaAmount));

            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_DARK_BLUE));

            gc.fillRoundRectangle(
                    x - extent.x / 2,
                    y - extent.y / 2,
                    extent.x - 1,
                    extent.y + 1,
                    ROUND_RECT_ARC_HEIGHT,
                    ROUND_RECT_ARC_HEIGHT
            );

            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));

            gc.drawText(
                    Integer.toString(pizzaAmount),
                    x - extent.x / 2 - X_OFFSET,
                    y - extent.y / 2 + 1,
                    true
            );
        }
    }
}

