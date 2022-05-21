package thread;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.Taxi;
import model.TaxiServiceImpl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TaxiGrpcServer extends Thread {
    private final Taxi taxi;

    public TaxiGrpcServer(Taxi taxi) {
        this.taxi = taxi;
    }

    @Override
    public void run() {
        startGrpcServer();
    }

    private void startGrpcServer() {
        Server server =
                ServerBuilder.forPort(taxi.getPort()).addService(new TaxiServiceImpl(taxi)).build();

        try {
            server.start();
            System.out.println("Taxi " + taxi.getId() + " server started on port " + taxi.getPort());

            server.awaitTermination(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            System.exit(0);}
    }
}
