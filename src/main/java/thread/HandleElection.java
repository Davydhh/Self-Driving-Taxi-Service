package thread;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import model.Taxi;
import model.TaxiState;
import rest.beans.RideRequest;
import rest.beans.TaxiBean;
import seta.proto.taxi.Taxi.ElectionRequestMessage;
import seta.proto.taxi.Taxi.ElectionResponseMessage;
import seta.proto.taxi.Taxi.RideRequestMessage;
import seta.proto.taxi.TaxiServiceGrpc;

import java.util.List;

import static util.Utils.getDistance;

public class HandleElection extends Thread {
    private final Taxi taxi;

    private final RideRequest request;

    private int okCounter;

    private final Object counterLock;

    public HandleElection(Taxi taxi, RideRequest request) {
        this.taxi = taxi;
        this.request = request;
        this.okCounter = 0;
        this.counterLock = new Object();
    }

    @Override
    public void run() {
        System.out.println("Starting election for request " + request.getId());

        double distance = getDistance(request.getStartPos(), taxi.getStartPos());

        RideRequestMessage rideRequest = RideRequestMessage.newBuilder()
                .setId(request.getId())
                .setStartX(request.getStartPos().getX())
                .setStartY(request.getStartPos().getY())
                .setEndX(request.getEndPos().getX())
                .setEndY(request.getEndPos().getY())
                .build();

        ElectionRequestMessage electionRequest = ElectionRequestMessage.newBuilder()
                .setTaxiId(taxi.getId())
                .setTaxiBattery(taxi.getBattery())
                .setTaxiDistance(distance)
                .setRideRequest(rideRequest)
                .build();

        List<TaxiBean> taxiList = taxi.getOtherTaxis();

        if (taxiList.isEmpty()) {
            System.out.println("Taxi " + taxi.getId() + " takes charge of the ride " + request.getId());
            taxi.setState(TaxiState.BUSY);
            taxi.setRequestIdTaken(request.getId());
            taxi.drive(request);
        } else {
            synchronized (taxi.getOtherTaxisLock()) {
                for (TaxiBean t : taxiList) {
                    final ManagedChannel channel =
                            ManagedChannelBuilder.forTarget(t.getIp() + ":" + t.getPort()).usePlaintext().build();
                    System.out.println("Taxi " + taxi.getId() + " send election to taxi " + t.getId() + " about request " + request.getId());
                    TaxiServiceGrpc.TaxiServiceStub stub = TaxiServiceGrpc.newStub(channel);

                    stub.sendElection(electionRequest, new StreamObserver<ElectionResponseMessage>() {
                        @Override
                        public void onNext(ElectionResponseMessage value) {
                            if (!value.getOk()) {
                                System.out.println("Taxi " + taxi.getId() + " did not receive ok from " +
                                        "Taxi " + t.getId() + " about request " + request.getId());
                            } else {
                                System.out.println("Taxi " + taxi.getId() + " received ok from Taxi " + t.getId()
                                        + " about request " + request.getId());

                                synchronized (counterLock) {
                                    okCounter += 1;

                                    if (okCounter == taxiList.size()) {
                                        taxi.setState(TaxiState.BUSY);
                                        taxi.setRequestIdTaken(request.getId());
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
            }

            waitUntilReceiveAllOk();
        }
    }

    private void waitUntilReceiveAllOk() {
        System.out.println("Taxi " + taxi.getId() + " wait for receiving ok for ride request " + request.getId());

        synchronized (counterLock) {
            while (taxi.getState() != TaxiState.BUSY) {
                try {
                    counterLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (taxi.getState() == TaxiState.BUSY && taxi.getRequestIdTaken() == request.getId()) {
                    System.out.println("Taxi " + taxi.getId() + " takes charge of the ride " + request.getId());
                    taxi.drive(request);
                }
            }
        }
    }
}



