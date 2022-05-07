package rest.beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;

@XmlRootElement
public class Taxi {
    private int id;

    private int port;

    private String serverAddress;

    private int battery;

    private Point startPos;

    private Point endPoint;

    private List otherTaxis;

    public Taxi() {}

    public Taxi(int id, int port, String serverAddress) {
        this.id = id;
        this.port = port;
        this.serverAddress = serverAddress;
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getBattery() {
        return battery;
    }

    public Point getStartPos() {
        return startPos;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final Taxi other = (Taxi) obj;
        return this.id == other.id;
    }
}