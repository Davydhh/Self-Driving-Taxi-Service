package model;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ChargingStations {
    private final List<ChargingStation> chargingStations;

    private static ChargingStations instance;

    private ChargingStations() {
        ChargingStation station1 = new ChargingStation(1, new Point(0, 0));
        ChargingStation station2 = new ChargingStation(2, new Point(0, 9));
        ChargingStation station3 = new ChargingStation(3, new Point(9, 0));
        ChargingStation station4 = new ChargingStation(4, new Point(9, 9));
        chargingStations = Arrays.asList(station1, station2, station3, station4);
    }

    public static ChargingStations getInstance(){
        if (instance == null)
            instance = new ChargingStations();
        return instance;
    }

    public List<ChargingStation> getChargingStations() {
        return chargingStations;
    }
}
