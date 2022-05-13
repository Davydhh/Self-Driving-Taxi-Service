package rest.services;

import rest.beans.Statistics;
import rest.beans.TaxiBean;
import rest.beans.Taxis;
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
}