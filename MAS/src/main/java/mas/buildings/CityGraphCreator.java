package mas.buildings;

import com.github.rinde.rinsim.geom.*;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.util.Map;

public class CityGraphCreator {

    private static ImmutableTable<Integer, Integer, Point> createMatrix(int cols, int rows, Point offset, int vehicle_length) {
        final ImmutableTable.Builder<Integer, Integer, Point> builder =
                ImmutableTable.builder();
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                Point point = new Point(
                        offset.x + c * vehicle_length * 2,
                        offset.y + r * vehicle_length * 2
                );

                builder.put(r, c, point);
            }
        }
        return builder.build();
    }

    public static ListenableGraph<LengthData> createGraph(Integer size, int vehicle_length) {
        final Graph<LengthData> g = new TableGraph<>();

        final Table<Integer, Integer, Point> leftMatrix =
                createMatrix(size, size, new Point(0, 0), vehicle_length);

        for (final Map<Integer, Point> column : leftMatrix.columnMap().values()) {
            Graphs.addBiPath(g, column.values());
        }

        for (final Map<Integer, Point> row : leftMatrix.rowMap().values()) {
            Graphs.addBiPath(g, row.values());
        }

        return new ListenableGraph<>(g);
    }
}