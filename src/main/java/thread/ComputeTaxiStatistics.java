package thread;

import model.Taxi;
import model.TaxiState;
import rest.beans.Statistics;
import simulator.Measurement;
import util.Utils;

import java.util.List;
import java.util.stream.Collectors;

public class ComputeTaxiStatistics extends Thread {
    private final Taxi taxi;

    private boolean condition;

    public ComputeTaxiStatistics(Taxi taxi) {
        this.taxi = taxi;
        this.condition = true;
    }

    @Override
    public void run() {
        while (condition) {
            synchronized (taxi.getStatisticsLock()) {
                try {
                    taxi.getStatisticsLock().wait(15000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            Statistics statistics = new Statistics(taxi.getKm(), taxi.getRides(), getPollutionAverages(),
                    Utils.getCurrentTimestamp(), taxi.getBattery());

            taxi.sendStatistics(statistics);
        }
    }

    public void stopMeGently() {
        condition = false;
    }

    private List<Double> getPollutionAverages() {
        synchronized (taxi.getBuffer()) {
            return taxi.getBuffer().readAllAndClean().stream().map(Measurement::getValue).collect(Collectors.toList());
        }
    }
}
