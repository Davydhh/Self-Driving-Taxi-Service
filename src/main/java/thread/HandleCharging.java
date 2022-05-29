package thread;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.ChargingStation;
import model.ChargingStations;
import model.Taxi;
import model.TaxiState;
import rest.beans.TaxiBean;
import seta.proto.taxi.Taxi.ChargingRequestMessage;
import seta.proto.taxi.TaxiServiceGrpc;
import util.Utils;

import java.awt.*;
import java.util.List;

public class HandleCharging extends Thread {
    private final Taxi taxi;

    private int okCounter;

    private final Object counterLock;

    public HandleCharging(Taxi taxi) {
        this.taxi = taxi;
        this.okCounter = 0;
        this.counterLock = new Object();
    }

    @Override
    public void run() {
        ChargingStation station = Utils.getStationsFromPosition(taxi.getStartPos());

        ChargingRequestMessage chargingRequest = ChargingRequestMessage.newBuilder()
                .setTaxiId(taxi.getId())
                .setStationId(station.getId())
                .setTimestamp(Utils.getCurrentTimestamp())
                .build();

        taxi.setRechargeStationId(station.getId());
        taxi.setRechargeRequestTimestamp(Utils.getCurrentTimestamp());

        List<TaxiBean> taxiList = taxi.getOtherTaxis();

        synchronized (counterLock) {
            if (taxiList.isEmpty()) {
                taxi.setState(TaxiState.CHARGING);
                taxi.recharge(station.getPosition());
            } else {
                for (TaxiBean t : taxiList) {
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

                                synchronized (counterLock) {
                                    okCounter += 1;

                                    if (okCounter == taxiList.size()) {
                                        taxi.setState(TaxiState.CHARGING);
                                        counterLock.notifyAll();
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

                waitUntilReceiveAllOk(station);
            }
        }
    }

    private void waitUntilReceiveAllOk(ChargingStation station) {
        System.out.println("Waiting for receiving ok for charging request for station " + station.getId());

        while (taxi.getState() != TaxiState.CHARGING) {
            try {
                counterLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (taxi.getState() == TaxiState.CHARGING) {
                taxi.recharge(station.getPosition());
            }
        }
    }
}
