package model;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import rest.beans.RegistrationResponse;
import rest.beans.TaxiBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

@XmlRootElement
public class Taxi {
    private static int id;

    private static int port;

    private static String ip;

    private static int battery;

    private static Point startPos;

    private static Point endPoint;

    private static List<TaxiBean> otherTaxis;

    private static Client restClient;

    private static MqttClient mqttClient;

    private static String broker;

    private static String mqttClientId;

    private static void register(TaxiBean taxiBean) {
        ClientResponse response = postRequest(ip + "/taxis", taxiBean);
        if (response != null && response.getStatus() == 200) {
            RegistrationResponse responseBody = new Gson().fromJson(response.getEntity(String.class),
                    RegistrationResponse.class);
            startPos = responseBody.getStarPos();
            otherTaxis = responseBody.getTaxis();
            //TODO: Acquiring Data from sensors
            if (!otherTaxis.isEmpty()) {

            }
        } else {
            System.exit(0);
        }
    }

    private static ClientResponse postRequest(String url, TaxiBean taxiBean) {
        WebResource webResource = restClient.resource(url);
        String input = new Gson().toJson(taxiBean);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    private static void startMqttConnection() throws MqttException {
        broker = "tcp://localhost:1883";
        mqttClientId = MqttClient.generateClientId();
        String topic = getDistrictTopicFromPosition();
        int qos = 2;
    }

    private static void subMqttTopic() throws MqttException {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        System.out.println(mqttClientId + " Connecting Broker " + broker);
        mqttClient.connect(connOpts);
        System.out.println(mqttClient + " Connected - Taxi ID: " + id);
    }

    private static String getDistrictTopicFromPosition() {
        double x = startPos.getX();
        double y = endPoint.getY();

        if (x <= 4 && y <= 4) {
            return "seta/smartcity/rides/district1";
        } else if (x >= 5 && y <= 4) {
            return "seta/smartcity/rides/district2";
        } else if (x <= 4 && y >= 5){
            return "seta/smartcity/rides/district3";
        } else {
            return "seta/smartcity/rides/district4";
        }
    }

    public static boolean isValidURL(String urlString) {
        try {
            URL url = new URL(urlString);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        ip = "http://localhost";
        battery = 100;
        restClient = Client.create();

        BufferedReader inFromUser =
                new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Creating taxi");
        System.out.println("Insert the id (number)");
        int id = -1;
        try {
            id = Integer.parseInt(inFromUser.readLine());
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            System.out.println("The id must be a number!");
            System.exit(0);
        }
        System.out.println("Insert the port (number)");
        int port = -1;
        try {
            port = Integer.parseInt(inFromUser.readLine());
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            System.out.println("The port must be a number!");
            System.exit(0);
        }
        System.out.println("Insert the server address");
        String serverAddress = inFromUser.readLine();
        if (!isValidURL(serverAddress)) {
            System.out.println("The server address is invalid!");
            System.exit(0);
        }
        register(new TaxiBean(id, port, ip));
    }
}