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
        System.out.println("Taxi " + taxi.getId() + " other taxis: " + taxi.getOtherTaxis());
        Taxi.AddTaxiResponseMessage response =
                Taxi.AddTaxiResponseMessage.newBuilder().setAdded(taxiList.contains(taxiBean)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void sendElection(Taxi.ElectionRequestMessage electionRequest,
                             StreamObserver<Taxi.ElectionResponseMessage> responseObserver) {
        Taxi.RideRequestMessage rideRequest = electionRequest.getRideRequest();

        System.out.println("\nTaxi " + taxi.getId() + " has received election from " +
                "Taxi " + electionRequest.getTaxiId() + " about request " + rideRequest.getId());

        Taxi.ElectionResponseMessage response;

        if (!taxi.getTopic().equals(Utils.getDistrictTopicFromPosition(
                new Point((int) rideRequest.getStartX(), (int) rideRequest.getStartY())))) {
            System.out.println("Taxi " + taxi.getId() + " has received request " + rideRequest.getId() +
                    " from Taxi " + electionRequest.getTaxiId() + " that is from another district");

            response = Taxi.ElectionResponseMessage.newBuilder().setOk(true).build();
        } else if ((taxi.getState() != TaxiState.FREE && taxi.getRequestIdTaken() != rideRequest.getId())) {
            System.out.println("Taxi " + taxi.getId() + " is already driving but for request " + taxi.getRequestIdTaken() +
                    " or is charging");

            response = Taxi.ElectionResponseMessage.newBuilder().setOk(true).build();
        } else if (taxi.getState() == TaxiState.BUSY && taxi.getRequestIdTaken() == rideRequest.getId()) {
            System.out.println("Taxi " + taxi.getId() + " is already driving for request " + taxi.getRequestIdTaken());

            response = Taxi.ElectionResponseMessage.newBuilder().setOk(false).build();
        } else {
            double currentDistance = Utils.getDistance(new Point((int) rideRequest.getStartX(),
                    (int) rideRequest.getStartY()), taxi.getStartPos());
            double requestDistance = electionRequest.getTaxiDistance();

            if (currentDistance < requestDistance) {
                System.out.println("Taxi " + taxi.getId() + " has better distance (" + currentDistance +
                        ") than Taxi " + electionRequest.getTaxiId() + " (" + requestDistance + ")"
                        + " about request " + rideRequest.getId());

                response = Taxi.ElectionResponseMessage.newBuilder().setOk(false).build();
            } else if (currentDistance > requestDistance) {
                System.out.println("Taxi " + taxi.getId() + " has worse distance (" + currentDistance +
                        ") than Taxi " + electionRequest.getTaxiId() + " (" + requestDistance + ")"
                        + " about request " + rideRequest.getId());

                response = Taxi.ElectionResponseMessage.newBuilder().setOk(true).build();
            } else {
                System.out.println("Taxi " + taxi.getId() + " and Taxi " + electionRequest.getTaxiId()
                        + " have the same distance: " + currentDistance + " = " + requestDistance
                        + " about request " + rideRequest.getId());

                int currentTaxiBattery = taxi.getBattery();
                int requestTaxiBattery = electionRequest.getTaxiBattery();

                if (currentTaxiBattery > requestTaxiBattery) {
                    System.out.println("Taxi " + taxi.getId() + " has better battery (" + currentTaxiBattery +
                            "%) than Taxi " + electionRequest.getTaxiId() + " (" + requestTaxiBattery + ")"
                            + " about request " + rideRequest.getId());

                    response = Taxi.ElectionResponseMessage.newBuilder().setOk(false).build();
                } else if (currentTaxiBattery < requestTaxiBattery) {
                    System.out.println("Taxi " + taxi.getId() + " has worse battery (" + currentTaxiBattery +
                            "%) than Taxi " + electionRequest.getTaxiId() + " (" + requestTaxiBattery + ")"
                            + " about request " + rideRequest.getId());

                    response = Taxi.ElectionResponseMessage.newBuilder().setOk(true).build();
                } else {
                    System.out.println("Taxi " + taxi.getId() + " and Taxi " + electionRequest.getTaxiId()
                            + " have the same battery: " + currentTaxiBattery + " = " + requestTaxiBattery
                            + " about request " + rideRequest.getId());

                    int currentTaxiId = taxi.getId();
                    int requestTaxiId = electionRequest.getTaxiId();

                    if (currentTaxiId > requestTaxiId) {
                        System.out.println("Taxi " + taxi.getId() + " has greater id " +
                                "than Taxi " + electionRequest.getTaxiId() + " about request " + rideRequest.getId());

                        response = Taxi.ElectionResponseMessage.newBuilder().setOk(false).build();
                    } else {
                        System.out.println("Taxi " + taxi.getId() + " has lesser id " +
                                "than Taxi " + electionRequest.getTaxiId() + " about request " + rideRequest.getId());

                        response = Taxi.ElectionResponseMessage.newBuilder().setOk(true).build();
                    }
                }
            }
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void sendChargingRequest(Taxi.ChargingRequestMessage chargingRequest,
                                    StreamObserver<Taxi.ChargingResponseMessage> responseObserver) {
        int senderTaxiId = chargingRequest.getTaxiId();
        int stationId = chargingRequest.getStationId();

        Taxi.ChargingResponseMessage response;

        System.out.println("\nTaxi " + taxi.getId() + " has received charging request from Taxi " + senderTaxiId +
                " about station " + chargingRequest.getStationId());

        if (taxi.getState() == TaxiState.CHARGING && taxi.getRechargeStationId() == stationId) {
            System.out.println("Taxi " + taxi.getId() + " is just charging on station " + stationId);

            waitUntilChargingCompleted(responseObserver, stationId);
        } else if (taxi.getState() == TaxiState.CHARGING && taxi.getRechargeStationId() != stationId) {
            System.out.println("Taxi " + taxi.getId() + " is charging but on station " + stationId);

            response = Taxi.ChargingResponseMessage.newBuilder()
                    .setOk(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else if (taxi.getState() != TaxiState.CHARGING && taxi.getRechargeStationId() == stationId) {
            System.out.println("Taxi " + taxi.getId() + " is not charging but want charging in " +
                    "the same station " + stationId);

            if (taxi.getRechargeRequestTimestamp() < chargingRequest.getTimestamp()) {
                System.out.println("Taxi " + taxi.getId() + " has timestamp " + taxi.getRechargeRequestTimestamp() +
                        "lesser than Taxi " + chargingRequest.getTaxiId() + " timestamp " + chargingRequest.getTimestamp());

                waitUntilChargingCompleted(responseObserver, stationId);
            } else {
                System.out.println("Taxi " + taxi.getId() + " has timestamp " + taxi.getRechargeRequestTimestamp() +
                        " greater than Taxi " + chargingRequest.getTaxiId() + " timestamp " + chargingRequest.getTimestamp());
                response = Taxi.ChargingResponseMessage.newBuilder()
                        .setOk(true)
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        } else {
            System.out.println("Taxi " + taxi.getId() + " is not charging on station " + stationId);

            response = Taxi.ChargingResponseMessage.newBuilder()
                    .setOk(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private void waitUntilChargingCompleted(StreamObserver<Taxi.ChargingResponseMessage> responseObserver, int stationId) {
        System.out.println("Waiting for the recharge station " + stationId + " to finish ");

        synchronized (taxi.getChargingLock()) {
            while (taxi.getState() == TaxiState.CHARGING) {
                try {
                    taxi.getChargingLock().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (taxi.getState() != TaxiState.CHARGING) {
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
}
