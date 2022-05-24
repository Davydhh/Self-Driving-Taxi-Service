package thread;

import model.ChargingStation;
import model.Taxi;
import util.Utils;

public class HandleCharging extends Thread {
    private final Taxi taxi;

    private final ChargingStation station;

    public HandleCharging(Taxi taxi, ChargingStation station) {
        this.taxi = taxi;
        this.station = station;
    }

    @Override
    public void run() {
        System.out.println("Waiting for receive ok for charging request ");

        while (!taxi.isCharging()) {
            synchronized (taxi) {
                try {
                    taxi.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (taxi.isCharging()) {
                System.out.println("Taxi " + taxi.getId() + " is charging on station " + station.getId());
                try {
                    charge();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void charge() throws InterruptedException {
        double distance = Utils.getDistance(taxi.getStartPos(), station.getPosition());
        taxi.setBattery(taxi.getBattery() - (int) Math.round(distance));
        taxi.setStartPos(station.getPosition());
        Thread.sleep(10000);
        taxi.setBattery(100);
        taxi.setCharging(false);
        //TODO: Invio ok a tutti quelli interessati alla station
    }
}



