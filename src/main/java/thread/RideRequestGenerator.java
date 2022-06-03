package thread;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import rest.beans.RideRequest;
import util.Utils;

import java.awt.*;
import java.util.*;

public class RideRequestGenerator extends Thread {
    private int requestId;

    private final Random random;

    private final MqttClient client;

    private final Map<String, Queue<RideRequest>> requests;

    public RideRequestGenerator(MqttClient client, Map<String, Queue<RideRequest>> requests) {
        this.client = client;
        this.requestId = 0;
        this.random = new Random();
        this.requests = requests;
    }

    @Override
    public void run() {
        while(true) {
            generateRequest();
            generateRequest();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private void generateRequest() {
        requestId += 1;
        System.out.println("------------------------------");
        System.out.println("Generating request " + requestId);
        Point startPos;
        Point endPos;

        do {
            startPos = new Point(random.nextInt(10), random.nextInt(10));
            endPos = new Point(random.nextInt(10), random.nextInt(10));
        } while (startPos == endPos);

        RideRequest request = new RideRequest(requestId, startPos, endPos);
        String pubTopic = Utils.getDistrictTopicFromPosition(startPos);
        String payload = new Gson().toJson(request);
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(2);

        System.out.println("Publishing message: " + payload);

        try {
            requests.computeIfAbsent(pubTopic, r -> new LinkedList<>()).offer(request);
            client.publish(pubTopic, message);
            System.out.println("Seta requests: " + requests);
        } catch (MqttException e) {
            System.out.println("reason " + e.getReasonCode());
            System.out.println("msg " + e.getMessage());
            System.out.println("loc " + e.getLocalizedMessage());
            System.out.println("cause " + e.getCause());
            System.out.println("excep " + e);
            e.printStackTrace();
        }
        System.out.println("Message published to topic " + pubTopic);
    }
}
