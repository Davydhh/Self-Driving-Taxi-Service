package thread;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttException;
import rest.beans.RideRequest;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

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
        String topic = getDistrictTopicFromPosition(startPos);
        String payload = new Gson().toJson(request);
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);
        System.out.println("Publishing message: " + payload);
        try {
            client.publish(topic, message);
        } catch (MqttException me ) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
        System.out.println("Message published to topic " + topic);
        System.out.println();
    }

    private String getDistrictTopicFromPosition(Point position) {
        double x = position.getX();
        double y = position.getY();

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
}
