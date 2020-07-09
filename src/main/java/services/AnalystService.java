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
    public String getNumberOfNodes(){
        int val=NodeNetwork.getInstance().countNodes();
        return String.valueOf(val);
    }

    @Path("get/statistics/{n}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastStatistics(@PathParam("n") int n){
        List<Measurement> list= Statistics.getInstance().getLastStatistics(n);
        GenericEntity<List<Measurement>> genericEntity= new GenericEntity<List<Measurement>>(list){};
        return Response.ok().entity(genericEntity).build();
    }

    @Path("get/mean/{n}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMean(@PathParam("n") int n){
        float mean =Statistics.getInstance().getMean(n);
        if(mean>=0) {
            return String.valueOf(mean);
        }
        else {
            return "NaN";
        }
    }

    @Path("get/std/{n}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getStd(@PathParam("n") int n){
        float std=Statistics.getInstance().getStd(n);
        if(std>=0){
           return String.valueOf(std);
        }
        else {
            return "NaN";
        }
    }

}
