package rest.beans;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

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

    private Client client;

    public Taxi() {
        this.serverAddress = "http://localhost:1337";
        this.battery = 100;
        this.client = Client.create();
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

    public void setId(int id) {
        this.id = id;
    }

    public void setStartPos(Point startPos) {
        this.startPos = startPos;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    public void setOtherTaxis(List otherTaxis) {
        this.otherTaxis = otherTaxis;
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

    private void register() {
        ClientResponse response = postRequest(serverAddress + "/taxis");
        if (response != null && response.getStatus() == 200) {
            response.
        } else {

        }
    }

    private ClientResponse postRequest(String url) {
        WebResource webResource = client.resource(url);
        String input = new Gson().toJson(this);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    public static void main(String[] args) {
        Taxi taxi = new Taxi();
        taxi.register();
    }
}