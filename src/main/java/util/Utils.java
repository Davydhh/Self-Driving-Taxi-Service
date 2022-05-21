package util;

import java.awt.*;

public class Utils {
    public static double getDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) +
                Math.pow(p2.getY() - p1.getY(), 2));
    }
}
