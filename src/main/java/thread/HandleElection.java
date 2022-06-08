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
import java.util.stream.Collectors;

import static model.TaxiState.HANDLING_RIDE;
import static util.Utils.getDistance;

public class HandleElection extends Thread {
    private final Taxi taxi;

    private final RideRequest request;

    private final List<Integer> taxiIdList;

    private final ElectionRequestMessage electionRequest;

    public HandleElection(Taxi taxi, RideRequest request) {
        this.taxi = taxi;
        this.request = request;
        this.taxiIdList = new ArrayList<>();
        RideRequestMessage rideRequest = RideRequestMessage.newBuilder()
                .setId(request.getId())
                .setStartX(request.getStartPos().getX())
                .setStartY(request.getStartPos().getY())
                .setEndX(request.getEndPos().getX())
                .setEndY(request.getEndPos().getY())
                .build();
        this.electionRequest = ElectionRequestMessage.newBuilder()
                .setTaxiId(taxi.getId())
                .setTaxiBattery(taxi.getBattery())
                .setTaxiDistance(getDistance(request.getStartPos(), taxi.getStartPos()))
                .setRideRequest(rideRequest)
                .build();
    }

    public List<Integer> getTaxiIdList() {
        return taxiIdList;
    }

    @Override
    public void run() {
        System.out.println("Starting election for request " + request.getId());

        taxi.resetOkCounter();
        taxi.setRequestId(request.getId());

        List<TaxiBean> taxiList = new ArrayList<>(taxi.getOtherTaxis());
        taxiIdList.addAll(taxiList.stream().map(TaxiBean::getId).collect(Collectors.toList()));

        int size = taxiList.size();

        if (taxiList.isEmpty()) {
            System.out.println("\nTaxi " + taxi.getId() + " takes charge of the ride " + request.getId());
            taxi.drive(request);
        } else {
            synchronized (taxi.getOkCounterLock()) {
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
                                if (taxi.getState() == HANDLING_RIDE && taxi.getRequestId() == request.getId()) {
                                    taxi.setRequestId(-1);
                                    taxi.handlePendingRequests();
                                }

                                synchronized (taxi.getOkCounterLock()) {
                                    taxi.getOkCounterLock().notify();
                                }
                            } else {
                                System.out.println("Taxi " + taxi.getId() + " received ok from Taxi " + t.getId()
                                        + " about request " + request.getId());

                                synchronized (taxi.getOkCounterLock()) {
                                    taxi.incrementCounter();

                                    if (taxi.getOkCounter() == size) {
                                        System.out.println("Received all ack!");
                                        taxi.getOkCounterLock().notify();
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            System.out.println("Taxi " + taxi.getId() + " received ok from a Taxi" +
                                    " that has leaved about request " + request.getId());

                            synchronized (taxi.getOkCounterLock()) {
                                taxi.incrementCounter();

                                if (taxi.getOkCounter() == size) {
                                    taxi.getOkCounterLock().notify();
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

        try {
            taxi.getOkCounterLock().wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (taxi.getOkCounter() == size && taxi.getRequestId() == request.getId() && !taxi.isLeaving()) {
            System.out.println("Taxi " + taxi.getId() + " takes charge of the ride " + request.getId());
            taxi.drive(request);
        } else if (taxi.isLeaving()) {
            System.out.println("Taxi is leaving before election finished");
            synchronized (taxi.getStateLock()) {
                taxi.setState(TaxiState.BUSY);
                taxi.getStateLock().notifyAll();

                taxi.setState(TaxiState.FREE);
            }
        }
    }

    public void addTaxi(int addTaxiId) {
        System.out.println("The Taxi " + addTaxiId + " has been added to election");

        TaxiBean taxiToAdd =
                taxi.getOtherTaxis().stream().filter(t -> t.getId() == addTaxiId).findFirst().orElse(null);

        if (taxiToAdd != null) {
            final ManagedChannel channel =
                    ManagedChannelBuilder.forTarget(taxiToAdd.getIp() + ":" + taxiToAdd.getPort()).usePlaintext().build();
            System.out.println("Taxi " + taxi.getId() + " send election to taxi " + addTaxiId + " about " +
                    "request " + request.getId());
            TaxiServiceGrpc.TaxiServiceStub stub = TaxiServiceGrpc.newStub(channel);

            stub.sendElection(electionRequest, new StreamObserver<ElectionResponseMessage>() {
                @Override
                public void onNext(ElectionResponseMessage value) {
                    if (!value.getOk()) {
                        System.out.println("Taxi " + taxi.getId() + " did not receive ok from " +
                                "Taxi " + taxiToAdd.getId() + " about request " + request.getId());
                        taxi.removeRequest(request);
                        if (taxi.getState() == HANDLING_RIDE && taxi.getRequestId() == request.getId()) {
                            taxi.setRequestId(-1);
                            taxi.handlePendingRequests();
                        }

                        synchronized (taxi.getOkCounterLock()) {
                            taxi.getOkCounterLock().notify();
                        }
                    } else {
                        System.out.println("Taxi " + taxi.getId() + " received ok from Taxi " + taxiToAdd.getId()
                                + " about request " + request.getId());

                        synchronized (taxi.getOkCounterLock()) {
                            taxi.incrementCounter();

                            if (taxi.getOkCounter() == taxiIdList.size()) {
                                System.out.println("Received all ack!");
                                taxi.getOkCounterLock().notify();
                            }
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Taxi " + taxi.getId() + " received ok from a Taxi" +
                            " that has leaved about request " + request.getId());

                    synchronized (taxi.getOkCounterLock()) {
                        taxi.incrementCounter();

                        if (taxi.getOkCounter() == taxiIdList.size()) {
                            taxi.getOkCounterLock().notify();
                        }
                    }
                }

                @Override
                public void onCompleted() {
                    channel.shutdownNow();
                }
            });
        }
    }
}



