package p2p;


import beans.Node;
import beans.NodeNetwork;
import com.netHandler.RingNetworkHandler;
import pm10.PM10Simulator;
import pm10.Simulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Random;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.Client;

import javax.ws.rs.core.MediaType;

import io.grpc.Server;
import io.grpc.ServerBuilder;


public class Nodo {


    public static String gateway="localhost";
    public static int gatewayPort=8080;
    private static final String resource= "/SDP_papaluca_2020_war_exploded";
    private static final int idLength = 5;
    private static final int maxPort = 49151;
    private static final int minPort = 1024;


    private static final String ALPHA_NUMERIC_STRING = "abcdefghijklmnopqrstuvwxyz0123456789";
    public static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public Nodo(){ }


    public static void main(String [] args) {
        BufferImpl buffer = new BufferImpl();
        Simulator sensorSimulator = new PM10Simulator(buffer);
        sensorSimulator.start();
        System.out.println("Sensing started...\n");
        try {
            Client client = Client.create();
            WebResource webResource;
            ClientResponse response;
            NetworkHandler networkHandler = new NetworkHandler();
            TokenHandler tokenHandler = new TokenHandler(buffer);
            networkHandler.setTokenHandler(tokenHandler);
            Server server;
            int nodePort;
            Random r=new Random();
            Node node;
            do {
                nodePort=r.nextInt(maxPort-minPort) + minPort;
                try {
                    server = ServerBuilder.forPort(nodePort).addService(networkHandler).addService(tokenHandler).build();
                    ClientResponse addNodeResponse;
                    webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/add/node"));
                    do {
                        String nodeId=randomAlphaNumeric(idLength);
                        node = new Node(nodeId,  "localhost", nodePort);
                        addNodeResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, node);
                    } while (addNodeResponse.getStatus() != 200); //400 bad_request se c'è già id.

                    networkHandler.setNode(node);
                    tokenHandler.setNode(node);
                    /*webResource=client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/nodes"));
                    response=webResource.accept("application/json").get(ClientResponse.class);*/
                    NodeNetwork nodeNetwork=addNodeResponse.getEntity(NodeNetwork.class);
                    System.out.println("ho "+nodeNetwork.countNodes()+ "nodi");
                    //prima di far partire il server il nodo deve sapere attraverso la conoscenza locale il suo next e prev attuale
                    networkHandler.setNodes(nodeNetwork.getNodes());
                    int count=nodeNetwork.countNodes();
                    if(count==2){
                        tokenHandler.createToken=true;
                    }
                    tokenHandler.setNetworkSize(count);
                    networkHandler.next=networkHandler.findNext(node);
                    tokenHandler.setDestination(networkHandler.getNext());
                    networkHandler.previous=networkHandler.findPrev(node);
                    server.start();
                    break;
                }catch (java.net.BindException exception){
                    continue;
                }catch (java.io.IOException ioe){
                    continue;
                }
            }while (true);

            ThreadHandler threadHandler =new ThreadHandler();
            networkHandler.setThreadHandler(threadHandler);
            tokenHandler.setThreadHandler(threadHandler);
            Thread networkThread=new Thread(networkHandler);
            Thread tokenThread=new Thread(tokenHandler);
            networkThread.start();
            tokenThread.start();

            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String check;
            do {
                check=br.readLine();
            }while(!check.toLowerCase().equals("exit"));

            webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/remove/node"));
            webResource.accept(MediaType.APPLICATION_JSON).post(String.class, node.getId());

            threadHandler.notifyExit();



        } catch (IOException e) { ///per la readLine
            e.printStackTrace();
        } finally {
            sensorSimulator.stopMeGently();
        }

    }
}

