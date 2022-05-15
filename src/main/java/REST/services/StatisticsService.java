package rest.services;

import com.google.gson.Gson;
import rest.beans.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("statistics")
public class StatisticsService {
    @POST
    @Path("{taxi}")
    @Consumes({"application/json", "application/xml"})
    @Produces({"application/json", "application/xml"})
    public Response addStatistics(@PathParam("taxi") String taxi, Statistics statistics) {
        int id;

        try {
            id = Integer.parseInt(taxi);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        TaxisStatistics.getInstance().add(id, statistics);
        return Response.ok().build();
    }

    @GET
    @Path("{taxi}")
    public Response getLastStatistics(@PathParam("taxi") String taxi, @QueryParam("n") int n) {
        int id;

        try {
            id = Integer.parseInt(taxi);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String response = TaxisStatistics.getInstance().getLastAverageStatisticsByTaxi(id, n);
        return Response.ok(response).build();
    }
}