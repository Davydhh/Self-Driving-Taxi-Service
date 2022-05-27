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
import thread.HandleCharging;
import thread.HandleElection;
import thread.TaxiGrpcServer;
import util.Utils;

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

    private int battery;

    private Point startPos;

    private final Object otherTaxisLock;

    private List<TaxiBean> otherTaxis;

    private final Client restClient;

    private MqttClient mqttClient;

    private TaxiState state;

    private final Object drivingLock;

    private final Object chargingLock;

    private String topic;

    private final Object topicLock;

    private int requestIdTaken;

    private int rechargeStationId;

    private long rechargeRequestTimestamp;

    private final Object batteryLock;

    private int rides;

    private final Object ridesLock;

    public Taxi(int id, int port) {
        this.id = id;
        this.port = port;
        this.ip = "localhost";
        this.battery = 100;
        this.restClient = Client.create();
        this.state = TaxiState.FREE;
        this.rechargeStationId = -1;
        this.rides = 0;
        this.drivingLock = new Object();
        this.chargingLock = new Object();
        this.batteryLock = new Object();
        this.otherTaxisLock = new Object();
        this.ridesLock = new Object();
        this.topicLock = new Object();
    }

    public TaxiState getState() {
        synchronized (state) {
            return state;
        }
    }

    public void setState(TaxiState value) {
        synchronized (this.state) {
            this.state = value;
            System.out.println("Taxi " + id + " changed state to " + value);
        }
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
        synchronized (batteryLock) {
            return battery;
        }
    }

    public Point getStartPos() {
        return startPos;
    }

    public int getRequestIdTaken() {
        return requestIdTaken;
    }

    public int getRechargeStationId() {
        return rechargeStationId;
    }

    public String getTopic() {
        synchronized (topicLock) {
            return topic;
        }
    }

    public long getRechargeRequestTimestamp() {
        return rechargeRequestTimestamp;
    }

    public Object getDrivingLock() {
        return drivingLock;
    }

    public Object getChargingLock() {
        return chargingLock;
    }

    public Object getBatteryLock() {
        return batteryLock;
    }

    public Object getOtherTaxisLock() {
        return otherTaxisLock;
    }

    public Object getRidesLock() {
        return ridesLock;
    }

    public int getRides() {
        synchronized (ridesLock) {
            return rides;
        }
    }

    public void incrementRides() {
        synchronized (ridesLock) {
            rides++;
        }
    }

    public void setStartPos(Point startPos) {
        this.startPos = startPos;
        System.out.println("Taxi " + id + " new starting position " + startPos);
    }

    public void setBattery(int battery) {
        synchronized (batteryLock) {
            this.battery = battery;
            System.out.println("Battery level: " + this.battery);
        }
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

    public void addTaxi(TaxiBean taxi) {
        synchronized (otherTaxisLock) {
            otherTaxis.add(taxi);
        }
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

    public void completeRide(Point endPoint) {
        System.out.println("Handling ride...");
        double distance = Utils.getDistance(startPos, endPoint);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setStartPos(endPoint);
        subMqttTopic();
        decreaseBattery((int) Math.round(distance));
        setRequestIdTaken(-1);
        incrementRides();

        if (battery < 30) {
            System.out.println("\nTaxi " + id + " has battery lower than 30%");
            new HandleCharging(this).start();
        }

        setState(TaxiState.FREE);
        System.out.println("Request completed");
    }

    public void completeCharging(Point stationPosition) {
        double distance = Utils.getDistance(startPos, stationPosition);
        decreaseBattery((int) Math.round(distance));
        setStartPos(stationPosition);

        System.out.println("Taxi " + id + " is charging on station " + rechargeStationId);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        setState(TaxiState.FREE);
        setRechargeStationId(-1);
        setBattery(100);

        synchronized (getChargingLock()) {
            getChargingLock().notifyAll();
        }
    }

    private void decreaseBattery(int value) {
        synchronized (batteryLock) {
            battery -= value;
        }
        System.out.println("Battery level: " + battery + "%");
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
            mqttClient = new MqttClient(broker, mqttClientId, null);
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

                if (getState() == TaxiState.FREE && rechargeStationId == -1) {
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

        synchronized (topicLock) {
            if (!newTopic.equals(this.topic)) {
                try {
                    if (topic != null) {
                        System.out.println("Taxi " + id + " Unsubscribe from topic: " + topic);
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

                System.out.println("Taxi " + id + " Subscribed to topic : " + newTopic);
            }
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
        synchronized (otherTaxisLock) {
            return otherTaxis;
        }
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