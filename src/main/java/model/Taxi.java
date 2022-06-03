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
import rest.beans.Statistics;
import rest.beans.TaxiBean;
import seta.proto.taxi.TaxiServiceGrpc;
import simulator.PM10Simulator;
import thread.ComputeTaxiStatistics;
import thread.HandleCharging;
import thread.HandleElection;
import thread.TaxiGrpcServer;
import util.Utils;

import javax.xml.bind.annotation.XmlRootElement;
import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.Queue;
import java.util.*;

@XmlRootElement
public class Taxi {

    private final int id;

    private final String serverAddress;

    private final int port;

    private final String ip;

    private int battery;

    private Point startPos;

    private List<TaxiBean> otherTaxis;

    private final Client restClient;

    private MqttClient mqttClient;

    private TaxiState state;

    private String topic;

    private final TaxiBuffer buffer;

    private int requestId;

    private int rechargeStationId;

    private long rechargeRequestTimestamp;

    private int rides;

    private double km;

    private final Queue<RideRequest> requests;

    private final Object otherTaxisLock;

    private final Object topicLock;

    private final Object batteryLock;

    private final Object stateLock;

    private final Object ridesLock;

    private final Object startPosLock;

    private final Object chargingLock;

    private final Object kmLock;

    public Taxi(int id, int port, String serverAddress) {
        this.id = id;
        this.port = port;
        this.serverAddress = serverAddress;
        this.ip = "localhost";
        this.battery = 100;
        this.restClient = Client.create();
        this.state = TaxiState.FREE;
        this.rechargeStationId = -1;
        this.rides = 0;
        this.km = 0;
        this.requests = new LinkedList<>();
        this.buffer = new TaxiBuffer();
        this.batteryLock = new Object();
        this.otherTaxisLock = new Object();
        this.ridesLock = new Object();
        this.topicLock = new Object();
        this.stateLock = new Object();
        this.startPosLock = new Object();
        this.chargingLock = new Object();
        this.kmLock = new Object();
    }

    public TaxiState getState() {
        synchronized (stateLock) {
            return state;
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
        synchronized (startPosLock) {
            return startPos;
        }
    }

    public int getRequestId() {
        return requestId;
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

    public Object getStateLock() {
        return stateLock;
    }

    public Object getOtherTaxisLock() {
        return otherTaxisLock;
    }

    public Object getChargingLock() {
        return chargingLock;
    }

    public int getRides() {
        synchronized (ridesLock) {
            return rides;
        }
    }

    public double getKm() {
        synchronized (kmLock) {
            return km;
        }
    }

    public TaxiBuffer getBuffer() {
        return buffer;
    }

    public void setState(TaxiState value) {
        synchronized (stateLock) {
            this.state = value;
            System.out.println("\nTaxi " + id + " changed state to " + value);
        }

        if (value == TaxiState.FREE) {
            MqttMessage message = new MqttMessage(getTopic().getBytes());
            try {
                mqttClient.publish("seta/smartcity/taxis/free", message);
            } catch (MqttException e) {
                System.out.println("reason " + e.getReasonCode());
                System.out.println("msg " + e.getMessage());
                System.out.println("loc " + e.getLocalizedMessage());
                System.out.println("cause " + e.getCause());
                System.out.println("excep " + e);
                e.printStackTrace();
            }
        }
    }

    public void setStartPos(Point startPos) {
        synchronized (startPosLock) {
            this.startPos = startPos;
            System.out.println("Taxi " + id + " new starting position " + startPos);
        }
    }

    public void setBattery(int battery) {
        synchronized (batteryLock) {
            this.battery = battery;
            System.out.println("Battery level: " + this.battery);
        }
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
        System.out.println("Set Taxi " + id + " request " + requestId);
    }

    public void setRechargeStationId(int rechargeStationId) {
        this.rechargeStationId = rechargeStationId;
    }

    public void setRechargeRequestTimestamp(long rechargeRequestTimestamp) {
        this.rechargeRequestTimestamp = rechargeRequestTimestamp;
    }

    public void addRequest(RideRequest request){
        synchronized (requests) {
            requests.offer(request);
        }
    }

    public void removeRequest(RideRequest request) {
        synchronized (requests) {
            requests.remove(request);
            System.out.println("Queue: " + requests);
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

    public void incrementRides() {
        synchronized (ridesLock) {
            rides++;
        }
    }

    private void incrementKm(double distance) {
        synchronized (kmLock) {
            km += distance;
        }
    }

    public void addTaxi(TaxiBean taxi) {
        synchronized (otherTaxisLock) {
            otherTaxis.add(taxi);
        }
    }

    public void removeTaxi(TaxiBean taxi) {
        synchronized (otherTaxisLock) {
            otherTaxis.remove(taxi);
        }
    }

    public void drive(RideRequest request) {
        System.out.println("Handling ride...");
        double distance = Utils.getDistance(startPos, request.getEndPos());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        incrementKm(distance);
        setStartPos(request.getEndPos());
        subMqttTopic();
        dischargeBattery((int) Math.round(distance));
        setRequestId(-1);
        removeRequest(request);
        incrementRides();
        System.out.println("Request " + request.getId() + " completed");

        if (battery < 30) {
            System.out.println("\nTaxi " + id + " has battery lower than 30%");
            setState(TaxiState.LOW);
            new HandleCharging(this).start();
        } else if (!requests.isEmpty()) {
            setState(TaxiState.HANDLING);
            RideRequest pendingRequest;

            do {
                pendingRequest = requests.poll();
            } while (pendingRequest != null && !getTopic().equals(Utils.getDistrictTopicFromPosition(pendingRequest.getStartPos())));

            if (pendingRequest != null) {
                System.out.println("Getting request " + pendingRequest.getId() + " from queue");
                try {
                    String payload = new Gson().toJson(pendingRequest);
                    mqttClient.publish("seta/smartcity/rides/handled", new MqttMessage(payload.getBytes()));
                } catch (MqttException e) {
                    System.out.println("reason " + e.getReasonCode());
                    System.out.println("msg " + e.getMessage());
                    System.out.println("loc " + e.getLocalizedMessage());
                    System.out.println("cause " + e.getCause());
                    System.out.println("excep " + e);
                    e.printStackTrace();
                }

                new HandleElection(this, pendingRequest).start();
            } else {
                setState(TaxiState.FREE);
                synchronized (getStateLock()) {
                    getStateLock().notifyAll();
                }
            }
        } else {
            setState(TaxiState.FREE);
            synchronized (getStateLock()) {
                getStateLock().notifyAll();
            }
        }
    }

    public void recharge(Point stationPosition) {
        double distance = Utils.getDistance(startPos, stationPosition);
        dischargeBattery((int) Math.round(distance));
        incrementKm(distance);
        setStartPos(stationPosition);

        System.out.println("Taxi " + id + " is charging on station " + rechargeStationId);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        setRechargeStationId(-1);
        setBattery(100);
        setState(TaxiState.FREE);

        synchronized (getChargingLock()) {
            getChargingLock().notifyAll();
        }

        synchronized (getStateLock()) {
            getStateLock().notifyAll();
        }
    }

    private void dischargeBattery(int value) {
        synchronized (batteryLock) {
            battery -= value;
        }
        System.out.println("Battery level: " + battery + "%");
    }

    private void register() {
        TaxiBean taxiBean = new TaxiBean(id, port, ip);
        ClientResponse response = sendAddTaxiPost(serverAddress + "/taxis", taxiBean);
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
        PM10Simulator pm10Simulator = new PM10Simulator(buffer);
        pm10Simulator.start();
        new ComputeTaxiStatistics(this).start();
    }

    private ClientResponse sendAddTaxiPost(String url, TaxiBean taxiBean) {
        WebResource webResource = restClient.resource(url);
        String input = new Gson().toJson(taxiBean);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    private ClientResponse deleteRequest(String url, int id) {
        WebResource webResource = restClient.resource(url + "/" + id);
        try {
            return webResource.type("application/json").delete(ClientResponse.class);
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
                System.out.println("--------------------------------");
                System.out.println("Taxi " + id + " received a Message!" +
                        "\n\tTopic:   " + topic +
                        "\n\tMessage: " + receivedMessage +
                        "\n\tQoS:     " + message.getQos() + "\n");

                RideRequest rideRequest = new Gson().fromJson(receivedMessage, RideRequest.class);

                addRequest(rideRequest);

                if (getState() == TaxiState.FREE) {
                    setState(TaxiState.HANDLING);

                    try {
                        mqttClient.publish("seta/smartcity/rides/handled", message);
                    } catch (MqttException e) {
                        System.out.println("reason " + e.getReasonCode());
                        System.out.println("msg " + e.getMessage());
                        System.out.println("loc " + e.getLocalizedMessage());
                        System.out.println("cause " + e.getCause());
                        System.out.println("excep " + e);
                        e.printStackTrace();
                    }

                    new HandleElection(taxi, rideRequest).start();
                } else {
                    System.out.println("Taxi " + id + " is already driving or charging or in " +
                            "mutual exclusion for charging or has already the request in queue");
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
            System.out.println("\nMqtt disconnect");
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

    private void notifyOtherTaxisForJoining() {
        for (TaxiBean t: getOtherTaxis()) {
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

    private void notifyOtherTaxisForLeaving() {
        for (TaxiBean t: getOtherTaxis()) {
            final ManagedChannel channel =
                    ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            System.out.println("Taxi " + id + " connected to address " + t.getIp() + ":" + t.getPort());
            TaxiServiceGrpc.TaxiServiceBlockingStub stub = TaxiServiceGrpc.newBlockingStub(channel);
            seta.proto.taxi.Taxi.TaxiMessage message =
                    seta.proto.taxi.Taxi.TaxiMessage.newBuilder().setId(id).setIp(ip).setPort(port).setStartX(startPos.getX()).setStartY(startPos.getY()).build();
            seta.proto.taxi.Taxi.RemoveTaxiResponseMessage response = stub.removeTaxi(message);

            if (response.getRemoved()) {
                System.out.println("Taxi " + t.getId() + " correctly updated");
            } else {
                System.out.println("Taxi " + t.getId() + " have been not correctly " +
                        "updated");
            }
        }
    }

    private void leave() {
        setState(TaxiState.LEAVING);
        mqttDisconnect();
        notifyOtherTaxisForLeaving();

        ClientResponse response = deleteRequest(serverAddress + "/taxis", id);
        if (response != null && response.getStatus() == 200) {
            System.out.println("\nRemoval successful");
            System.exit(0);
        } else {
            System.out.println("\nRemoval unsuccessful");
        }
    }

    private ClientResponse sendStatisticsPost(String url, Statistics statistics) {
        WebResource webResource = restClient.resource(url);
        String input = new Gson().toJson(statistics);
        try {
            return webResource.type("application/json").post(ClientResponse.class, input);
        } catch (ClientHandlerException e) {
            System.out.println("Server non disponibile");
            return null;
        }
    }

    public void sendStatistics(Statistics statistics) {
        ClientResponse response = sendStatisticsPost(serverAddress + "/statistics/" + id, statistics);
        if (response != null && response.getStatus() == 200) {
            System.out.println("\nStatistics sent successfully");
        } else {
            System.out.println("\nStatistics sent unsuccessfully");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Creating taxi");
        System.out.println("Insert the id");
        int id = -1;
        try {
            id = scanner.nextInt();
        } catch (InputMismatchException ex) {
            ex.printStackTrace();
            System.out.println("The id must be a number!");
            System.exit(0);
        }

        System.out.println("Insert the port");
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

        Taxi taxi = new Taxi(id, port, serverAddress);
        taxi.register();

//        taxi.startAcquiringData();

        new TaxiGrpcServer(taxi).start();

        if (!taxi.getOtherTaxis().isEmpty()) {
            taxi.notifyOtherTaxisForJoining();
        }

        taxi.startMqttConnection(taxi);
        taxi.subMqttTopic();

        String action;

        do {
            action = scanner.nextLine();

            if (action.equals("recharge")) {
                synchronized (taxi.getStateLock()) {
                    if (taxi.getState() == TaxiState.FREE) {
                        new HandleCharging(taxi).start();
                    } else {
                        while (taxi.getState() != TaxiState.FREE) {
                            try {
                                taxi.getStateLock().wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (taxi.getState() == TaxiState.FREE) {
                                new HandleCharging(taxi).start();
                            }
                        }
                    }
                }
            }
        } while (!action.equals("quit"));

        synchronized (taxi.getStateLock()) {
            if (taxi.getState() == TaxiState.FREE) {
                taxi.leave();
            } else {
                while (taxi.getState() != TaxiState.FREE) {
                    System.out.println("Wait until all operations finish");
                    try {
                        taxi.getStateLock().wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (taxi.getState() == TaxiState.FREE) {
                        taxi.leave();
                    }
                }
            }
        }
    }
}