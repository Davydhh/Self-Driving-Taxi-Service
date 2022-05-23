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

    public synchronized void addTaxi(Taxi.TaxiMessage message,
                                     StreamObserver<Taxi.AddResponse> responseObserver) {
        TaxiBean taxiBean = new TaxiBean(message.getId(), message.getPort(), message.getIp());
        List<TaxiBean> taxiList = taxi.getOtherTaxis();
        taxiList.add(taxiBean);
        System.out.println("Taxi " + taxi.getId() + " other taxis: " + taxi.getOtherTaxis());
        Taxi.AddResponse response =
                Taxi.AddResponse.newBuilder().setAdded(taxiList.contains(taxiBean)).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void sendElection(Taxi.ElectionRequest electionRequest,
                             StreamObserver<Taxi.ElectionResponse> responseObserver) {
        Taxi.RideRequest rideRequest = electionRequest.getRideRequest();

        System.out.println("Taxi " + taxi.getId() + " has received election from " +
                "taxi " + electionRequest.getTaxiId() + " about request " + rideRequest.getId());

        double currentDistance = Utils.getDistance(new Point((int) rideRequest.getStartX(),
                (int) rideRequest.getStartY()), taxi.getStartPos());
        double requestDistance = electionRequest.getTaxiDistance();

        if (currentDistance < requestDistance) {
            System.out.println("Taxi " + taxi.getId() + " has better distance (" + currentDistance +
                    ") than Taxi " + electionRequest.getTaxiId() + " (" + requestDistance + ")"
                    + " about request " + rideRequest.getId());
            Taxi.ElectionResponse response =
                    Taxi.ElectionResponse.newBuilder().setOk(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else if (currentDistance > requestDistance) {
            System.out.println("Taxi " + taxi.getId() + " has worse distance (" + currentDistance +
                    ") than Taxi " + electionRequest.getTaxiId() + " (" + requestDistance + ")"
                    + " about request " + rideRequest.getId());
            Taxi.ElectionResponse response =
                    Taxi.ElectionResponse.newBuilder().setOk(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            System.out.println("Taxi " + taxi.getId() + " and Taxi " + electionRequest.getTaxiId()
                    + " have the same distance: " + currentDistance + " = " + requestDistance
                    + " about request " + rideRequest.getId());
            int currentTaxiBattery = taxi.getBattery();
            int requestTaxiBattery = electionRequest.getTaxiBattery();

            if (currentTaxiBattery > requestTaxiBattery) {
                System.out.println("Taxi " + taxi.getId() + " has better battery (" + currentTaxiBattery +
                        ") than Taxi " + electionRequest.getTaxiId() + " (" + requestTaxiBattery + ")"
                        + " about request " + rideRequest.getId());
                Taxi.ElectionResponse response =
                        Taxi.ElectionResponse.newBuilder().setOk(false).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else if (currentTaxiBattery < requestTaxiBattery) {
                System.out.println("Taxi " + taxi.getId() + " has worse battery (" + currentTaxiBattery +
                        ") than Taxi " + electionRequest.getTaxiId() + " (" + requestTaxiBattery + ")"
                        + " about request " + rideRequest.getId());
                Taxi.ElectionResponse response =
                        Taxi.ElectionResponse.newBuilder().setOk(true).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else {
                System.out.println("Taxi " + taxi.getId() + " and Taxi " + electionRequest.getTaxiId()
                        + " have the same battery: " + currentTaxiBattery + " = " + requestTaxiBattery
                        + " about request " + rideRequest.getId());
                int currentTaxiId = taxi.getId();
                int requestTaxiId = electionRequest.getTaxiId();

                if (currentTaxiId > requestTaxiId) {
                    System.out.println("Taxi " + taxi.getId() + " has greater id " +
                            "than Taxi " + electionRequest.getTaxiId() + " about request " + rideRequest.getId());
                    Taxi.ElectionResponse response =
                            Taxi.ElectionResponse.newBuilder().setOk(false).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } else {
                    System.out.println("Taxi " + taxi.getId() + " has lesser id " +
                            "than Taxi " + electionRequest.getTaxiId() + " about request " + rideRequest.getId());
                    Taxi.ElectionResponse response =
                            Taxi.ElectionResponse.newBuilder().setOk(true).build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }
            }
        }
    }
}
