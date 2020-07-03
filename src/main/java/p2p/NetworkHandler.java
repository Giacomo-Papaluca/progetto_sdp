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

    private StreamObserver generateStream(int type){
        if(type==0){
            toPrevious=ManagedChannelBuilder.forTarget(previous.getAddress()+":"+previous.getPort()).usePlaintext(true).build();
            UpdateNeighboursStub stubPrevious=UpdateNeighboursGrpc.newStub(toPrevious);
            return stubPrevious.update(new StreamObserver<RingNetworkHandler.UpdateNeighboursResponse>() {
                @Override
                public void onNext(RingNetworkHandler.UpdateNeighboursResponse response) {
                    System.out.println(response);
                    if(!response.getOk()){  ///gli ok vengono semplicemente droppati
                        if(response.hasPrevious()){
                            RingNetworkHandler.Node suggestedPrevious=response.getPrevious();
                            previous=new Node(suggestedPrevious.getId(), suggestedPrevious.getAddress(), suggestedPrevious.getPort());
                            streamPrevious=generateStream(0);
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("error");
                    nodes.remove(previous);
                    synchronized (nodes){
                        previous=findPrev(node, nodes);
                    }
                    if(previous!=null) {
                        streamPrevious = generateStream(0);
                    }
                    else{
                        onCompleted();
                    }
                }

                @Override
                public void onCompleted() {
                    toPrevious.shutdown();
                }
            });
        }
        else if(type==1){
            toNext=ManagedChannelBuilder.forTarget(next.getAddress()+":"+next.getPort()).usePlaintext(true).build();
            UpdateNeighboursStub stubNext= UpdateNeighboursGrpc.newStub(toNext);
            return stubNext.update(new StreamObserver<RingNetworkHandler.UpdateNeighboursResponse>() {
                @Override
                public void onNext(RingNetworkHandler.UpdateNeighboursResponse response) {
                    System.out.println(response);
                    if(!response.getOk()){
                        if(response.hasNext()){
                            RingNetworkHandler.Node suggestedNext=response.getNext();
                            next=new Node(suggestedNext.getId(), suggestedNext.getAddress(), suggestedNext.getPort());
                            streamNext=generateStream(1);
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("error");
                    nodes.remove(next);
                    synchronized (nodes) {
                        next = findNext(node, nodes);
                    }
                    if(next!=null){
                        streamNext=generateStream(1);
                    }
                    else{
                        onCompleted();
                    }
                }

                @Override
                public void onCompleted() {
                    toNext.shutdown();
                }
            });
        }
        else {
            return null;
        }
    }

    private synchronized void addNode(Node node){
        boolean contained=false;
        int i=0;
        for (Node n:nodes) {
            System.out.println("compare "+n.getId());
            System.out.println("with "+node.getId());
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

    private synchronized void removeNode(String id){
        for (Node n: nodes) {
            if(n.getId().equals(id)){
                nodes.remove(n);
                return;
            }
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



            streamPrevious=generateStream(0);
            streamNext=generateStream(1);


            if(nodes.size()>2){
                streamPrevious.onNext(message);
            }
            streamNext.onNext(message);

        }

        entering = false;

        threadHandler.waitForUser();

        exiting=true;

        UpdateNeighboursMessage message=UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                .setFrom(RingNetworkHandler.Node.newBuilder().setId(node.getId()).setPort(node.getPort()).setAddress(node.getAddress()).build())
                .setPrevious(RingNetworkHandler.Node.newBuilder().setAddress(previous.getAddress()).setPort(previous.getPort()).setId(previous.getId()).build())
                .setNext(RingNetworkHandler.Node.newBuilder().setId(next.getId()).setPort(next.getPort()).setAddress(next.getAddress()).build())
                .build();

        if(nodes.size()>2){
            streamPrevious.onNext(message);
        }
        streamNext.onNext(message);

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
                System.out.println("received message: \n"+message);
                RingNetworkHandler.Node from=message.getFrom();
                String fromId=from.getId(), fromAddress=from.getAddress();
                RingNetworkHandler.UpdateNeighboursResponse response;
                Node fromNode=new Node(fromId, fromAddress, from.getPort());
                if(message.getEntering()){
                    addNode(fromNode);
                    if(node.getId().equals(message.getNext().getId())&&!node.getId().equals(message.getPrevious().getId())){
                        synchronized (streamPrevious){
                            if (evaluateLeftNeighbouring(fromId)) {
                                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                                previous = fromNode;
                                System.out.println("new prev: " + previous.getId());
                                streamPrevious = generateStream(0);
                                if (nodes.size() > 3) {
                                    if (!(next.getId().equals(fromId) || next.getId().equals(message.getNext().getId()) || next.getId().equals(message.getPrevious().getId()))) {
                                        streamNext.onNext(message);
                                    }
                                }
                            } else {
                                nodes.remove(fromNode);
                                RingNetworkHandler.Node suggestedNext = RingNetworkHandler.Node.newBuilder()
                                        .setPort(previous.getPort()).setAddress(previous.getAddress()).setId(previous.getId())
                                        .build();
                                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                            }
                        }
                    }
                    else if(node.getId().equals(message.getPrevious().getId())&&!node.getId().equals(message.getNext().getId())){
                        synchronized (streamNext){
                            if (evaluateRightNeighbouring(fromId)) {
                                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                                next = fromNode;
                                System.out.println("new next " + next.getId());
                                streamNext = generateStream(1);
                            } else {
                                nodes.remove(fromNode);
                                RingNetworkHandler.Node suggestedPrevious = RingNetworkHandler.Node.newBuilder()
                                        .setId(next.getId()).setAddress(next.getAddress()).setPort(next.getPort())
                                        .build();
                                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).build();
                            }
                        }
                    }
                    else if(node.getId().equals(message.getNext().getId())&&node.getId().equals(message.getPrevious().getId())){
                        response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        next=previous=fromNode;
                        System.out.println("new prev and next: "+fromNode.getId());
                        streamPrevious=generateStream(0);
                        streamNext=generateStream(1);
                    }
                    else{
                        response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        if(nodes.size()>3){
                            if(!(next.getId().equals(fromId) || next.getId().equals(message.getNext().getId()) || next.getId().equals(message.getPrevious().getId()))){
                                streamNext.onNext(message);
                            }
                        }
                    }
                    responseObserver.onNext(response);
                }
                else if (message.getExiting()){
                    if (exiting) {
                         //in generale se mi arriva un messaggio di uscita da un nodo più grande mentre sto uscendo gli dico
                        // di scrivere al mio prev se ricevo da un nodo più piccolo gli dico di scrivere al mio next
                        if(fromId.compareTo(node.getId())>0){
                            RingNetworkHandler.Node suggestedPrevious= RingNetworkHandler.Node.newBuilder().setAddress(previous.getAddress())
                                    .setPort(previous.getPort()).setId(previous.getId()).build();
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedPrevious).build();
                            responseObserver.onNext(response);
                        }
                        else {
                            RingNetworkHandler.Node suggestedNext= RingNetworkHandler.Node.newBuilder().setAddress(next.getAddress())
                                    .setPort(next.getPort()).setId(next.getId()).build();
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                            responseObserver.onNext(response);
                        }
                    }
                    else {
                        if(message.getPrevious().getId().equals(node.getId())){
                            removeNode(fromId);
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                            responseObserver.onNext(response);
                            next=new Node(message.getNext().getId(), message.getNext().getAddress(), message.getNext().getPort());
                            streamNext=generateStream(1);
                        }
                        else if(next.getId().equals(fromId)){
                            removeNode(next.getId());
                            response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        }
                        else {
                            response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("server error");
                responseObserver.onError(throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }


}
