package p2p;


import beans.Node;
import beans.NodeNetwork;
import pm10.Buffer;
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
    private static final int idLength = 20;
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


    /*public static String buildInsertionRequestBody() throws JsonProcessingException {
        ObjectMapper objectMapper=new ObjectMapper();
        String nodeId=randomAlphaNumeric(idLength);
        int nodePort=3456;
        LinkedHashMap<String, String>nodeValues=new LinkedHashMap<String, String>(){{
            put("id", nodeId);
            put ("address", "localhost");
            put("port", String.valueOf(nodePort));
        }};
        return objectMapper.writeValueAsString(nodeValues);
    }*/



    public static void main(String[] args) {
        BufferImpl buffer = new BufferImpl();
        Simulator sensorSimulator = new PM10Simulator(buffer);
        sensorSimulator.start();
        System.out.println("Sensing started...\n");
        try {
            Client client = Client.create();
            WebResource webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/api/hello"));
            ClientResponse response = webResource.accept("text/plain").get(ClientResponse.class);
            System.out.println(response.getEntity(String.class));
            Server server;
            int nodePort;
            Random r=new Random();
            do {
                nodePort=r.nextInt(maxPort-minPort) + minPort;
                try {
                    server = ServerBuilder.forPort(nodePort).addService(new UpdateNeighboursImpl()).build();
                    server.start();
                    break;
                }catch (java.io.IOException exception){
                    continue;
                }
            }while (true);
            ClientResponse addNodeResponse;
            webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/add/node"));
            //String requestBody;
            Node node;
            do {
                //requestBody = buildInsertionRequestBody();
                //System.out.println(requestBody);
                String nodeId=randomAlphaNumeric(idLength);
                node = new Node(nodeId,  "localhost", nodePort);
                addNodeResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, node);
                if (addNodeResponse.getStatus()==404){
                    System.out.println("404, Not Found");
                    return;
                }
            } while (addNodeResponse.getStatus() != 200); //400 bad_request se c'è già id.

            System.out.println("fatto");
            webResource=client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/nodes"));
            response=webResource.accept("application/json").get(ClientResponse.class);
            NodeNetwork nodeNetwork=response.getEntity(NodeNetwork.class);

            ThreadHandler threadHandler =new ThreadHandler();
            NetworkHandler networkHandler = new NetworkHandler(node, nodeNetwork, threadHandler);
            Thread networkThread=new Thread(networkHandler);
            networkThread.start();

            BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
            String check;
            do {
                check=br.readLine();
            }while(!check.toLowerCase().equals("exit"));

            threadHandler.notifyExit();

            /*for (Node n: nodeNetwork.getNodes()) { //////Parte per rimozione
                System.out.println(n.getId());
            }

            webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/api/nodenetwork/remove/node"));
            ClientResponse prova= webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, node);
            nodeNetwork=prova.getEntity(NodeNetwork.class);
            System.out.println(nodeNetwork.getNodes().size());*/

        } catch (IOException e) { ///per la readLine
            e.printStackTrace();
        } finally {
            sensorSimulator.stopMeGently();
        }

    }
}

