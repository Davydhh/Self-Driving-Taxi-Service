package thread;

import model.ChargingStation;
import model.Taxi;
import model.TaxiState;
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
        System.out.println("Waiting for receiving ok for charging request for station " + station.getId());

        while (taxi.getState() != TaxiState.CHARGING) {
            synchronized (taxi.getChargingLock()) {
                try {
                    taxi.getChargingLock().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (taxi.getState() == TaxiState.CHARGING) {
                    charge();
                }
            }
        }
    }

    private void charge() {
        double distance = Utils.getDistance(taxi.getStartPos(), station.getPosition());
        taxi.setBattery(taxi.getBattery() - (int) Math.round(distance));
        taxi.setStartPos(station.getPosition());

        System.out.println("Taxi " + taxi.getId() + " is charging on station " + station.getId());

        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        taxi.setState(TaxiState.FREE);
        taxi.setRechargeStationId(-1);
        taxi.setBattery(100);
        taxi.getChargingLock().notifyAll();
    }
}



