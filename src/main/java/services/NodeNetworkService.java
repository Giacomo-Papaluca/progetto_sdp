package services;

import beans.Measurement;
import beans.Node;
import beans.NodeNetwork;
import beans.Statistics;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("nodenetwork")
public class NodeNetworkService {

    @Path("add/node")
    @POST
    @Consumes("*/*")
    public List<Node> addNode(Node node){
        List<Node> check= NodeNetwork.getInstance().addNode(node);
        return check;
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
