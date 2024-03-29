package util;

import model.ChargingStation;
import model.ChargingStations;

import java.awt.*;
import java.util.List;

public class Utils {
    public static double getDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.getX() - p1.getX(), 2) +
                Math.pow(p2.getY() - p1.getY(), 2));
    }

    public static String getDistrictTopicFromPosition(Point position) {
        double x = position.getX();
        double y = position.getY();

        if (x <= 4 && y <= 4) {
            return "seta/smartcity/rides/district1";
        } else if (x <= 4 && y >= 5) {
            return "seta/smartcity/rides/district2";
        } else if (x >= 5 && y <= 4){
            return "seta/smartcity/rides/district3";
        } else {
            return "seta/smartcity/rides/district4";
        }
    }

    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    public static ChargingStation getStationsFromPosition(Point position) {
        List<ChargingStation> chargingStations =
                ChargingStations.getInstance().getChargingStations();

        double x = position.getX();
        double y = position.getY();

        if (x <= 4 && y <= 4) {
            return chargingStations.get(0);
        } else if (x <= 4 && y >= 5) {
            return chargingStations.get(1);
        } else if (x >= 5 && y <= 4){
            return chargingStations.get(2);
        } else {
            return chargingStations.get(3);
        }
    }
}
