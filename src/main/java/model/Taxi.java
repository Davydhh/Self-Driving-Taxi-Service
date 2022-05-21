package model;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.*;
import rest.beans.RegistrationResponse;
import rest.beans.RideRequest;
import rest.beans.TaxiBean;
import seta.proto.taxi.TaxiServiceGrpc;
import simulator.PM10Simulator;
import thread.HandleRide;
import thread.TaxiGrpcServer;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.net.URL;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

@XmlRootElement
public class Taxi {
    private final int id;

    private final int port;

    private final String ip;

    private int battery;

    private Point startPos;

    private Point endPoint;

    private List<TaxiBean> otherTaxis;

    private Client restClient;

    private MqttClient mqttClient;

    private boolean riding;

    private boolean charging;

    public Taxi(int id, int port) {
        this.id = id;
        this.port = port;
        this.ip = "localhost";
        this.battery = 100;
        this.restClient = Client.create();
        this.riding = false;
        this.charging = false;
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
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

    public Client getRestClient() {
        return restClient;
    }

    public MqttClient getMqttClient() {
        return mqttClient;
    }

    public boolean isRiding() {
        return riding;
    }

    public boolean isCharging() {
        return charging;
    }

    public void setStartPos(Point startPos) {
        this.startPos = startPos;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    public void setOtherTaxis(List<TaxiBean> otherTaxis) {
        this.otherTaxis = otherTaxis;
    }

    public void setRestClient(Client restClient) {
        this.restClient = restClient;
    }

    public void setMqttClient(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void setRiding(boolean riding) {
        this.riding = riding;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    private static void register(TaxiBean taxiBean, String serverAddress, Taxi taxi) {
        ClientResponse response = postRequest(serverAddress + "/taxis", taxiBean, taxi.getRestClient());
        if (response != null && response.getStatus() == 200) {
            System.out.println("\nRegistration successful");
            RegistrationResponse responseBody = new Gson().fromJson(response.getEntity(String.class),
                    RegistrationResponse.class);
            System.out.println(responseBody.toString());
            taxi.setStartPos(responseBody.getStarPos());
            taxi.setOtherTaxis(responseBody.getTaxis());
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

    private static ClientResponse postRequest(String url, TaxiBean taxiBean, Client restClient) {
        WebResource webResource = restClient.resource(url);
        String input = new Gson().toJson(taxiBean);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    private static void startAndSubMqttConnection(Taxi taxi) {
        String broker = "tcp://localhost:1883";
        String mqttClientId = MqttClient.generateClientId();

        try {
            taxi.setMqttClient(new MqttClient(broker, mqttClientId));
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error in mqtt connection creation, exit");
            System.exit(0);
        }

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);

        System.out.println(mqttClientId + " Connecting Broker " + broker);

        try {
            taxi.getMqttClient().connect(connOpts);
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error in mqtt connection, exit");
            System.exit(0);
        }

        System.out.println(mqttClientId + " Connected - Taxi ID: " + taxi.getId());

        String subTopic = getDistrictTopicFromPosition(taxi.getStartPos());

        taxi.getMqttClient().setCallback(new MqttCallback() {

            public void messageArrived(String topic, MqttMessage message) {
                String receivedMessage = new String(message.getPayload());
                System.out.println("Taxi " + taxi.getId() + " received a Message!" +
                        "\n\tTopic:   " + topic +
                        "\n\tMessage: " + receivedMessage +
                        "\n\tQoS:     " + message.getQos() + "\n");

                RideRequest rideRequest = new Gson().fromJson(receivedMessage, RideRequest.class);

                if (!taxi.isRiding() && !taxi.isCharging()) {
                    new HandleRide(taxi, rideRequest).start();
                }
            }

            public void connectionLost(Throwable cause) {
                System.out.println("Taxi " + taxi.getId() + " has lost the mqtt connection! cause:" + cause.getMessage() +
                        "- Thread PID: " + Thread.currentThread().getId());
            }

            public void deliveryComplete(IMqttDeliveryToken token) {
                // Not used
            }

        });

        try {
            taxi.getMqttClient().subscribe(subTopic, 1);
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error subscribing to topic " + subTopic + ", exit");
            System.exit(0);
        }

        System.out.println("Taxi " + taxi.getId() + " Subscribed to topics : " + subTopic);
    }

    private static String getDistrictTopicFromPosition(Point startPos) {
        double x = startPos.getX();
        double y = startPos.getY();

        if (x <= 4 && y <= 4) {
            return "seta/smartcity/rides/district1";
        } else if (x >= 5 && y <= 4) {
            return "seta/smartcity/rides/district3";
        } else if (x <= 4 && y >= 5){
            return "seta/smartcity/rides/district2";
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

    public static void mqttDisconnect(MqttClient mqttClient) {
        try {
            mqttClient.disconnect();
        } catch (MqttException ex) {
            ex.printStackTrace();
            System.out.println("Error in mqtt disconnection");
            System.exit(0);
        }
    }

    public List<TaxiBean> getOtherTaxis() {
        return otherTaxis;
    }

    private static void sendToOtherTaxis(Taxi taxi) {
        for (TaxiBean t: taxi.getOtherTaxis()) {
            final ManagedChannel channel =
                    ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            System.out.println("Taxi " + taxi.getId() + " connected to address " + t.getIp() + ":" + t.getPort());
            TaxiServiceGrpc.TaxiServiceStub stub = TaxiServiceGrpc.newStub(channel);
            seta.proto.taxi.Taxi.TaxiMessage message =
                    seta.proto.taxi.Taxi.TaxiMessage.newBuilder().setId(taxi.getId()).setIp(taxi.getIp()).setPort(taxi.getPort()).setStartX(taxi.getStartPos().getX()).setStartY(taxi.getStartPos().getY()).build();
            stub.addTaxi(message, new StreamObserver<seta.proto.taxi.Taxi.AddResponse>() {
                @Override
                public void onNext(seta.proto.taxi.Taxi.AddResponse value) {
                    if (value.getAdded()) {
                        System.out.println("Taxi " + t.getId() + " correctly updated");
                    } else {
                        System.out.println("Taxi " + t.getId() + " have been not correctly " +
                                "updated");
                    }
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    System.out.println(0);
                }

                @Override
                public void onCompleted() {
                    channel.shutdownNow();
                }
            });
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Creating taxi");
        System.out.println("Insert the id (number)");
        int id = -1;
        try {
            id = scanner.nextInt();
        } catch (InputMismatchException ex) {
            ex.printStackTrace();
            System.out.println("The id must be a number!");
            System.exit(0);
        }

        System.out.println("Insert the port (number)");
        int port = -1;
        try {
            port = scanner.nextInt();
        } catch (InputMismatchException ex) {
            ex.printStackTrace();
            System.out.println("The port must be a number!");
            System.exit(0);
        }

        System.out.println("Insert the server address");
        scanner.nextLine();
        String serverAddress = scanner.nextLine();

        if (!isValidURL(serverAddress)) {
            System.out.println("The server address is invalid!");
            System.exit(0);
        }

        Taxi taxi = new Taxi(id, port);
        register(new TaxiBean(id, port, taxi.getIp()), serverAddress, taxi);

//        startAcquiringData();

        new TaxiGrpcServer(taxi).start();

        if (!taxi.getOtherTaxis().isEmpty()) {
            sendToOtherTaxis(taxi);
        }

        startAndSubMqttConnection(taxi);
    }
}