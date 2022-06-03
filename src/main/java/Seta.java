import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import rest.beans.RideRequest;
import thread.RideRequestGenerator;
import util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Seta {
    public static void main(String[] args) {
        System.out.println("SETA system started");
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();

        try {
            client = new MqttClient(broker, clientId, null);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected");

            Map<String, List<RideRequest>> requests = new HashMap<>();

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Seta Connection lost!");
                    cause.printStackTrace();
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
                        for (RideRequest r: requests.get(receivedMessage)) {
                            String payload = new Gson().toJson(r);
                            MqttMessage pubMessage = new MqttMessage(payload.getBytes());
                            try {
                                client.publish(receivedMessage, pubMessage);
                                System.out.println("Resend request " + r.getId());
                            } catch (MqttException e) {
                                System.out.println("reason " + e.getReasonCode());
                                System.out.println("msg " + e.getMessage());
                                System.out.println("loc " + e.getLocalizedMessage());
                                System.out.println("cause " + e.getCause());
                                System.out.println("excep " + e);
                                e.printStackTrace();
                            }
                        }
                    } else if (topic.equals("seta/smartcity/rides/handled")) {
                        RideRequest rideRequest = new Gson().fromJson(receivedMessage, RideRequest.class);
                        String requestTopic =
                                Utils.getDistrictTopicFromPosition(rideRequest.getStartPos());
                        List<RideRequest> requestsByTopic = requests.get(requestTopic);

                        if (requestsByTopic != null && !requestsByTopic.isEmpty()) {
                            requestsByTopic.remove(rideRequest);
                            System.out.println("\nRequest " + rideRequest.getId() + " removed");
                            System.out.println("Seta requests: " + requests);
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

            client.subscribe(subTopics, subQos);

            new RideRequestGenerator(client, requests).start();
        } catch (MqttException me ) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }
}
