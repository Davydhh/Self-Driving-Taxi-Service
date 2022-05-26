package thread;

import model.Taxi;
import rest.beans.RideRequest;
import util.Utils;

public class HandleRide extends Thread {
    private final Taxi taxi;

    private final RideRequest request;

    public HandleRide(Taxi taxi, RideRequest request) {
        this.taxi = taxi;
        this.request = request;
    }

    @Override
    public void run() {
        System.out.println("Waiting for election for request " + request.getId());

        while (!taxi.isDriving()) {
            synchronized (taxi) {
                try {
                    taxi.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (taxi.isDriving() && taxi.getRequestIdTaken() == request.getId()) {
                System.out.println("Taxi " + taxi.getId() + " takes charge of the ride " + request.getId());
                try {
                    takeRequest();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void takeRequest() throws InterruptedException {
        double distance = Utils.getDistance(taxi.getStartPos(), request.getEndPos());
        Thread.sleep(5000);
        taxi.setStartPos(request.getEndPos());
        taxi.subMqttTopic();
        taxi.setBattery(taxi.getBattery() - (int) Math.round(distance));
        taxi.setDriving(false);

        if (taxi.getBattery() < 95) {
            System.out.println("\nTaxi " + taxi.getId() + " has battery lower than 30%");
            new ChargingRequest(taxi).start();
        }
    }
}



