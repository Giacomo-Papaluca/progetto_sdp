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

    private RingNetworkHandler.Node nodeBeanToMessage(Node node){
        return RingNetworkHandler.Node.newBuilder().setId(node.getId()).setPort(node.getPort()).setAddress(node.getAddress()).build();
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

    private void removeNode(String id){
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
        if(nodes.size()>1) {

            UpdateNeighboursMessage message=UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                    .setFrom(nodeBeanToMessage(node))
                    .setNext(nodeBeanToMessage(next))
                    .setPrevious(nodeBeanToMessage(previous))
                    .build();

            if(nodes.size()==2){
                toNext=toPrevious=ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                entering=false;
                UpdateNeighboursBlockingStub stub=UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse response=stub.update(message);
                System.out.println(response);
            }
            else{
                toPrevious = ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubPrevious=UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                RingNetworkHandler.UpdateNeighboursResponse fromPrevious=stubPrevious.update(message);
                while (!fromPrevious.getOk()){
                    if(fromPrevious.hasNext()&&fromPrevious.hasPrevious()){
                        next=new Node(fromPrevious.getNext().getId(), fromPrevious.getNext().getAddress(), fromPrevious.getNext().getPort());
                        previous=new Node(fromPrevious.getPrevious().getId(), fromPrevious.getPrevious().getAddress(), fromPrevious.getPrevious().getPort());
                        break;
                    }
                    System.out.println(node.getId()+" received prev no ok");
                    RingNetworkHandler.Node suggestedPrevious =fromPrevious.getPrevious();
                    previous=new Node(suggestedPrevious.getId(), suggestedPrevious.getAddress(), suggestedPrevious.getPort());
                    addNode(previous);
                    toPrevious=ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                    stubPrevious=UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                    message= UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting).setFrom(message.getFrom())
                            .setPrevious(suggestedPrevious)
                            .setNext(message.getNext()).build();
                    fromPrevious=stubPrevious.update(message);
                }

                message=UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                        .setFrom(nodeBeanToMessage(node))
                        .setNext(nodeBeanToMessage(next))
                        .setPrevious(nodeBeanToMessage(previous))
                        .build();

                toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubNext=UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse fromNext=stubNext.update(message);
                while (!fromNext.getOk()){
                    if(fromNext.hasNext()&&fromNext.hasPrevious()){
                        next=new Node(fromNext.getNext().getId(), fromNext.getNext().getAddress(), fromNext.getNext().getPort());
                        previous=new Node(fromNext.getPrevious().getId(), fromNext.getPrevious().getAddress(), fromNext.getPrevious().getPort());
                        break;
                    }
                    System.out.println(node.getId()+" received next no ok");
                    RingNetworkHandler.Node suggestedNext= fromNext.getNext();
                    next=new Node(suggestedNext.getId(), suggestedNext.getAddress(), suggestedNext.getPort());
                    addNode(next);
                    toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                    stubNext=UpdateNeighboursGrpc.newBlockingStub(toNext);
                    message= UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting).setFrom(message.getFrom())
                            .setNext(suggestedNext)
                            .setPrevious(message.getPrevious()).build();
                    fromNext=stubNext.update(message);
                }

                System.out.println(fromPrevious);
                System.out.println(fromNext);

                for (Node n: nodes) {
                    if(!n.getId().equals(next.getId())&&!n.getId().equals(previous.getId())&&!n.getId().equals(node.getId())){
                        UpdateNeighboursBlockingStub stub= UpdateNeighboursGrpc.newBlockingStub(
                                ManagedChannelBuilder.forTarget(n.getAddress()+":"+n.getPort()).usePlaintext(true).build()
                        );
                        stub.update(message);
                    }
                }



            }
        }

        entering = false;

        threadHandler.waitForUser();

        exiting=true;

        System.out.println("bye");
        System.out.println("io: "+node.getId()+" next: "+next.getId()+" prev: "+previous.getId());
        System.out.println("lista");
        for (Node n: nodes) {
            System.out.println(n.getId());
        }

        if(nodes.size()>1) {
            UpdateNeighboursMessage message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                    .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();

            if(nodes.size()==2){
                toNext=toPrevious=ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stub=UpdateNeighboursGrpc.newBlockingStub(toNext);
                stub.update(message);
            }

            else {
                toPrevious = ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                RingNetworkHandler.UpdateNeighboursResponse fromPrevious = stubPrevious.update(message);
                while (!fromPrevious.getOk()) {
                    RingNetworkHandler.Node suggestedPrevious = fromPrevious.getPrevious();
                    previous = new Node(suggestedPrevious.getId(), suggestedPrevious.getAddress(), suggestedPrevious.getPort());
                    message = UpdateNeighboursMessage.newBuilder().setEntering(message.getEntering()).setExiting(message.getExiting())
                            .setFrom(message.getFrom()).setPrevious(suggestedPrevious).setNext(message.getNext()).build();
                    toPrevious = ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                    stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                    fromPrevious = stubPrevious.update(message);
                }

                message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                        .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();

                toNext= ManagedChannelBuilder.forTarget(next.getAddress()+":"+next.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse fromNext = stubNext.update(message);
                while (!fromNext.getOk()) {
                    RingNetworkHandler.Node suggestedNext = fromNext.getNext();
                    next = new Node(suggestedNext.getId(), suggestedNext.getAddress(), suggestedNext.getPort());
                    message = UpdateNeighboursMessage.newBuilder().setEntering(message.getEntering()).setExiting(message.getExiting()).setFrom(message.getFrom())
                            .setNext(suggestedNext)
                            .setPrevious(message.getPrevious()).build();
                    toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                    stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                    fromNext = stubNext.update(message);
                }

                message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                        .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();

                for (Node n : nodes) {
                    if (!n.getId().equals(next.getId()) && !n.getId().equals(previous.getId()) && !n.getId().equals(node.getId())) {
                        UpdateNeighboursBlockingStub stub = UpdateNeighboursGrpc.newBlockingStub(
                                ManagedChannelBuilder.forTarget(n.getAddress() + ":" + n.getPort()).usePlaintext(true).build()
                        );
                        stub.update(message);
                    }
                }
            }
        }

    }


    public void update(UpdateNeighboursMessage message, StreamObserver<RingNetworkHandler.UpdateNeighboursResponse> responseObserver){
        System.out.println("update");
        RingNetworkHandler.Node from = message.getFrom();
        String fromId=from.getId();
        RingNetworkHandler.UpdateNeighboursResponse response;
        Node fromNode = new Node(fromId, from.getAddress(), from.getPort());

        if (message.getEntering()) {
            if(!exiting){
                if (message.getNext().getId().equals(node.getId())&&!message.getPrevious().getId().equals(node.getId())) {
                    if (evaluateLeftNeighbouring(fromId)) {
                        addNode(fromNode);
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        previous=fromNode;
                        System.out.println("new prev: "+previous.getId());
                    } else {
                        RingNetworkHandler.Node suggestedNext = RingNetworkHandler.Node.newBuilder().setId(previous.getId())
                                .setPort(previous.getPort()).setAddress(previous.getAddress()).build();
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                    }
                } else if (message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())) {
                    if (evaluateRightNeighbouring(fromId)) {
                        addNode(fromNode);
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                        next=fromNode;
                        System.out.println("new next: "+next.getId());
                    } else {
                        RingNetworkHandler.Node suggestedPrevious = RingNetworkHandler.Node.newBuilder().setId(next.getId())
                                .setPort(next.getPort()).setAddress(next.getAddress()).build();
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).build();
                    }
                } else if(message.getNext().getId().equals(node.getId())&&message.getPrevious().getId().equals(node.getId())){
                    addNode(fromNode);
                    next=previous=fromNode;
                    System.out.println("new next e prev: "+next.getId());
                    response=RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                } else {
                    addNode(fromNode);
                    System.out.println("aggiunto nodo: "+fromNode.getId());
                    response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                }
            }
            else{
                System.out.println("sto uscendo e ho ricevuto");
                if (message.getNext().getId().equals(node.getId())&&!message.getPrevious().getId().equals(node.getId())){
                    RingNetworkHandler.Node suggestedNext = RingNetworkHandler.Node.newBuilder().setId(next.getId())
                            .setPort(previous.getPort()).setAddress(previous.getAddress()).build();
                    response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                }
                else if(message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())){
                    RingNetworkHandler.Node suggestedPrevious = RingNetworkHandler.Node.newBuilder().setId(previous.getId())
                            .setPort(next.getPort()).setAddress(next.getAddress()).build();
                    response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).build();
                }else if(message.getPrevious().getId().equals(node.getId())&&message.getNext().getId().equals(node.getId())){
                    response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(from).setNext(from).build();
                }
                else {
                    //se ricevo messaggi di ingresso e non sono ne prev ne next mentre sto uscendo non mi interessa
                    response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                }
            }
        } else if (message.getExiting()) {
            if(!exiting){
                removeNode(fromId);
                if (message.getNext().getId().equals(node.getId())&&!message.getPrevious().getId().equals(node.getId())) {
                    previous=new Node(message.getPrevious().getId(), message.getPrevious().getAddress(), message.getPrevious().getPort());
                    System.out.println("prev removed, new prev: "+previous.getId());
                } else if (message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())) {
                    next=new Node(message.getNext().getId(), message.getNext().getAddress(), message.getNext().getPort());
                    System.out.println("next removed, new next: "+next.getId());
                } else if(message.getNext().getId().equals(node.getId())&&message.getPrevious().getId().equals(node.getId())){
                    next=previous=node;
                    System.out.println("removed nex&prev, new next&prev: "+node.getId());
                    response=RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                } else {
                    removeNode(fromId);
                    System.out.println("rimosso nodo: "+fromNode.getId());
                }
                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
            }
            else{
                if (message.getNext().getId().equals(node.getId())&&!message.getPrevious().getId().equals(node.getId())) {
                    RingNetworkHandler.Node suggestedNext= RingNetworkHandler.Node.newBuilder().setId(next.getId())
                            .setPort(next.getPort()).setAddress(next.getAddress()).build();
                    response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                } else if (message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())) {
                    RingNetworkHandler.Node suggestedPrevious = RingNetworkHandler.Node.newBuilder().setAddress(previous.getAddress())
                            .setPort(previous.getPort()).setId(previous.getId()).build();
                    response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).build();
                } else if(message.getNext().getId().equals(node.getId())&&message.getPrevious().getId().equals(node.getId())){
                    response=RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                } else {
                    //se sto uscendo da un'altra parte della rete non mi interessa
                    response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                }
            }
        } else {
            //non dovrebbero circolare messaggi con exiting e entering entrambi a false
            response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }


}
