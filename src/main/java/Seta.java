import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import rest.beans.RideRequest;
import thread.RideRequestGenerator;
import util.Utils;

import java.util.*;

public class Seta {
    private static void mqttConnect(MqttClient client) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(false);

        int errors = 0;
        while(true) {
            try {
                client.connect(connOpts);
                System.out.println("Seta connected");
                break;
            } catch (MqttException e) {
                System.out.println("reason " + e.getReasonCode());
                System.out.println("msg " + e.getMessage());
                System.out.println("loc " + e.getLocalizedMessage());
                System.out.println("cause " + e.getCause());
                System.out.println("excep " + e);

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }

                if (++errors == 5) {
                    System.exit(0);
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("SETA system started");
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();

        try {
            client = new MqttClient(broker, clientId, null);

            System.out.println(clientId + " Connecting Broker " + broker);
            mqttConnect(client);

            Map<String, Queue<RideRequest>> requests = new HashMap<>();

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Seta Connection lost!");
                    cause.printStackTrace();
                    mqttConnect(client);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String receivedMessage = new String(message.getPayload());
                    System.out.println("\nSeta Received a Message! " +
                            "\n\tTopic:   " + topic +
                            "\n\tMessage: " + receivedMessage +
                            "\n\tQoS:     " + message.getQos() + "\n");

                    if (topic.equals("seta/smartcity/taxis/free")) {
                        System.out.println("Taxis available on topic " + receivedMessage);
                        RideRequest r = null;

                        synchronized (requests) {
                            Queue<RideRequest> queue = requests.get(receivedMessage);
                            if (queue != null) {
                                r = queue.peek();
                            }
                        }

                        if (r != null) {
                            String payload = new Gson().toJson(r);
                            MqttMessage pubMessage = new MqttMessage(payload.getBytes());

                            int errors = 0;
                            while(true) {
                                try {
                                    client.publish(receivedMessage, pubMessage);
                                    System.out.println("Resend request " + r.getId());
                                    break;
                                } catch (MqttException e) {
                                    System.out.println("reason " + e.getReasonCode());
                                    System.out.println("msg " + e.getMessage());
                                    System.out.println("loc " + e.getLocalizedMessage());
                                    System.out.println("cause " + e.getCause());
                                    System.out.println("excep " + e);

                                    if (!client.isConnected()) {
                                        mqttConnect(client);
                                    }

                                    if (++errors == 5) {
                                        System.exit(0);
                                    }
                                }
                            }
                        }
                    } else if (topic.equals("seta/smartcity/rides/handled")) {
                        RideRequest rideRequest = new Gson().fromJson(receivedMessage, RideRequest.class);
                        String requestTopic =
                                Utils.getDistrictTopicFromPosition(rideRequest.getStartPos());

                        synchronized (requests) {
                            Queue<RideRequest> requestsByTopic = requests.get(requestTopic);

                            if (requestsByTopic != null && requestsByTopic.remove(rideRequest)) {
                                System.out.println("\nRequest " + rideRequest.getId() + " removed");
                                System.out.println("Seta requests: " + requests);

                                int errors = 0;
                                while(true) {
                                    try {
                                        client.publish("seta/smartcity/rides/removed", message);
                                        break;
                                    } catch (MqttException e) {
                                        System.out.println("reason " + e.getReasonCode());
                                        System.out.println("msg " + e.getMessage());
                                        System.out.println("loc " + e.getLocalizedMessage());
                                        System.out.println("cause " + e.getCause());
                                        System.out.println("excep " + e);

                                        if (!client.isConnected()) {
                                            mqttConnect(client);
                                        }

                                        if (++errors == 5) {
                                            System.exit(0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    //not used
                }
            });

            String[] subTopics = new String[] {"seta/smartcity/taxis/free", "seta/smartcity/rides/handled"};
            int[] subQos = new int[] {2, 2};

            int errors = 0;
            while(true) {
                try {
                    client.subscribe(subTopics, subQos);
                    break;
                } catch (MqttException e) {
                    System.out.println("reason " + e.getReasonCode());
                    System.out.println("msg " + e.getMessage());
                    System.out.println("loc " + e.getLocalizedMessage());
                    System.out.println("cause " + e.getCause());
                    System.out.println("excep " + e);

                    if (!client.isConnected()) {
                        mqttConnect(client);
                    }

                    if (++errors == 5) {
                        System.exit(0);
                    }
                }
            }

            new RideRequestGenerator(client, requests).start();
        } catch (MqttException e) {
            System.out.println("reason " + e.getReasonCode());
            System.out.println("msg " + e.getMessage());
            System.out.println("loc " + e.getLocalizedMessage());
            System.out.println("cause " + e.getCause());
            System.out.println("excep " + e);

            System.exit(0);
        }
    }
}
