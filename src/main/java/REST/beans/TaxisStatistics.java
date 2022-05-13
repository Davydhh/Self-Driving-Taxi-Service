package rest.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

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
            taxisStatistics.put(taxiId, Collections.singletonList(statistics));
        } else {
            List<Statistics> taxisStatisticsList = taxisStatistics.get(taxiId);
            taxisStatisticsList.add(statistics);
            taxisStatistics.put(taxiId, taxisStatisticsList);
        }
    }
}