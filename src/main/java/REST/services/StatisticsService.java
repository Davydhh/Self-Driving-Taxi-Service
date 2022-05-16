package rest.services;

import rest.beans.Statistics;
import rest.beans.TaxisStatistics;

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

        if (response == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (response.equals("403")) {
            return Response.status(Response.Status.FORBIDDEN).entity("Not enough statistics").build();
        }

        return Response.ok(response).build();
    }

    @GET
    public Response getAllStatisticsBetweenTimestamps(@QueryParam("t1") long t1,
                                                      @QueryParam("t2") long t2) {
        if (t1 >= t2) {
            return Response.status(Response.Status.FORBIDDEN).entity("t2 must be greater than " +
                    "t1").build();
        }

        String response = TaxisStatistics.getInstance().getAllStatisticsBetweenTimestamps(t1, t2);

        return Response.ok(response).build();
    }
}