package model;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.eclipse.paho.client.mqttv3.*;
import rest.beans.RegistrationResponse;
import rest.beans.TaxiBean;
import simulator.PM10Simulator;

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

    private static void register(TaxiBean taxiBean, String serverAddress) {
        ClientResponse response = postRequest(serverAddress + "/taxis", taxiBean);
        if (response != null && response.getStatus() == 200) {
            System.out.println("Registration successful");
            RegistrationResponse responseBody = new Gson().fromJson(response.getEntity(String.class),
                    RegistrationResponse.class);

            startPos = responseBody.getStarPos();
            otherTaxis = responseBody.getTaxis();
        } else {
            System.exit(0);
        }
    }

    private static void startAcquiringData() {
        System.out.println("Starting acquiring data from PM10 sensor");
        TaxiBuffer buffer = new TaxiBuffer();
        PM10Simulator pm10Simulator = new PM10Simulator(buffer);
        pm10Simulator.start();
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

    private static void startAndSubMqttConnection() {
        String broker = "tcp://localhost:1883";
        String mqttClientId = MqttClient.generateClientId();

        try {
            mqttClient = new MqttClient(broker, mqttClientId);
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error in mqtt connection creation, exit");
            System.exit(0);
        }

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        System.out.println(mqttClientId + " Connecting Broker " + broker);

        try {
            mqttClient.connect(connOpts);
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error in mqtt connection, exit");
            System.exit(0);
        }

        System.out.println(mqttClientId + " Connected - Taxi ID: " + id);

        String subTopic = getDistrictTopicFromPosition();

        mqttClient.setCallback(new MqttCallback() {

            public void messageArrived(String topic, MqttMessage message) {
                String receivedMessage = new String(message.getPayload());
                System.out.println("Taxi " + id + " received a Message!" +
                        "\n\tTopic:   " + topic +
                        "\n\tMessage: " + receivedMessage +
                        "\n\tQoS:     " + message.getQos() + "\n");

                //TODO: Start election
            }

            public void connectionLost(Throwable cause) {
                System.out.println("Taxi " + id + " has lost the mqtt connection! cause:" + cause.getMessage() +
                        "- Thread PID: " + Thread.currentThread().getId());
            }

            public void deliveryComplete(IMqttDeliveryToken token) {
                // Not used
            }

        });

        try {
            mqttClient.subscribe(subTopic, 1);
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error subscribing to topic " + subTopic + ", exit");
            System.exit(0);
        }

        System.out.println("Taxi " + id + " Subscribed to topics : " + subTopic);
    }

    private static String getDistrictTopicFromPosition() {
        double x = startPos.getX();
        double y = startPos.getY();

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

    public static void mqttDisconnect() {
        try {
            mqttClient.disconnect();
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error in mqtt disconnection");
            System.exit(0);
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

        register(new TaxiBean(id, port, ip), serverAddress);

        startAcquiringData();

        if (!otherTaxis.isEmpty()) {
            //TODO: send position to other taxis
        }

        startAndSubMqttConnection();
    }
}