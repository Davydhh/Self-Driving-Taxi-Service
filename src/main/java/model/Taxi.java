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
import thread.HandleElection;
import thread.TaxiGrpcServer;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.net.URL;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

@XmlRootElement
public class Taxi {
    private final int id;

    private final int port;

    private final String ip;

    private Integer battery;

    private Point startPos;

    private List<TaxiBean> otherTaxis;

    private final Client restClient;

    private MqttClient mqttClient;

    private boolean driving;

    private boolean charging;

    private String topic;

    private int requestIdTaken;

    private int rechargeStationId;

    private long rechargeRequestTimestamp;

    public Taxi(int id, int port) {
        this.id = id;
        this.port = port;
        this.ip = "localhost";
        this.battery = 100;
        this.restClient = Client.create();
        this.driving = false;
        this.charging = false;
        this.rechargeStationId = -1;
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

    public Integer getBattery() {
        return battery;
    }

    public Point getStartPos() {
        return startPos;
    }

    public int getRequestIdTaken() {
        return requestIdTaken;
    }

    public boolean isDriving() {
        return driving;
    }

    public boolean isCharging() {
        return charging;
    }

    public int getRechargeStationId() {
        return rechargeStationId;
    }

    public String getTopic() {
        return topic;
    }

    public long getRechargeRequestTimestamp() {
        return rechargeRequestTimestamp;
    }

    public void setStartPos(Point startPos) {
        this.startPos = startPos;
        System.out.println("Taxi " + id + " new starting position " + startPos);
    }
    public void setDriving(boolean driving) {
        this.driving = driving;
        if (driving) {
            System.out.println("Taxi " + id + " is driving");
        } else {
            System.out.println("Taxi " + id + " has finished the ride");
        }
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public void setBattery(int battery) {
        this.battery = battery;
        System.out.println("Battery level: " + this.battery);
    }

    public void setRequestIdTaken(int requestIdTaken) {
        this.requestIdTaken = requestIdTaken;
        System.out.println("Set Taxi " + id + " request " + requestIdTaken);
    }

    public void setRechargeStationId(int rechargeStationId) {
        this.rechargeStationId = rechargeStationId;
    }

    public void setRechargeRequestTimestamp(long rechargeRequestTimestamp) {
        this.rechargeRequestTimestamp = rechargeRequestTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Taxi taxi = (Taxi) o;
        return id == taxi.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private void register(TaxiBean taxiBean, String serverAddress) {
        ClientResponse response = postRequest(serverAddress + "/taxis", taxiBean);
        if (response != null && response.getStatus() == 200) {
            System.out.println("\nRegistration successful");
            RegistrationResponse responseBody = new Gson().fromJson(response.getEntity(String.class),
                    RegistrationResponse.class);
            System.out.println(responseBody.toString());
            startPos = responseBody.getStarPos();
            otherTaxis = responseBody.getTaxis();
        } else {
            System.exit(0);
        }
    }

    private void startAcquiringData() {
        System.out.println("Starting acquiring data from PM10 sensor");
        TaxiBuffer buffer = new TaxiBuffer();
        PM10Simulator pm10Simulator = new PM10Simulator(buffer);
        pm10Simulator.start();
    }

    private ClientResponse postRequest(String url, TaxiBean taxiBean) {
        WebResource webResource = restClient.resource(url);
        String input = new Gson().toJson(taxiBean);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    private void startMqttConnection(Taxi taxi) {
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

        mqttClient.setCallback(new MqttCallback() {

            public void messageArrived(String topic, MqttMessage message) {
                String receivedMessage = new String(message.getPayload());
                System.out.println("Taxi " + id + " received a Message!" +
                        "\n\tTopic:   " + topic +
                        "\n\tMessage: " + receivedMessage +
                        "\n\tQoS:     " + message.getQos() + "\n");

                RideRequest rideRequest = new Gson().fromJson(receivedMessage, RideRequest.class);

                if (!driving && !charging && rechargeStationId == -1) {
                    new HandleElection(taxi, rideRequest).start();
                } else {
                    System.out.println("Taxi " + id + " is already driving or charging or in " +
                            "mutual exclusion for charging");
                }
            }

            public void connectionLost(Throwable cause) {
                System.out.println("Taxi " + id + " has lost the mqtt connection! cause:" + cause.getMessage());
            }

            public void deliveryComplete(IMqttDeliveryToken token) {
                // Not used
            }

        });
    }

    public void subMqttTopic() {
        double x = startPos.getX();
        double y = startPos.getY();
        String newTopic;

        if (x <= 4 && y <= 4) {
            newTopic = "seta/smartcity/rides/district1";
        } else if (x >= 5 && y <= 4) {
            newTopic = "seta/smartcity/rides/district3";
        } else if (x <= 4 && y >= 5){
            newTopic = "seta/smartcity/rides/district2";
        } else {
            newTopic = "seta/smartcity/rides/district4";
        }

        if (!newTopic.equals(this.topic)) {
            try {
                if (topic != null) {
                    mqttClient.unsubscribe(topic);
                }
                mqttClient.subscribe(newTopic, 1);
                this.topic = newTopic;
            } catch (MqttException ex) {
                ex.printStackTrace();
                mqttDisconnect();
                System.out.println("Error subscribing to topic " + newTopic + ", exit");
                System.exit(0);
            }

            System.out.println("Taxi " + id + " Subscribed to topics : " + newTopic);
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

    public void mqttDisconnect() {
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

    private void sendToOtherTaxis() {
        for (TaxiBean t: otherTaxis) {
            final ManagedChannel channel =
                    ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            System.out.println("Taxi " + id + " connected to address " + t.getIp() + ":" + t.getPort());
            TaxiServiceGrpc.TaxiServiceStub stub = TaxiServiceGrpc.newStub(channel);
            seta.proto.taxi.Taxi.TaxiMessage message =
                    seta.proto.taxi.Taxi.TaxiMessage.newBuilder().setId(id).setIp(ip).setPort(port).setStartX(startPos.getX()).setStartY(startPos.getY()).build();
            stub.addTaxi(message, new StreamObserver<seta.proto.taxi.Taxi.AddTaxiResponseMessage>() {
                @Override
                public void onNext(seta.proto.taxi.Taxi.AddTaxiResponseMessage value) {
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
        taxi.register(new TaxiBean(id, port, taxi.getIp()), serverAddress);

//        startAcquiringData();

        new TaxiGrpcServer(taxi).start();

        if (!taxi.getOtherTaxis().isEmpty()) {
            taxi.sendToOtherTaxis();
        }

        taxi.startMqttConnection(taxi);
        taxi.subMqttTopic();
    }
}