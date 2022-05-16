package rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class AverageStatistics {
    private double km;

    private double battery;

    private double pollution;

    private double rides;

    public AverageStatistics() {}

    public AverageStatistics(double km, double battery, double pollution, double rides) {
        this.km = km;
        this.battery = battery;
        this.pollution = pollution;
        this.rides = rides;
    }

    public double getKm() {
        return km;
    }

    public double getBattery() {
        return battery;
    }

    public double getPollution() {
        return pollution;
    }

    public double getRides() {
        return rides;
    }
}
