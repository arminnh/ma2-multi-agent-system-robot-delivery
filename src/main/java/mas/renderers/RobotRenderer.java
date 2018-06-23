package mas.renderers;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractCanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import mas.agents.RobotAgent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.jetbrains.annotations.NotNull;


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
    public void renderStatic(@NotNull GC gc, @NotNull ViewPort vp) {
    }

    @Override
    public void renderDynamic(@NotNull GC gc, @NotNull ViewPort vp, long time) {
        for (RobotAgent robot : this.roadModel.getObjectsOfType(RobotAgent.class)) {
            if (!robot.getPosition().isPresent()) {
                continue;
            }

            final Point p = robot.getPosition().get();

            final int x = vp.toCoordX(p.x);
            final int y = vp.toCoordY(p.y);

            drawBattery(x, y, gc, robot.getRemainingBatteryCapacityPercentage() * 100);

            // Draw waiting for ants.
            if (robot.waitingForAnts()) {
                gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
                gc.drawText(
                        "Waiting " + robot.getWaitingForAntsType() + " ants",
                        x - 40,
                        y + 12,
                        true
                );
            }

            if (robot.isIdle()) {
                // Draw robot's idle time
                gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
                gc.drawText(
                        "Idle: " + Double.toString(robot.getIdleTime() / 1000),
                        x - 40,
                        y,
                        true
                );
            } else {
                // Draw robot's estimated time of arrival
                gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
                gc.drawText(
                        "Arrive: " + Double.toString(robot.getIntendedArrivalTime() / 1000),
                        x - 40,
                        y,
                        true
                );
            }
        }

    }

    private void drawBattery(int x, int y, @NotNull GC gc, double currentBattery) {
        x += X_OFFSET;
        y += Y_OFFSET;

        final org.eclipse.swt.graphics.Point extent = gc.textExtent(Integer.toString(100) + "%");
        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

        gc.fillRoundRectangle(
                x - extent.x / 2,
                y - extent.y / 2,
                extent.x + 2,
                extent.y + 2,
                ROUND_RECT_ARC_HEIGHT,
                ROUND_RECT_ARC_HEIGHT
        );

        if (currentBattery > 80.0) {
            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));

        } else if (currentBattery > 30.0) {
            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));

        } else {
            gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
        }

        gc.fillRoundRectangle(
                x - extent.x / 2,
                y - extent.y / 2,
                (int) Math.max((extent.x + 2) * currentBattery / 100.0, 0),
                extent.y + 2,
                ROUND_RECT_ARC_HEIGHT, ROUND_RECT_ARC_HEIGHT
        );

        gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

        gc.drawText(
                Integer.toString((int) Math.round(currentBattery)) + "%",
                x - extent.x / 2 + 1,
                y - extent.y / 2 + 1,
                true
        );
    }
}

