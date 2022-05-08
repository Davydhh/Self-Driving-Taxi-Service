//package thread;
//
//import model.RideRequest;
//import org.eclipse.paho.client.mqttv3.MqttClient;
//
//import java.awt.*;
//import java.util.Random;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class RideRequestGenerator extends Thread {
//    private final AtomicInteger requestId;
//
//    private final Random random;
//
//    private final MqttClient client;
//
//    public RideRequestGenerator(AtomicInteger requestId, MqttClient client) {
//        this.requestId = requestId;
//        this.client = client;
//        random = new Random();
//    }
//
//    @Override
//    public void run() {
//        while(true) {
//            generateRequest();
//            generateRequest();
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//
//    private void generateRequest() throws InterruptedException {
//        RideRequest request = new RideRequest(requestId.getAndIncrement(),
//                new Point(random.nextInt(10), random.nextInt(10)),
//                new Point(random.nextInt(10), random.nextInt(10)));
//    }
//
//    private String getDistrictTopicFromPosition(Point position) {
//        double x = position.getX();
//        double y = position.getY();
//
//        if (x <= 4 && y <= 4) {
//            return "seta/smartcity/rides/district1";
//        } else if (x >= 5 && y <= 4) {
//            return "seta/smartcity/rides/district2";
//        } else if (x <= 4 && y >= 5){
//            return "seta/smartcity/rides/district3";
//        } else {
//            return "seta/smartcity/rides/district4";
//        }
//    }
//}
