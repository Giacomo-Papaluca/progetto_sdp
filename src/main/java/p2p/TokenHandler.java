package p2p;

import beans.Measurement;
import beans.Node;
import com.google.protobuf.Descriptors;
import com.netHandler.RingNetworkHandler;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.tokenHandler.SendTokenGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.ws.rs.core.MediaType;
import java.net.URI;

public class TokenHandler extends SendTokenGrpc.SendTokenImplBase implements Runnable{

    boolean createToken;
    boolean exiting;
    Node node;
    Node destination;
    int networkSize;
    ThreadHandler threadHandler;
    BufferImpl buffer;
    long lastMeasurementTimestamp;
    public static String gateway="localhost";
    public static int gatewayPort=8080;
    private static final String resource= "/SDP_papaluca_2020_war_exploded";
    private static final Client client= Client.create();

    public TokenHandler(BufferImpl buffer){
        this.buffer=buffer;
        exiting=false;
        destination=new Node();
        networkSize=0;
        lastMeasurementTimestamp=0;
        createToken=false;
    }

    public void setThreadHandler(ThreadHandler threadHandler){
        this.threadHandler=threadHandler;
    }

    public void setNode(Node node) {
        this.node=node;
    }

    public void setDestination(Node destination){
        synchronized (destination){this.destination=destination;}
    }

    public Node getDestination(){synchronized (destination) {return this.destination;}}

    public synchronized void setNetworkSize(int size){
        networkSize=size;
    }

    public static com.tokenHandler.TokenHandler.Node nodeBeanToMessage(Node node){
        return com.tokenHandler.TokenHandler.Node.newBuilder().setPort(node.getPort()).setAddress(node.getAddress()).setId(node.getId()).build();
    }

    @Override
    public void run() {
        while (networkSize==1){
            while (buffer.getMostRecentTimestamp()<=lastMeasurementTimestamp){
                buffer.waitForMeasurement();
            }
            lastMeasurementTimestamp=buffer.getMostRecentTimestamp();
            Measurement measurement=new Measurement(node.getId(), buffer.getAggregatedValue(), System.currentTimeMillis());
            WebResource webResource=client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/add/measurement"));
            ClientResponse r=webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, measurement);
            System.out.println(r.getStatus()+": sono da solo, invio misuraz");
        }
        threadHandler.waitForDestination();
        if(createToken){
            System.out.println("token generato");
            com.tokenHandler.TokenHandler.Token token;
            if(buffer.getMostRecentTimestamp()>0){
                token= com.tokenHandler.TokenHandler.Token.newBuilder().setAggregatedValue(buffer.getAggregatedValue())
                        .setStarter(nodeBeanToMessage(node)).setHopCount(networkSize).build();
                lastMeasurementTimestamp=buffer.getMostRecentTimestamp();
            }
            else{
                token=com.tokenHandler.TokenHandler.Token.newBuilder().setHopCount(0).build();
            }
            trySend(token);
        }
        threadHandler.waitForUser();
        threadHandler.waitForTokenRelease();
    }


    @Override
    public void send(com.tokenHandler.TokenHandler.Token token, StreamObserver<com.tokenHandler.TokenHandler.TokenResponse> responseObserver) {
        threadHandler.acquiredToken();
        if(!exiting){
            com.tokenHandler.TokenHandler.TokenResponse response=com.tokenHandler.TokenHandler.TokenResponse.newBuilder().setOk(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            int hopCount = token.getHopCount();
            if (hopCount == 0) {
                com.tokenHandler.TokenHandler.Token.Builder builder = com.tokenHandler.TokenHandler.Token.newBuilder();
                if (buffer.getMostRecentTimestamp() > lastMeasurementTimestamp) {
                    lastMeasurementTimestamp=buffer.getMostRecentTimestamp();
                    builder.setStarter(nodeBeanToMessage(node)).setAggregatedValue(buffer.getAggregatedValue())
                            .setHasAggregatedValue(true).setHopCount(networkSize);
                } else {
                    builder.setHopCount(0).setHasAggregatedValue(false);
                }
                com.tokenHandler.TokenHandler.Token newToken = builder.build();
                trySend(newToken);
            }
            else if(token.getStarter().equals(node.getId())){
                Measurement measurement=new Measurement(node.getId(), token.getAggregatedValue(), System.currentTimeMillis());
                WebResource webResource=client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/add/measurement"));
                ClientResponse r=webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, measurement);
                System.out.println(r.getStatus()+": sono starter, invio misuraz");
                com.tokenHandler.TokenHandler.Token newToken=com.tokenHandler.TokenHandler.Token.newBuilder().setHopCount(0).build();
                trySend(newToken);
            }
            else if (hopCount == 1) {
                com.tokenHandler.TokenHandler.Token newToken;
                if (buffer.getMostRecentTimestamp() > lastMeasurementTimestamp) {
                    lastMeasurementTimestamp=buffer.getMostRecentTimestamp();
                    float value=(token.getAggregatedValue()+buffer.getAggregatedValue())/2;
                    Measurement measurement=new Measurement(node.getId(), value, System.currentTimeMillis());
                    WebResource webResource=client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/add/measurement"));
                    ClientResponse r=webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, measurement);
                    System.out.println(r.getStatus()+": hop count 1, invio misuraz");
                    newToken=com.tokenHandler.TokenHandler.Token.newBuilder().setHopCount(0).build();
                } else {
                    newToken=token;
                }
                trySend(newToken);
            } else {
                com.tokenHandler.TokenHandler.Token newToken;
                if(buffer.getMostRecentTimestamp()>lastMeasurementTimestamp){
                    lastMeasurementTimestamp=buffer.getMostRecentTimestamp();
                    float value=(token.getAggregatedValue()+buffer.getAggregatedValue())/2;
                    newToken=com.tokenHandler.TokenHandler.Token.newBuilder().setHopCount(hopCount-1)
                            .setStarter(token.getStarter()).setAggregatedValue(value).build();
                }
                else {
                    newToken=token;
                }
                trySend(newToken);
            }
        }
        else{
            com.tokenHandler.TokenHandler.TokenResponse response=com.tokenHandler.TokenHandler.TokenResponse.newBuilder().setOk(false)
                    .setNextHop(nodeBeanToMessage(destination)).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
        threadHandler.releaseToken();
    }

    private void trySend(com.tokenHandler.TokenHandler.Token token) {
        com.tokenHandler.TokenHandler.TokenResponse response;
        Node dest=getDestination();
        ManagedChannel channel = ManagedChannelBuilder.forTarget(dest.getAddress() + ":" + dest.getPort()).usePlaintext(true).build();
        SendTokenGrpc.SendTokenBlockingStub stub = SendTokenGrpc.newBlockingStub(channel);
        response = stub.send(token);
        while (!response.getOk()) {
            System.out.println("response no ok from "+dest.getId());
            com.tokenHandler.TokenHandler.Node nextHop = response.getNextHop();
            if (nextHop.getId().equals(node.getId())) {
                break;
            } else {
                setDestination(new Node(nextHop.getId(), nextHop.getAddress(), nextHop.getPort()));
                dest=getDestination();
                channel = ManagedChannelBuilder.forTarget(dest.getAddress() + ":" + dest.getPort()).usePlaintext(true).build();
                stub = SendTokenGrpc.newBlockingStub(channel);
                response = stub.send(token);
            }
        }
    }
}
