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

import java.util.ArrayList;
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

        taxi.setRequestId(request.getId());

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

        List<TaxiBean> taxiList = new ArrayList<>(taxi.getOtherTaxis());

        int size = taxiList.size();

        if (taxiList.isEmpty()) {
            System.out.println("\nTaxi " + taxi.getId() + " takes charge of the ride " + request.getId());
            taxi.drive(request);
        } else {
            synchronized (counterLock) {
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
                                taxi.removeRequest(request);
                                taxi.setState(TaxiState.FREE);
                                taxi.setRequestId(-1);
                            } else {
                                System.out.println("Taxi " + taxi.getId() + " received ok from Taxi " + t.getId()
                                        + " about request " + request.getId());

                                synchronized (counterLock) {
                                    okCounter += 1;

                                    if (okCounter == size) {
                                        counterLock.notifyAll();
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            System.out.println("Taxi " + taxi.getId() + " received ok from a Taxi" +
                                    " that has leaved about request " + request.getId());

                            synchronized (counterLock) {
                                okCounter += 1;

                                if (okCounter == size) {
                                    counterLock.notifyAll();
                                }
                            }
                        }

                        @Override
                        public void onCompleted() {
                            channel.shutdownNow();
                        }
                    });
                }

                waitUntilReceiveAllOk(size);
            }
        }
    }

    private void waitUntilReceiveAllOk(int size) {
        System.out.println("\nTaxi " + taxi.getId() + " wait for receiving ok for ride request " + request.getId());

        while (okCounter < size) {
            try {
                counterLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (okCounter == size && taxi.getRequestId() == request.getId()) {
                System.out.println("Taxi " + taxi.getId() + " takes charge of the ride " + request.getId());
                taxi.drive(request);
            }
        }
    }
}



