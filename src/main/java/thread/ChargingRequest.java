package thread;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.ChargingStation;
import model.ChargingStations;
import model.Taxi;
import rest.beans.TaxiBean;
import seta.proto.taxi.Taxi.ChargingRequestMessage;
import seta.proto.taxi.TaxiServiceGrpc;
import util.Utils;

import java.awt.*;
import java.util.List;

public class ChargingRequest extends Thread {
    private final Taxi taxi;

    private int okCounter;

    public ChargingRequest(Taxi taxi) {
        this.taxi = taxi;
        this.okCounter = 0;
    }

    @Override
    public void run() {
        ChargingStation station = getStationsFromPosition(taxi.getStartPos());

        ChargingRequestMessage chargingRequest = ChargingRequestMessage.newBuilder()
                .setTaxiId(taxi.getId())
                .setStationId(station.getId())
                .setTimestamp(Utils.getCurrentTimestamp())
                .build();

        taxi.setRechargeStationId(station.getId());

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
