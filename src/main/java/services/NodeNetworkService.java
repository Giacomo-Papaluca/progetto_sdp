package services;

import beans.Measurement;
import beans.Node;
import beans.NodeNetwork;
import beans.Statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("nodenetwork")
public class NodeNetworkService {

    @Path("add/node")
    @POST
    @Consumes("*/*")
    public Response addNode(Node node){
        NodeNetwork check= NodeNetwork.getInstance().addNode(node);
        if(check!=null){
            return Response.ok(check).build();
        }
        else{
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Path("add/measurement")
    @POST
    @Consumes("*/*")
    public Response addMeasurement(Measurement measurement){
        Statistics.getInstance().addMeasurement(measurement);
        return Response.ok().build();
    }

    @Path("nodes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNodes(){
       return Response.ok(NodeNetwork.getInstance()).build();
    }

    @Path("remove/node/{id}")
    @DELETE
    public Response removeNode(@PathParam("id") String nodeId){
        boolean check=NodeNetwork.getInstance().removeNode(nodeId);
        if(check) {
            return Response.ok().build();
        }
        else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

}
