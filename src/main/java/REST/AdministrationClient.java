package rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import java.util.InputMismatchException;
import java.util.Scanner;

public class AdministrationClient {

    public static void main(String[] args){
        Client client = Client.create();
        String serverAddress = "http://localhost:1337";

        String[] services = {"1 - Get list of taxis",
                "2 - Get the average of the last n local statistics of a given Taxi",
                "3 - Get the average of the previously introduced statistics provided by all the taxis " +
                        "and occurred from timestamps t1 and t2",
                "4 - Exit"};

        Scanner scanner = new Scanner(System.in);

        System.out.println("\nAdministration Server services available:");
        int choice = 0;
        while (choice != 4) {
            for (String service : services) {
                System.out.println(service);
            }

            System.out.print("\nChoose one service: ");

            try {
                choice = scanner.nextInt();
                switch (choice) {
                    case 1:
                        getTaxiList(client, serverAddress);
                        break;
                    case 2:
                        System.out.println("Insert the id of the taxi of your interest");
                        int taxiId = scanner.nextInt();
                        System.out.println("Now insert the number of the last statistics you want");
                        int n = scanner.nextInt();
                        getLastTaxiStatistics(client, serverAddress, taxiId, n);
                        break;
                    case 3:
                        System.out.println("Insert t1");
                        long t1 = scanner.nextLong();
                        System.out.println("Insert t2");
                        long t2 = scanner.nextLong();
                        getAllTaxiStatisticsBetweenTimestamps(client, serverAddress, t1, t2);
                        break;
                    default:
                        System.exit(0);
                }
            } catch (InputMismatchException ex){
                System.out.println("\nInsert a number!");
                scanner.next();
            }
            catch (Exception ex){
                ex.printStackTrace();
                scanner.next();
            }

        }
    }

    public static ClientResponse getRequest(Client client, String url){
        WebResource webResource = client.resource(url);
        try {
            return webResource.type("application/json").get(ClientResponse.class);
        } catch (ClientHandlerException e) {
            System.out.println("Server not available");
            return null;
        }
    }

    private static void getTaxiList(Client client, String url) {
        ClientResponse response = getRequest(client, url + "/taxis");

        if (response == null || response.getStatus() != 200) {
            System.out.println("Server error");
            System.out.println();

            return;
        }

        System.out.println("\nThe taxis are:");
        System.out.println(response.getEntity(String.class));
        System.out.println();
    }

    private static void getLastTaxiStatistics(Client client, String url, int taxiId, int n) {
        ClientResponse response = getRequest(client, url + "/statistics/" + taxiId + "?n=" + n);

        if (response == null) {
            System.out.println("Server error");
            System.out.println();

            return;
        }

        switch (response.getStatus()) {
            case 200:
                System.out.println("\nThe last " + n + " statistics of the taxi " + taxiId + " " +
                        "are:");
                System.out.println(response.getEntity(String.class));
                System.out.println();
                break;
            case 403:
                System.out.println("\nServer error " + response.getEntity(String.class));
                System.out.println();
                break;
            default:
                System.out.println("\nServer error taxi " + taxiId + " not found");
                System.out.println();
        }
    }

    private static void getAllTaxiStatisticsBetweenTimestamps(Client client, String url, long t1,
                                                              long t2) {
        ClientResponse response = getRequest(client, url + "/statistics/?t1=" + t1 + "&t2=" + t2);

        if (response == null) {
            System.out.println("Server error");
            System.out.println();

            return;
        }

        int statusCode = response.getStatus();

        if (statusCode == 403) {
            System.out.println("\nServer error " + response.getEntity(String.class));
            System.out.println();
            return;
        }

        System.out.println("\nAll statistics between " + t1 + " and " + t2 + " are:");
        System.out.println(response.getEntity(String.class));
        System.out.println();
    }
}
