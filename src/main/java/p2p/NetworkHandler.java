package p2p;

import beans.Node;
import beans.NodeNetwork;
import com.netHandler.RingNetworkHandler;
import com.netHandler.RingNetworkHandler.UpdateNeighboursMessage;
import com.netHandler.UpdateNeighboursGrpc;
import com.netHandler.UpdateNeighboursGrpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NetworkHandler extends UpdateNeighboursImplBase implements Runnable{

    Node node;
    Node next;
    Node previous;
    List<Node> nodes;
    boolean entering;
    boolean exiting;
    ManagedChannel toNext;
    ManagedChannel toPrevious;
    ThreadHandler threadHandler;

    public NetworkHandler(){
        entering=true;
        exiting=false;
    }

    public void setNode(Node node){
        this.node=node;
    }

    public void setNodes(List<Node> nodes){
        this.nodes = nodes;
    }

    public void setThreadHandler(ThreadHandler threadHandler){
        this.threadHandler=threadHandler;
    }

    public Node getNext(){
        return next;
    }

    public Node getPrevious(){
        return previous;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    private void addNode(Node node){
        boolean contained=false;
        int i=0;
        for (Node n:nodes) {
            int check = n.getId().compareTo(node.getId());
            if (check==0){
                contained=true;
                break;
            }
            if(check>0){
                break;
            }
            i=i+1;
        }
        if(!contained) {
            nodes.add(i, new Node(node.getId(), node.getAddress(), node.getPort()));
        }

    }

    private boolean evaluateLeftNeighbouring(String fromId){
        if(previous.getId().equals(fromId)){    /// può capitare che dei nodi abbiano già la lista aggiornata dal gw e
                                                // quindi sappiano già i propri vicini
            return true;
        }
        if(nodes.get(0).getId().equals(node.getId())){
            if(fromId.compareTo(previous.getId())>0 || fromId.compareTo(node.getId())<0){
                return true;
            }
            else {
                return false;
            }
        }
        else{
            if(fromId.compareTo(previous.getId())>0 && fromId.compareTo(node.getId())<0){
                return true;
            }
            else{
                return false;
            }
        }
    }


    private boolean evaluateRightNeighbouring(String fromId){
        if(next.getId().equals(fromId)){        /// può capitare che dei nodi abbiano già la lista aggiornata dal gw e
                                                // quindi sappiano già i propri vicini
            return true;
        }
        if(nodes.get(nodes.size()-1).getId().equals(node.getId())){
            if(fromId.compareTo(next.getId())<0||fromId.compareTo(node.getId())>0){
                return true;
            }
            else {
                return false;
            }
        }
        else{
            if(fromId.compareTo(next.getId())<0 && fromId.compareTo(node.getId())>0){
                return true;
            }
            else{
                return false;
            }
        }
    }

    public Node findPrev(Node node, List<Node> nodes){
        for (int i = nodes.size()-1; i >= 0  ; i--) {
            String id=node.getId();
            Node n = nodes.get(i);
            if (id.equals(n.getId())){
                int index=Math.floorMod((i-1), nodes.size());
                return nodes.get(index);
            }
        }
        return null;
    }

    public Node findNext(Node node, List<Node> nodes){
        for (int i = nodes.size()-1; i >= 0  ; i--) {
            String id=node.getId();
            Node n = nodes.get(i);
            if (id.equals(n.getId())){
                int index=(i+1)%nodes.size();
                return nodes.get(index);
            }
        }
        return null;
    }


    @Override
    public void run() {
        for (Node n: nodes) {
            System.out.println(n.getId());
        }
        next=findNext(node, nodes);
        previous=findPrev(node, nodes);
        System.out.println("io: "+node.getId()+ " ho come prev: "+previous.getId()+" e next: "+next.getId());
        try {
            Random s=new Random();
            Thread.sleep(1000*s.nextInt(4));
        } catch (InterruptedException e) {
            System.out.println("davvero?");
            e.printStackTrace();
        }
        if(nodes.size()>1) {

            UpdateNeighboursMessage message=UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                    .setFrom(RingNetworkHandler.Node.newBuilder().setAddress(node.getAddress()).setId(node.getId()).setPort(node.getPort()).build())
                    .setNext(RingNetworkHandler.Node.newBuilder().setAddress(next.getAddress()).setId(next.getId()).setPort(next.getPort()).build())
                    .setPrevious(RingNetworkHandler.Node.newBuilder().setAddress(previous.getAddress()).setId(previous.getId()).setPort(previous.getPort()).build())
                    .build();

            if(nodes.size()==2){
                toNext=toPrevious=ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                entering=false;
                UpdateNeighboursBlockingStub stub=UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse response=stub.update(message);
                System.out.println(response);
            }
            else{
                toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                toPrevious = ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubPrevious=UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                RingNetworkHandler.UpdateNeighboursResponse fromPrevious=stubPrevious.update(message);
                while (!fromPrevious.getOk()){
                    System.out.println(node.getId()+" received prev no ok");
                    RingNetworkHandler.Node suggestedPrevious =fromPrevious.getPrevious();
                    /*toPrevious.shutdownNow();
                    try {
                        toPrevious.awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    previous=new Node(suggestedPrevious.getId(), suggestedPrevious.getAddress(), suggestedPrevious.getPort());
                    addNode(previous);
                    toPrevious=ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                    stubPrevious=UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                    message= UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting).setFrom(message.getFrom())
                            .setPrevious(RingNetworkHandler.Node.newBuilder().setId(previous.getId()).setAddress(previous.getAddress()).setPort(previous.getPort()).build())
                            .setNext(message.getNext()).build();
                    fromPrevious=stubPrevious.update(message);
                }
                UpdateNeighboursBlockingStub stubNext=UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse fromNext=stubNext.update(message);
                while (!fromNext.getOk()){
                    System.out.println(node.getId()+" received next no ok");
                    RingNetworkHandler.Node suggestedNext= fromNext.getNext();
                    /*toNext.shutdownNow();
                    try {
                        toNext.awaitTermination(10,TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                    next=new Node(suggestedNext.getId(), suggestedNext.getAddress(), suggestedNext.getPort());
                    addNode(next);
                    toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                    stubNext=UpdateNeighboursGrpc.newBlockingStub(toNext);
                    message= UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting).setFrom(message.getFrom())
                            .setNext(RingNetworkHandler.Node.newBuilder().setId(next.getId()).setAddress(next.getAddress()).setPort(next.getPort()).build())
                            .setPrevious(message.getPrevious()).build();
                    fromNext=stubNext.update(message);
                }
                entering = false;
                System.out.println(fromPrevious);
                System.out.println(fromNext);
                if(nodes.size()>3){
                    message= RingNetworkHandler.UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                            .setFrom(RingNetworkHandler.Node.newBuilder().setPort(node.getPort()).setAddress(node.getAddress()).setId(node.getId()).build())
                            .build();
                    stubNext=UpdateNeighboursGrpc.newBlockingStub(toNext);
                    stubNext.update(message);
                }
            }
        }


        threadHandler.waitForUser();

        exiting=true;

        System.out.println("bye");
        System.out.println("io: "+node.getId()+" next: "+next.getId()+" prev: "+previous.getId());
        System.out.println("lista");
        for (Node n: nodes) {
            System.out.println(n.getId());
        }



    }


    public void update(UpdateNeighboursMessage message, StreamObserver<RingNetworkHandler.UpdateNeighboursResponse> responseObserver){
        boolean forward=false;
        System.out.println("update");
        RingNetworkHandler.Node from = message.getFrom();
        String fromId=from.getId(), fromAddress=from.getAddress();
        int fromPort=from.getPort();
        RingNetworkHandler.UpdateNeighboursResponse response;
        if(nodes!=null) {
            /*List<RingNetworkHandler.Node> nodes = message.getNetworkList();   //// per ora niente network
            System.out.println(nodes.indexOf(node));
            int index = (nodes.indexOf(node) + 1) % nodes.size();
            next = message.getNetwork(index);
            ManagedChannel channel = ManagedChannelBuilder.forTarget(next.getId() + ":" + next.getPort()).usePlaintext(true).build();
            UpdateNeighboursBlockingStub stub = UpdateNeighboursGrpc.newBlockingStub(channel);
            UpdateNeighboursMessage.Builder builder = UpdateNeighboursMessage.newBuilder().setEntering(message.getEntering()).setExiting(message.getExiting())
                    .setFrom(message.getFrom())
                    .setNext(message.getNext())
                    .setPrevious(message.getPrevious()).setTo(next);
            /*for (RingNetworkHandler.Node n : nodes) {
                builder.addNetwork(n);
            }
            stub.update(builder.build());
            channel.shutdown();*/



            if (message.getEntering()) {
                if (message.getNext().getId().equals(node.getId())) {
                    if(evaluateLeftNeighbouring(fromId)){
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                    }
                    else{
                        RingNetworkHandler.Node suggestedNext= RingNetworkHandler.Node.newBuilder().setId(previous.getId())
                                .setPort(previous.getPort()).setAddress(previous.getAddress()).build();
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                    }
                }
                else if (message.getPrevious().getId().equals(node.getId())) {
                    if(evaluateRightNeighbouring(fromId)){
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                    }
                    else{
                        RingNetworkHandler.Node suggestedPrevious= RingNetworkHandler.Node.newBuilder().setId(next.getId())
                                .setPort(next.getPort()).setAddress(next.getAddress()).build();
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).build();
                    }
                } else {
                    response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).build();
                    //non ricevo messaggi di entering se non sono diretto interessato perchè sarà poi il messaggio di update
                    // con entering ed exiting a false che mi farà aggiornare la lista di nodi
                }
            } else if (message.getExiting()) {
                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();    ///da fare
            } else {
                //messaggi con entering e exiting a false, aggiungo solo il nodo from e inoltro.
                addNode(new Node(fromId, message.getFrom().getAddress(), message.getFrom().getPort()));
                response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                if(!(next.getId().equals(fromId)|| next.getId().equals(message.getPrevious().getId()) || next.getId().equals(message.getNext().getId()))){
                    forward=true;
                }
            }



        }
        else {
            RingNetworkHandler.Node me = RingNetworkHandler.Node.newBuilder().setId(node.getId()).setPort(node.getPort()).setAddress(node.getAddress()).build();
            if (message.getPrevious().getId().equals(node.getId())) {
                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(me).build();
            }
            else if(message.getNext().getId().equals(node.getId())){
                response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(me).build();
            }
            else{
                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).build();   //non dovrei ricevere questi messaggi
            }
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if(forward){
            UpdateNeighboursBlockingStub stubNext=UpdateNeighboursGrpc.newBlockingStub(toNext);
            stubNext.update(message);
        }


    }


}
