package mas.renderers;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import mas.agents.RobotAgent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import java.util.Map;
import java.util.Map.Entry;


public class RobotRenderer extends AbstractCanvasRenderer {

    private static final int ROUND_RECT_ARC_HEIGHT = 5;
    private static final int X_OFFSET = -5;
    private static final int Y_OFFSET = -30;

    private final RoadModel roadModel;

    RobotRenderer(RoadModel r) {
        roadModel = r;
    }

    public static RobotRendererBuilder builder() {
        return new RobotRendererBuilder();
    }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {
        final Map<RoadUser, Point> map = Maps.filterEntries(roadModel.getObjectsAndPositions(), Filter.ROBOTS);

        for (final Entry<RoadUser, Point> robotEntry : map.entrySet()) {
            final RobotAgent robotAgent = (RobotAgent) robotEntry.getKey();
            final Point p = robotAgent.getPosition().get();
            final int currentBattery = robotAgent.getRemainingBatteryCapacity();
            final int x = vp.toCoordX(p.x) + X_OFFSET;
            final int y = vp.toCoordY(p.y) + Y_OFFSET;

            final org.eclipse.swt.graphics.Point extent = gc.textExtent(Integer.toString(100) + "%");
            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

            gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2, extent.x + 2, extent.y + 2,
                    ROUND_RECT_ARC_HEIGHT, ROUND_RECT_ARC_HEIGHT);

            if (currentBattery > 80) {
                gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));

            } else if (currentBattery > 30) {
                gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));

            } else {
                gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
            }

            gc.fillRoundRectangle(x - extent.x / 2, y - extent.y / 2,
                    Math.max((extent.x + 2) * currentBattery / 100, 0), extent.y + 2,
                    ROUND_RECT_ARC_HEIGHT, ROUND_RECT_ARC_HEIGHT);

            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

            gc.drawText(Integer.toString(Math.max(currentBattery, 0)) + "%", x - extent.x / 2 + 1,
                    y - extent.y / 2 + 1,true);
        }

    }

    private enum Filter implements Predicate<Entry<RoadUser, Point>> {
        ROBOTS {
            @Override
            public boolean apply(Entry<RoadUser, Point> input) {
                return input.getKey() instanceof RobotAgent;
            }

        }
    }
}

