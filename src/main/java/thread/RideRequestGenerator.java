package thread;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import rest.beans.RideRequest;
import util.Utils;

import java.awt.*;
import java.util.Random;

public class RideRequestGenerator extends Thread {
    private int requestId;

    private final Random random;

    private final MqttClient client;

    public RideRequestGenerator(MqttClient client) {
        this.client = client;
        this.requestId = 0;
        random = new Random();
    }

    @Override
    public void run() {
        while(true) {
            generateRequest();
            generateRequest();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void generateRequest() {
        requestId += 1;
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
        message.setQos(1);

        System.out.println("Publishing message: " + payload);

        try {
            client.publish(pubTopic, message);
        } catch (MqttException me ) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
        System.out.println("Message published to topic " + pubTopic);
        System.out.println();
    }
}
