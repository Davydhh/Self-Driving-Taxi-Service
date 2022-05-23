package thread;

import model.Taxi;
import rest.beans.RideRequest;

public class HandleRide extends Thread {
    private final Taxi taxi;

    private final RideRequest request;

    private final double distance;

    public HandleRide(Taxi taxi, RideRequest request, double distance) {
        this.taxi = taxi;
        this.request = request;
        this.distance = distance;
    }

    @Override
    public void run() {
        System.out.println("Waiting for election for request " + request.getId());

        while (!taxi.isRiding()) {
            synchronized (taxi) {
                try {
                    taxi.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            if (taxi.isRiding()) {
                System.out.println("Taxi " + taxi.getId() + " take charge or the ride " + request.getId());
                try {
                    takeRequest();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void takeRequest() throws InterruptedException {
        Thread.sleep(5000);
        taxi.setStartPos(request.getEndPos());
        taxi.setBattery(taxi.getBattery() - (int) Math.round(distance));

        if (taxi.getBattery() < 30) {

        }
    }
}



