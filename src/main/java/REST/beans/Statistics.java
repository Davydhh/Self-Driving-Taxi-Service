package rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class Statistics {
    private double km;

    private int rides;

    private List<Double> averagesMeasurements;

    private long timestamp;

    private int battery;

    public Statistics() {}

    public Statistics(double km, int rides, List<Double> averagesMeasurements, long timestamp, int battery) {
        this.km = km;
        this.rides = rides;
        this.averagesMeasurements = averagesMeasurements;
        this.timestamp = timestamp;
        this.battery = battery;
    }

    public double getKm() {
        return km;
    }

    public int getRides() {
        return rides;
    }

    public List<Double> getAveragesMeasurements() {
        return averagesMeasurements;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getBattery() {
        return battery;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "km=" + km +
                ", rides=" + rides +
                ", averagesMeasurements=" + averagesMeasurements +
                ", timestamp=" + timestamp +
                ", battery=" + battery +
                '}';
    }
}
