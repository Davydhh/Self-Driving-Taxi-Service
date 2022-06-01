package thread;

import model.Taxi;
import rest.beans.Statistics;
import simulator.Measurement;
import util.Utils;

import java.util.stream.Collectors;

public class ComputeTaxiStatistics extends Thread {
    private final Taxi taxi;

    public ComputeTaxiStatistics(Taxi taxi) {
        this.taxi = taxi;
    }

    @Override
    public void run() {
        Statistics statistics = new Statistics(taxi.getKm(), taxi.getRides(),
                taxi.getBuffer().readAllAndClean().stream().map(Measurement::getValue).collect(Collectors.toList()),
                Utils.getCurrentTimestamp(), taxi.getBattery());

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
