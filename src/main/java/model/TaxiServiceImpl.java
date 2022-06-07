package model;

import io.grpc.stub.StreamObserver;
import rest.beans.TaxiBean;
import seta.proto.taxi.Taxi;
import seta.proto.taxi.TaxiServiceGrpc.TaxiServiceImplBase;
import util.Utils;

import java.awt.*;
import java.util.List;

public class TaxiServiceImpl extends TaxiServiceImplBase {
    private final model.Taxi taxi;

    public TaxiServiceImpl(model.Taxi taxi) {
        this.taxi = taxi;
    }

    public void addTaxi(Taxi.TaxiMessage message,
                        StreamObserver<Taxi.AddTaxiResponseMessage> responseObserver) {
        TaxiBean taxiBean = new TaxiBean(message.getId(), message.getPort(), message.getIp());
        taxi.addTaxi(taxiBean);
        List<TaxiBean> taxiList = taxi.getOtherTaxis();
        System.out.println("Taxi " + taxi.getId() + " other taxis: " + taxiList);
        Taxi.AddTaxiResponseMessage response =
                Taxi.AddTaxiResponseMessage.newBuilder().setAdded(taxiList.contains(taxiBean)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void removeTaxi(Taxi.TaxiMessage message,
                           StreamObserver<Taxi.RemoveTaxiResponseMessage> responseObserver) {
        TaxiBean taxiBean = new TaxiBean(message.getId(), message.getPort(), message.getIp());
        taxi.removeTaxi(taxiBean);
        List<TaxiBean> taxiList = taxi.getOtherTaxis();
        System.out.println("Taxi " + taxi.getId() + " other taxis: " + taxiList);
        Taxi.RemoveTaxiResponseMessage response =
                Taxi.RemoveTaxiResponseMessage.newBuilder().setRemoved(!taxiList.contains(taxiBean)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void sendElection(Taxi.ElectionRequestMessage electionRequest,
                             StreamObserver<Taxi.ElectionResponseMessage> responseObserver) {
        Taxi.RideRequestMessage rideRequest = electionRequest.getRideRequest();

        int taxiId = taxi.getId();
        int requestTaxiId = electionRequest.getTaxiId();
        int requestId = rideRequest.getId();

        System.out.println("\nTaxi " + taxiId + " has received election from " +
                "Taxi " + requestTaxiId + " about request " + requestId);

        synchronized (taxi.getStateLock()) {
            if (!taxi.getTopic().equals(Utils.getDistrictTopicFromPosition(
                    new Point((int) rideRequest.getStartX(), (int) rideRequest.getStartY())))) {
                System.out.println("Taxi " + taxiId + " has received request " + requestId +
                        " from Taxi " + requestTaxiId + " that is from another district");

                sendResponse(true, responseObserver);
            } else if (taxi.getState() == TaxiState.BUSY && taxi.getRequestId() != requestId) {
                System.out.println("Taxi " + taxiId + " is already driving but for request " + taxi.getRequestId());

                sendResponse(true, responseObserver);
            } else if (taxi.getState() == TaxiState.CHARGING) {
                System.out.println("Taxi " + taxiId + " is charging");

                sendResponse(true, responseObserver);
            } else if (taxi.getState() == TaxiState.NEED_RECHARGE) {
                System.out.println("Taxi " + taxiId + " is low");

                sendResponse(true, responseObserver);
            } else if (taxi.getState() == TaxiState.LEAVING) {
                System.out.println("Taxi " + taxiId + " is leaving");

                sendResponse(true, responseObserver);
            } else if (taxi.getState() == TaxiState.HANDLING_RIDE && taxi.getRequestId() != requestId) {
                System.out.println("Taxi " + taxiId + " is handling another ride");

                sendResponse(true, responseObserver);
            } else if (taxi.getState() == TaxiState.BUSY && taxi.getRequestId() == requestId) {
                System.out.println("Taxi " + taxiId + " is already driving for request " + taxi.getRequestId());

                sendResponse(false, responseObserver);
            } else {
                double currentDistance = Utils.getDistance(new Point((int) rideRequest.getStartX(),
                        (int) rideRequest.getStartY()), taxi.getStartPos());
                double requestDistance = electionRequest.getTaxiDistance();

                if (currentDistance < requestDistance) {
                    System.out.println("Taxi " + taxiId + " has better distance (" + currentDistance +
                            ") than Taxi " + requestTaxiId + " (" + requestDistance + ")"
                            + " about request " + requestId);

                    waitUntilWinElection(responseObserver, requestId);
                } else if (currentDistance > requestDistance) {
                    System.out.println("Taxi " + taxiId + " has worse distance (" + currentDistance +
                            ") than Taxi " + requestTaxiId + " (" + requestDistance + ")"
                            + " about request " + requestId);

                    sendResponse(true, responseObserver);
                } else {
                    System.out.println("Taxi " + taxiId + " and Taxi " + requestTaxiId
                            + " have the same distance: " + currentDistance + " = " + requestDistance
                            + " about request " + requestId);

                    int currentTaxiBattery = taxi.getBattery();
                    int requestTaxiBattery = electionRequest.getTaxiBattery();

                    if (currentTaxiBattery > requestTaxiBattery) {
                        System.out.println("Taxi " + taxiId + " has better battery (" + currentTaxiBattery +
                                "%) than Taxi " + requestTaxiId + " (" + requestTaxiBattery + ")"
                                + " about request " + requestId);

                        waitUntilWinElection(responseObserver, requestId);
                    } else if (currentTaxiBattery < requestTaxiBattery) {
                        System.out.println("Taxi " + taxiId + " has worse battery (" + currentTaxiBattery +
                                "%) than Taxi " + requestTaxiId + " (" + requestTaxiBattery + ")"
                                + " about request " + requestId);

                        sendResponse(true, responseObserver);
                    } else {
                        System.out.println("Taxi " + taxiId + " and Taxi " + requestTaxiId
                                + " have the same battery: " + currentTaxiBattery + " = " + requestTaxiBattery
                                + " about request " + requestId);

                        if (taxiId > requestTaxiId) {
                            System.out.println("Taxi " + taxiId + " has greater id " +
                                    "than Taxi " + requestTaxiId + " about request " + requestId);

                            waitUntilWinElection(responseObserver, requestId);
                        } else {
                            System.out.println("Taxi " + taxiId + " has lesser id " +
                                    "than Taxi " + requestTaxiId + " about request " + requestId);

                            sendResponse(true, responseObserver);
                        }
                    }
                }
            }
        }
    }

    private void sendResponse(boolean value, StreamObserver<Taxi.ElectionResponseMessage> responseObserver) {
        Taxi.ElectionResponseMessage response =
                Taxi.ElectionResponseMessage.newBuilder().setOk(value).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void waitUntilWinElection(StreamObserver<Taxi.ElectionResponseMessage> responseObserver,
                                      int requestId) {
        System.out.println("\nWaiting for win the election for the ride " + requestId);

        while (taxi.getState() != TaxiState.BUSY) {
            try {
                taxi.getStateLock().wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (taxi.getState() == TaxiState.BUSY) {
                sendResponse(taxi.isLeaving(), responseObserver);
            }
        }
    }

    public void sendChargingRequest(Taxi.ChargingRequestMessage chargingRequest,
                                    StreamObserver<Taxi.ChargingResponseMessage> responseObserver) {
        int senderTaxiId = chargingRequest.getTaxiId();
        int stationId = chargingRequest.getStationId();
        int taxiId = taxi.getId();

        Taxi.ChargingResponseMessage response;

        System.out.println("\nTaxi " + taxiId + " has received charging request from Taxi " + senderTaxiId +
                " about station " + stationId);

        synchronized (taxi.getStateLock()) {
            if (taxi.getState() == TaxiState.CHARGING && taxi.getRechargeStationId() == stationId) {
                System.out.println("Taxi " + taxiId + " is just charging on station " + stationId);

                waitUntilChargingCompleted(responseObserver, stationId);
            } else if (taxi.getState() == TaxiState.CHARGING && taxi.getRechargeStationId() != stationId) {
                System.out.println("Taxi " + taxiId + " is charging but on station " + stationId);

                response = Taxi.ChargingResponseMessage.newBuilder()
                        .setOk(true)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else if (taxi.getState() != TaxiState.CHARGING && taxi.getRechargeStationId() == stationId) {
                System.out.println("Taxi " + taxiId + " is not charging but want charging in " +
                        "the same station " + stationId);

                if (taxi.getRechargeRequestTimestamp() < chargingRequest.getTimestamp()) {
                    System.out.println("Taxi " + taxiId + " has timestamp " + taxi.getRechargeRequestTimestamp() +
                            "lesser than Taxi " + chargingRequest.getTaxiId() + " timestamp " + chargingRequest.getTimestamp());

                    waitUntilChargingCompleted(responseObserver, stationId);
                } else {
                    System.out.println("Taxi " + taxiId + " has timestamp " + taxi.getRechargeRequestTimestamp() +
                            " greater than Taxi " + chargingRequest.getTaxiId() + " timestamp " + chargingRequest.getTimestamp());
                    response = Taxi.ChargingResponseMessage.newBuilder()
                            .setOk(true)
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            } else {
                System.out.println("Taxi " + taxiId + " is not charging on station " + stationId);

                response = Taxi.ChargingResponseMessage.newBuilder()
                        .setOk(true)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }

    private void waitUntilChargingCompleted(StreamObserver<Taxi.ChargingResponseMessage> responseObserver, int stationId) {
        System.out.println("\nWaiting for the recharge station " + stationId + " to finish ");

        while (taxi.getState() == TaxiState.CHARGING) {
            try {
                taxi.getStateLock().wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (taxi.getState() != TaxiState.CHARGING && taxi.getBattery() == 100) {
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
