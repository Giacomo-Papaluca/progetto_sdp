package p2p;


import beans.Node;
import pm10.PM10Simulator;
import pm10.Simulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        Client client = Client.create();
        WebResource webResource;
        Node node=new Node();
        try {
            NetworkHandler networkHandler = new NetworkHandler();
            TokenHandler tokenHandler = new TokenHandler(buffer);
            networkHandler.setTokenHandler(tokenHandler);
            tokenHandler.setNetworkHandler(networkHandler);
            Server server;
            int nodePort;
            Random r=new Random();
            boolean createToken=false;
            do {
                nodePort=r.nextInt(maxPort-minPort) + minPort;
                try {
                    server = ServerBuilder.forPort(nodePort).addService(networkHandler).addService(tokenHandler).build();
                    ClientResponse addNodeResponse;
                    webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/add/node"));
                    List<LinkedHashMap> returnedNetwork;
                    List<Node> nodeNetwork=new ArrayList<>();
                    int count;
                    do {
                        String nodeId=randomAlphaNumeric(idLength);
                        node = new Node(nodeId,  "localhost", nodePort);
                        addNodeResponse = webResource.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, node);
                        returnedNetwork=addNodeResponse.getEntity(List.class);
                        count=returnedNetwork.size();
                        for (int i = 0; i < count; i++) {
                            LinkedHashMap lhm=returnedNetwork.get(i);
                            Node n= new Node(lhm.get("id").toString(), lhm.get("address").toString(), Integer.parseInt(lhm.get("port").toString()));
                            nodeNetwork.add(i, n);
                        }
                    } while (returnedNetwork == null);


                    System.out.println("ho "+count+ "nodi");
                    networkHandler.setNode(node);
                    tokenHandler.setNode(node);
                    //prima di far partire il server il nodo deve sapere attraverso la conoscenza locale il suo next e prev attuale
                    networkHandler.setNodes(nodeNetwork);
                    if(count==2){
                        createToken=true;
                        networkHandler.justTwo=true;
                    }
                    tokenHandler.createToken=createToken;   //anche se molto poco probabile il secondo nodo generato potrebbe
                                                            //avere la stessa porta del primo e quindi ripetere l'ingresso attraverso il
                                                            //gateway. Per garantire la creazione del token voglio che il nodo crei il token
                                                            //se almeno una volta .getNodes() ha ricevuto la lista con 2 nodi
                    tokenHandler.setNetworkSize(count);
                    networkHandler.setNext(networkHandler.findNext(node));
                    tokenHandler.setDestination(networkHandler.getNext());
                    networkHandler.setPrevious(networkHandler.findPrev(node));
                    server.start();
                    break;
                }catch (java.net.BindException exception){
                    webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/remove/node/"+node.getId()));
                    webResource.accept(MediaType.APPLICATION_JSON).delete();
                    continue;
                }catch (java.io.IOException ioe){
                    webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/remove/node/"+node.getId()));
                    webResource.accept(MediaType.APPLICATION_JSON).delete();
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


            threadHandler.notifyExit();



        } catch (IOException e) { ///per la readLine
            e.printStackTrace();
        } finally {
            sensorSimulator.stopMeGently();
            System.out.println("esco");
            webResource = client.resource(URI.create("http://" + gateway + ":" + gatewayPort + resource + "/nodenetwork/remove/node/"+node.getId()));
            webResource.accept(MediaType.APPLICATION_JSON).delete();

        }

    }
}

