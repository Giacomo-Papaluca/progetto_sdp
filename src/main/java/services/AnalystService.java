package services;

import beans.NodeNetwork;
import beans.Statistics;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("analyst")
public class AnalystService {

    @Path("get/size")
    @GET
    @Produces("*/*")
    public Response getNumberOfNodes(){
        return Response.ok(NodeNetwork.getInstance().countNodes()).build();
    }

    @Path("get/statistics/{n}")
    @GET
    @Produces("*/*")
    public Response getLastStatistics(@PathParam("n") int n){
        return Response.ok(Statistics.getInstance().getLastStatistics(n)).build();
    }

    @Path("get/mean/{n}")
    @GET
    @Produces("*/*")
    public Response getMean(@PathParam("n") int n){
        return Response.ok(Statistics.getInstance().getMean(n)).build();
    }

    @Path("get/std/{n}")
    @GET
    @Produces("*/*")
    public Response getStd(@PathParam("n") int n){
        return Response.ok(Statistics.getInstance().getStd(n)).build();
    }

}
