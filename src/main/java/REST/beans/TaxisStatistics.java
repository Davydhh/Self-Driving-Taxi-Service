package rest.beans;

import com.google.gson.Gson;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class TaxisStatistics {

    @XmlElement
    private final Map<Integer, List<Statistics>> taxisStatistics;

    private static TaxisStatistics instance;

    private TaxisStatistics() {
        taxisStatistics = new HashMap<>();
    }

    public synchronized static TaxisStatistics getInstance(){
        if (instance == null)
            instance = new TaxisStatistics();
        return instance;
    }

    public synchronized Map<Integer, List<Statistics>> getTaxisStatistics() {
        return new HashMap<>(taxisStatistics);
    }

    public synchronized void add(int taxiId, Statistics statistics) {
        if (!taxisStatistics.containsKey(taxiId)) {
            taxisStatistics.put(taxiId, new ArrayList<>(Collections.singletonList(statistics)));
        } else {
            List<Statistics> taxisStatisticsList = taxisStatistics.get(taxiId);
            taxisStatisticsList.add(statistics);
            taxisStatistics.put(taxiId, taxisStatisticsList);
        }
    }

    public String getLastAverageStatisticsByTaxi(int taxiId, int n) {
        List<Statistics> taxisStatistics = getTaxisStatistics().get(taxiId);

        if (taxisStatistics == null) return null;

        int size = taxisStatistics.size();

        if (size < n) {
            return "403";
        }

        taxisStatistics = taxisStatistics.subList(size - n, size);

        double averageKm = getAverageKm(taxisStatistics);

        double averageBattery = getAverageBattery(taxisStatistics);

        double averagePollution = getAveragePollution(taxisStatistics);

        double averageRides = getAverageRides(taxisStatistics);

        return new Gson().toJson(new AverageStatisticsResponse(averageKm, averageBattery,
                averagePollution, averageRides));
    }

    private double getAverageRides(List<Statistics> taxisStatistics) {
        OptionalDouble averageRidesOpt =
                taxisStatistics.stream().mapToDouble(Statistics::getRides).average();

        return averageRidesOpt.isPresent() ? averageRidesOpt.getAsDouble() : 0;
    }

    private double getAveragePollution(List<Statistics> taxisStatistics) {
        List<Double> pollutionsAverages = new ArrayList<>();

        for (Statistics s: taxisStatistics) {
            OptionalDouble pollution =
                    s.getAveragesMeasurements().stream().mapToDouble(d -> d).average();

            pollutionsAverages.add(pollution.isPresent() ? pollution.getAsDouble() : 0);
        }

        OptionalDouble averagePollutionOpt =
                pollutionsAverages.stream().mapToDouble(d -> d).average();

        return averagePollutionOpt.isPresent() ?
                averagePollutionOpt.getAsDouble() : 0;
    }

    private double getAverageKm(List<Statistics> taxisStatistics) {
        OptionalDouble averageKmOpt =
                taxisStatistics.stream().mapToDouble(Statistics::getKm).average();

        return averageKmOpt.isPresent() ? averageKmOpt.getAsDouble() : 0;
    }

    private double getAverageBattery(List<Statistics> taxisStatistics) {
        OptionalDouble averageBatteryOpt =
                taxisStatistics.stream().mapToDouble(Statistics::getBattery).average();

        return averageBatteryOpt.isPresent() ? averageBatteryOpt.getAsDouble() : 0;
    }
    public String getAllStatisticsBetweenTimestamps(long t1, long t2) {
        List<AverageStatistics> averageStatistics = new ArrayList<>();

        //Compute average for each taxi
        for(Map.Entry<Integer, List<Statistics>> entry : getTaxisStatistics().entrySet()) {
            List<Statistics> statistics =
                    entry.getValue().stream().filter(s -> s.getTimestamp() >= t1 && s.getTimestamp() <= t2).collect(Collectors.toList());

            double averageKm = getAverageKm(statistics);
            double averageBattery = getAverageBattery(statistics);
            double averagePollution = getAveragePollution(statistics);
            double averageRides = getAverageRides(statistics);

            averageStatistics.add(new AverageStatistics(averageKm, averageBattery,
                    averagePollution, averageRides));
        }

        OptionalDouble averageKmOpt =
                averageStatistics.stream().mapToDouble(AverageStatistics::getKm).average();

        double averageKm = averageKmOpt.isPresent() ? averageKmOpt.getAsDouble() : 0;

        OptionalDouble averageBatteryOpt =
                averageStatistics.stream().mapToDouble(AverageStatistics::getBattery).average();

        double averageBattery =  averageBatteryOpt.isPresent() ?
                averageBatteryOpt.getAsDouble() : 0;

        OptionalDouble averagePollutionOpt =
                averageStatistics.stream().mapToDouble(AverageStatistics::getPollution).average();

        double averagePollution = averagePollutionOpt.isPresent() ?
                averagePollutionOpt.getAsDouble() : 0;

        OptionalDouble averageRidesOpt =
                averageStatistics.stream().mapToDouble(AverageStatistics::getRides).average();

        double averageRides =  averageRidesOpt.isPresent() ? averageRidesOpt.getAsDouble() : 0;

        return new Gson().toJson(new AverageStatistics(averageKm, averageBattery,
                averagePollution, averageRides));
    }
}