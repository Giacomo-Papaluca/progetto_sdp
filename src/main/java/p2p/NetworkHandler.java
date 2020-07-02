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
    StreamObserver<UpdateNeighboursMessage> streamNext;
    StreamObserver<UpdateNeighboursMessage> streamPrevious;

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


            toPrevious=ManagedChannelBuilder.forTarget(next.getAddress()+":"+next.getPort()).usePlaintext(true).build();
            UpdateNeighboursStub stubPrevious=UpdateNeighboursGrpc.newStub(toPrevious);
            streamPrevious=stubPrevious.update(new StreamObserver<RingNetworkHandler.UpdateNeighboursResponse>() {
                @Override
                public void onNext(RingNetworkHandler.UpdateNeighboursResponse response) {
                    if(!response.getOk()){
                        if(response.hasNext()){

                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    nodes.remove(previous);
                    findPrev(node, nodes);
                }

                @Override
                public void onCompleted() {
                    toPrevious.shutdown();
                }
            });


            entering = false;



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


    public StreamObserver<RingNetworkHandler.UpdateNeighboursMessage> update(StreamObserver<RingNetworkHandler.UpdateNeighboursResponse> responseObserver) {
        return new StreamObserver<UpdateNeighboursMessage>() {
            @Override
            public void onNext(UpdateNeighboursMessage message) {
                RingNetworkHandler.Node from=message.getFrom();
                String fromId=from.getId(), fromAddress=from.getAddress();
                RingNetworkHandler.UpdateNeighboursResponse response;
                if(!message.getExiting()){
                    if(node.getId().equals(message.getNext().getId())){
                        if(evaluateLeftNeighbouring(fromId)){
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        }
                        else{
                            RingNetworkHandler.Node suggestedNext= RingNetworkHandler.Node.newBuilder()
                                    .setPort(previous.getPort()).setAddress(previous.getAddress()).setId(previous.getId())
                                    .build();
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                        }
                    }
                    else if(node.getId().equals(message.getPrevious().getId())){
                        if(evaluateRightNeighbouring(fromId)){
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        }
                        else {
                            RingNetworkHandler.Node suggestedPrevious= RingNetworkHandler.Node.newBuilder()
                                    .setId(next.getId()).setAddress(next.getAddress()).setPort(next.getPort())
                                    .build();
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).build();
                        }

                    }
                    else{
                        response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).build();
                    }
                    responseObserver.onNext(response);
                }
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onCompleted() {

            }
        };
    }


}
