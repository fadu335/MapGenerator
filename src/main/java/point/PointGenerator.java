package point;

import java.util.List;

public class PointGenerator {

    public static String generateMapPoints(List<Point> points) {
        StringBuilder mapBuilder = new StringBuilder();

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            mapBuilder.append(point.x()).append("|").append(point.y()).append("|").append(point.mapTime());

            if (i < points.size() - 1) {
                mapBuilder.append(",");
            }
        }

        return mapBuilder.toString();
    }
}
