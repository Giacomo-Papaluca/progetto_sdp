package services;

import beans.Measurement;
import beans.NodeNetwork;
import beans.Statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("analyst")
public class AnalystService {

    @Path("get/size")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNumberOfNodes(){
        Integer val=NodeNetwork.getInstance().countNodes();
        GenericEntity<Integer> genericEntity = new GenericEntity<Integer>(val){};
        return Response.ok(genericEntity).build();
    }

    @Path("get/statistics/{n}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastStatistics(@PathParam("n") int n){
        List<Measurement> list= Statistics.getInstance().getLastStatistics(n);
        GenericEntity<List<Measurement>> genericEntity= new GenericEntity<List<Measurement>>(list){};
        return Response.ok(genericEntity).build();
    }

    @Path("get/mean/{n}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMean(@PathParam("n") int n){
        Float mean=Statistics.getInstance().getMean(n);
        GenericEntity<Float> genericEntity= new GenericEntity<Float>(mean){};
        return Response.ok(genericEntity).build();
    }

    @Path("get/std/{n}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStd(@PathParam("n") int n){
        Float std=Statistics.getInstance().getStd(n);
        GenericEntity<Float> genericEntity= new GenericEntity<Float>(std){};
        return Response.ok(genericEntity).build();
    }

}
