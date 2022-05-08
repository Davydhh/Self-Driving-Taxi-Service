package rest.services;

import rest.beans.RegistrationResponse;
import rest.beans.Taxi;
import rest.beans.Taxis;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("taxis")
public class TaxisService {

    @GET
    @Produces({"application/json", "application/xml"})
    public Response getTaxisList(){
        return Response.ok(Taxis.getInstance()).build();
    }

    @POST
    @Consumes({"application/json", "application/xml"})
    public Response addTaxi(Taxi taxi){
        RegistrationResponse response = Taxis.getInstance().add(taxi);

        if (response != null) {
            return Response.ok(response).build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @Path("{taxi}")
    @DELETE
    public Response deleteUser(@PathParam("taxi") String taxi) {
        int id;

        try {
            id = Integer.parseInt(taxi);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (Taxis.getInstance().delete(id)) {
            return Response.ok().build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }
}