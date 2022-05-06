import model.RideRequest;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Seta {

    public static void main(String[] args) {
        System.out.println("Starting SETA system...");
        MqttClient client;
        String broker = "tcp://localhost:1883";
        String clientId = MqttClient.generateClientId();
        List<String> topics = Arrays.asList("seta/smartcity/rides/district1", "seta/smartcity/rides/district2", "seta" +
                "/smartcity/rides/district3", "seta/smartcity/rides/district4");
        int qos = 1;

        //brew services start mosquitto

        try {
            client = new MqttClient(broker, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            // Connect the client
            System.out.println(clientId + " Connecting Broker " + broker);
            client.connect(connOpts);
            System.out.println(clientId + " Connected");

            String payload = String.valueOf(0 + (Math.random() * 10)); // create a random number between 0 and 10
            MqttMessage message = new MqttMessage(payload.getBytes());

            // Set the QoS on the Message
            message.setQos(qos);
            System.out.println(clientId + " Publishing message: " + payload + " ...");
            client.publish(topic, message);
            System.out.println(clientId + " Message published");

            if (client.isConnected())
                client.disconnect();
            System.out.println("Publisher " + clientId + " disconnected");
        } catch (MqttException me ) {
            System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    private void generateRequests() throws InterruptedException {
        while(true) {
            RideRequest request = new RideRequest()
            TimeUnit.SECONDS.sleep(5);
        }
    }
}
