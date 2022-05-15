package rest.beans;

import com.google.gson.Gson;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;
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

        taxisStatistics = taxisStatistics.subList(size - n, size);

        //Computer average on km
        OptionalDouble averageKmOpt =
                taxisStatistics.stream().mapToDouble(Statistics::getKm).average();

        double averageKm = averageKmOpt.isPresent() ? averageKmOpt.getAsDouble() : 0;

        //Computer average on battery
        OptionalDouble averageBatteryOpt =
                taxisStatistics.stream().mapToDouble(Statistics::getBattery).average();

        double averageBattery = averageBatteryOpt.isPresent() ? averageBatteryOpt.getAsDouble() : 0;

        //Computer average on pollution
        List<Double> pollutionsAverages = new ArrayList<>();

        for (Statistics s: taxisStatistics) {
            OptionalDouble pollution =
                    s.getAveragesMeasurements().stream().mapToDouble(d -> d).average();

            pollutionsAverages.add(pollution.isPresent() ? pollution.getAsDouble() : 0);
        }

        OptionalDouble averagePollutionOpt =
                pollutionsAverages.stream().mapToDouble(d -> d).average();

        double averagePollution = averagePollutionOpt.isPresent() ?
                averagePollutionOpt.getAsDouble() : 0;

        //Compute average on accomplished rides
        OptionalDouble averageRidesOpt =
                taxisStatistics.stream().mapToDouble(Statistics::getRides).average();

        double averageRides = averageRidesOpt.isPresent() ? averageRidesOpt.getAsDouble() : 0;

        return new Gson().toJson(new AverageStatisticsResponse(averageKm, averageBattery,
                averagePollution, averageRides));
    }
}