package thread;

import io.grpc.stub.StreamObserver;
import model.Taxi;
import seta.proto.taxi.Taxi.ChargingResponseMessage;

public class PendingChargingRequest extends Thread {
    private final Taxi taxi;
    private final StreamObserver<ChargingResponseMessage> responseObserver;

    private final int stationId;

    public PendingChargingRequest(Taxi taxi,
                                  StreamObserver<ChargingResponseMessage> responseObserver, int stationId) {
        this.taxi = taxi;
        this.responseObserver = responseObserver;
        this.stationId = stationId;
    }

    @Override
    public void run() {
        System.out.println("Waiting for the recharge station " + stationId + " to finish ");

        while (taxi.isCharging()) {
            synchronized (taxi) {
                try {
                    taxi.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!taxi.isCharging()) {
                System.out.println("\nTaxi " + taxi.getId() + " is not charging anymore on " +
                        "station " + stationId);
                seta.proto.taxi.Taxi.ChargingResponseMessage response = seta.proto.taxi.Taxi.ChargingResponseMessage.newBuilder()
                        .setOk(true)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }
}



