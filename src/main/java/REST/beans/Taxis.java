package rest.beans;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class Taxis {

    @XmlElement
    private final List<Taxi> taxis;

    private static Taxis instance;

    private Random random;

    private Taxis() {
        taxis = new ArrayList<>();
    }

    public synchronized static Taxis getInstance(){
        if (instance == null)
            instance = new Taxis();
        return instance;
    }

    public synchronized List<Taxi> getTaxis() {
        return new ArrayList<>(taxis);
    }

    public synchronized RegistrationResponse add(Taxi u) {
        if (taxis.contains(u)) {
            return null;
        }
        taxis.add(u);
        return new RegistrationResponse(generateStartingPoint(), getTaxis());
    }

    private Point generateStartingPoint() {
        return new Point(random.nextBoolean() ? 0 : 9, random.nextBoolean() ? 0 : 9);
    }

    public synchronized boolean delete(int id) {
        for (Taxi t: taxis) {
            if (t.getId() == id) {
                taxis.remove(t);
                return true;
            }
        }

        return false;
    }
}