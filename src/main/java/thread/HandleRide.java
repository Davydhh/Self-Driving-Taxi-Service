package thread;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.ChargingStation;
import model.ChargingStations;
import model.Taxi;
import rest.beans.RideRequest;
import rest.beans.TaxiBean;
import seta.proto.taxi.TaxiServiceGrpc;
import util.Utils;

import java.awt.*;
import java.util.List;

public class HandleRide extends Thread {
    private final Taxi taxi;

    private final RideRequest request;

    private int okCounter;

    public HandleRide(Taxi taxi, RideRequest request) {
        this.taxi = taxi;
        this.request = request;
        this.okCounter = 0;
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
            sendChargingRequests();
        }
    }

    public void sendChargingRequests() {
        ChargingStation station = getStationsFromPosition(taxi.getStartPos());

        seta.proto.taxi.Taxi.ChargingRequestMessage chargingRequest = seta.proto.taxi.Taxi.ChargingRequestMessage.newBuilder()
                .setTaxiId(taxi.getId())
                .setStationId(station.getId())
                .setTimestamp(Utils.getCurrentTimestamp())
                .build();

        taxi.setRechargeStationId(station.getId());
        taxi.setRechargeRequestTimestamp(Utils.getCurrentTimestamp());

        new HandleCharging(taxi, station).start();

        List<TaxiBean> taxiList = taxi.getOtherTaxis();

        for (TaxiBean t: taxiList) {
            final ManagedChannel channel =
                    ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
            System.out.println("Taxi " + taxi.getId() + " send charging request to taxi " + t.getId() +
                    " for station " + station.getId());
            TaxiServiceGrpc.TaxiServiceStub stub = TaxiServiceGrpc.newStub(channel);

            stub.sendChargingRequest(chargingRequest, new StreamObserver<seta.proto.taxi.Taxi.ChargingResponseMessage>() {
                @Override
                public void onNext(seta.proto.taxi.Taxi.ChargingResponseMessage value) {
                    if (value.getOk()) {
                        System.out.println("Taxi " + taxi.getId() + " received ok from Taxi " + t.getId()
                                + " about charging request for station " + station.getId());

                        synchronized (taxi) {
                            okCounter += 1;

                            if (okCounter == taxiList.size()) {
                                taxi.setCharging(true);
                                taxi.notifyAll();
                            }
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                    System.exit(0);
                }

                @Override
                public void onCompleted() {
                    channel.shutdownNow();
                }
            });
        }
    }

    private ChargingStation getStationsFromPosition(Point position) {
        List<ChargingStation> chargingStations =
                ChargingStations.getInstance().getChargingStations();

        double x = position.getX();
        double y = position.getY();

        if (x <= 4 && y <= 4) {
            return chargingStations.get(0);
        } else if (x <= 4 && y >= 5) {
            return chargingStations.get(1);
        } else if (x >= 5 && y <= 4){
            return chargingStations.get(2);
        } else {
            return chargingStations.get(3);
        }
    }
}



