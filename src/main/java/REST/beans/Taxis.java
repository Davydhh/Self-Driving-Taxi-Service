package rest.beans;

import com.google.gson.Gson;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class Taxis {

    @XmlElement
    private final List<TaxiBean> taxis;

    private static Taxis instance;

    private final Random random;

    private Taxis() {
        taxis = new ArrayList<>();
        random = new Random();
    }

    public synchronized static Taxis getInstance(){
        if (instance == null)
            instance = new Taxis();
        return instance;
    }

    public synchronized List<TaxiBean> getTaxis() {
        return new ArrayList<>(taxis);
    }

    public synchronized String add(TaxiBean u) {
        if (taxis.contains(u)) {
            return null;
        }
        taxis.add(u);
        List<TaxiBean> otherTaxis = getTaxis();
        otherTaxis.remove(u);
        return new Gson().toJson(new RegistrationResponse(generateStartingPoint(), otherTaxis));
    }

    private Point generateStartingPoint() {
        return new Point(random.nextBoolean() ? 0 : 9, random.nextBoolean() ? 0 : 9);
    }

    public synchronized boolean delete(int id) {
        for (TaxiBean t: taxis) {
            if (t.getId() == id) {
                taxis.remove(t);
                return true;
            }
        }

        return false;
    }
}