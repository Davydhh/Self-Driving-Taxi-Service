package model;

import io.grpc.stub.StreamObserver;
import rest.beans.TaxiBean;
import seta.proto.taxi.Taxi;
import seta.proto.taxi.TaxiServiceGrpc.TaxiServiceImplBase;

public class TaxiServiceImpl extends TaxiServiceImplBase {
    private final model.Taxi taxi;

    public TaxiServiceImpl(model.Taxi taxi) {
        this.taxi = taxi;
    }

    public synchronized void addTaxi(Taxi.TaxiMessage message,
                                     StreamObserver<Taxi.AddResponse> responseObserver) {
        TaxiBean taxiBean = new TaxiBean(message.getId(), message.getPort(), message.getIp());
        taxi.getOtherTaxis().add(taxiBean);
        System.out.println("Taxi " + taxi.getId() + " other taxis: " + taxi.getOtherTaxis());
        Taxi.AddResponse response = Taxi.AddResponse.newBuilder().setAdded(true).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
