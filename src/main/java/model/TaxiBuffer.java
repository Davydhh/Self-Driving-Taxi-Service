package model;

import simulator.Buffer;
import simulator.Measurement;

import java.util.ArrayList;
import java.util.List;

public class TaxiBuffer implements Buffer {
    int windowLength = 8;
    List<Measurement> measurements = new ArrayList<>();
    List<Measurement> averagesMeasurements = new ArrayList<>();

    @Override
    public synchronized void addMeasurement(Measurement measurement) {
        if (measurements.size() == windowLength) {
            double sum = 0.0;
            for (Measurement m : measurements) {
                sum += m.getValue();
            }

            averagesMeasurements.add(new Measurement(measurement.getId(), measurement.getType(),
                    sum/windowLength, System.currentTimeMillis()));
            measurements = measurements.subList(windowLength / 2, windowLength);

        } else {
            measurements.add(measurement);
        }
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> averagesValues = new ArrayList<>(averagesMeasurements);
        averagesMeasurements.clear();
        return averagesValues;
    }
}
