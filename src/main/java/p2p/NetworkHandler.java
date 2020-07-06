package p2p;

import beans.Node;
import beans.NodeNetwork;
import com.netHandler.RingNetworkHandler;
import com.netHandler.RingNetworkHandler.UpdateNeighboursMessage;
import com.netHandler.UpdateNeighboursGrpc;
import com.netHandler.UpdateNeighboursGrpc.*;
import io.grpc.ConnectivityState;
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

    public void setNext(Node next){
        this.next=next;
    }

    public void setPrevious(Node previous){
        this.previous=previous;
    }

    public void setThreadHandler(ThreadHandler threadHandler){
        this.threadHandler=threadHandler;
    }

    public Node getNext(){
        synchronized (next){return next;}
    }

    public Node getPrevious(){
        synchronized (previous){return previous;}
    }

    public List<Node> getNodes() {
        synchronized (nodes){return nodes;}
    }

    private RingNetworkHandler.Node nodeBeanToMessage(Node node){
        return RingNetworkHandler.Node.newBuilder().setId(node.getId()).setPort(node.getPort()).setAddress(node.getAddress()).build();
    }

    private void addNode(Node node){
        synchronized (nodes){
            boolean contained = false;
            int i = 0;
            for (Node n : nodes) {
                int check = n.getId().compareTo(node.getId());
                if (check == 0) {
                    contained = true;
                    break;
                }
                if (check > 0) {
                    break;
                }
                i = i + 1;
            }
            if (!contained) {
                nodes.add(i, new Node(node.getId(), node.getAddress(), node.getPort()));
            }
        }

    }

    private void removeNode(String id) {
        synchronized (nodes){
            for (Node n : nodes) {
                if (n.getId().equals(id)) {
                    nodes.remove(n);
                    return;
                }
            }
        }
    }

    private boolean evaluateLeftNeighbouring(String fromId){
        Node tempPrevious=getPrevious();
        List<Node> tempNodes=getNodes();
        if(tempPrevious.getId().equals(fromId)){    /// può capitare che dei nodi abbiano già la lista aggiornata dal gw e
                                                // quindi sappiano già i propri vicini
            return true;
        }
        if(tempNodes.get(0).getId().equals(node.getId())){
            if(fromId.compareTo(tempPrevious.getId())>0 || fromId.compareTo(node.getId())<0){
                return true;
            }
            else {
                return false;
            }
        }
        else{
            if(fromId.compareTo(tempPrevious.getId())>0 && fromId.compareTo(node.getId())<0){
                return true;
            }
            else{
                return false;
            }
        }
    }


    private boolean evaluateRightNeighbouring(String fromId){
        Node tempNext=getNext();
        List<Node> tempNodes=getNodes();
        if(tempNext.getId().equals(fromId)){        /// può capitare che dei nodi abbiano già la lista aggiornata dal gw e
                                                // quindi sappiano già i propri vicini
            return true;
        }
        if(tempNodes.get(tempNodes.size()-1).getId().equals(node.getId())){
            if(fromId.compareTo(tempNext.getId())<0||fromId.compareTo(node.getId())>0){
                return true;
            }
            else {
                return false;
            }
        }
        else{
            if(fromId.compareTo(tempNext.getId())<0 && fromId.compareTo(node.getId())>0){
                return true;
            }
            else{
                return false;
            }
        }
    }

    public Node findPrev(Node node){
        List<Node> tempNodes = getNodes();
        for (int i = tempNodes.size()-1; i >= 0  ; i--) {
            String id=node.getId();
            Node n = tempNodes.get(i);
            if (id.equals(n.getId())){
                int index=Math.floorMod((i-1), tempNodes.size());
                return tempNodes.get(index);
            }
        }
        return null;
    }

    public Node findNext(Node node){
        List<Node> tempNodes= getNodes();
        for (int i = tempNodes.size()-1; i >= 0  ; i--) {
            String id=node.getId();
            Node n = tempNodes.get(i);
            if (id.equals(n.getId())){
                int index=(i+1)%tempNodes.size();
                return tempNodes.get(index);
            }
        }
        return null;
    }


    @Override
    public void run() {
        for (Node n: nodes) {
            System.out.println(n.getId());
        }
        System.out.println("io: "+node.getId()+ " ho come prev: "+previous.getId()+" e next: "+next.getId());
        System.out.println("sleep");
        try {
            Random s=new Random();
            Thread.sleep(1000*s.nextInt(8));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Node> tempNodes=getNodes();
        if(tempNodes.size()>1) {

            UpdateNeighboursMessage message=UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                    .setFrom(nodeBeanToMessage(node))
                    .setNext(nodeBeanToMessage(getNext()))
                    .setPrevious(nodeBeanToMessage(getPrevious()))
                    .build();

            /*if(tempNodes.size()==2){
                toNext=toPrevious=ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                entering=false;
                UpdateNeighboursBlockingStub stub=UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse response=stub.update(message);
                System.out.println(response);
            }
            else{*/

                Node tempPrevious = getPrevious();
                toPrevious = ManagedChannelBuilder.forTarget(tempPrevious.getAddress() + ":" + tempPrevious.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                RingNetworkHandler.UpdateNeighboursResponse fromPrevious = stubPrevious.update(message);
                while (!fromPrevious.getOk()) {
                    if (fromPrevious.hasNext() && fromPrevious.hasPrevious()) {
                        setNext(new Node(fromPrevious.getNext().getId(), fromPrevious.getNext().getAddress(), fromPrevious.getNext().getPort()));
                        setPrevious(new Node(fromPrevious.getPrevious().getId(), fromPrevious.getPrevious().getAddress(), fromPrevious.getPrevious().getPort()));
                        break;
                    }
                    System.out.println(node.getId() + " received prev no ok, suggested: " + fromPrevious.getPrevious().getId());
                    RingNetworkHandler.Node suggestedPrevious = fromPrevious.getPrevious();
                    synchronized (previous) {
                        setPrevious(new Node(suggestedPrevious.getId(), suggestedPrevious.getAddress(), suggestedPrevious.getPort()));
                    }
                    tempPrevious = getPrevious();
                    addNode(tempPrevious);
                    toPrevious = ManagedChannelBuilder.forTarget(tempPrevious.getAddress() + ":" + tempPrevious.getPort()).usePlaintext(true).build();
                    stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                    message = UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting).setFrom(message.getFrom())
                            .setPrevious(suggestedPrevious)
                            .setNext(message.getNext()).build();
                    fromPrevious = stubPrevious.update(message);
                }
                System.out.println(fromPrevious);

                message=UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                        .setFrom(nodeBeanToMessage(node))
                        .setNext(nodeBeanToMessage(getNext()))
                        .setPrevious(nodeBeanToMessage(getPrevious()))
                        .build();

                Node tempNext = getNext();
                toNext = ManagedChannelBuilder.forTarget(tempNext.getAddress() + ":" + tempNext.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse fromNext = stubNext.update(message);
                while (!fromNext.getOk()) {
                    if (fromNext.hasNext() && fromNext.hasPrevious()) {
                        setNext(new Node(fromNext.getNext().getId(), fromNext.getNext().getAddress(), fromNext.getNext().getPort()));
                        setPrevious(new Node(fromNext.getPrevious().getId(), fromNext.getPrevious().getAddress(), fromNext.getPrevious().getPort()));
                        break;
                    }
                    System.out.println(node.getId() + " received next no ok, suggested: " + fromNext.getNext().getId());
                    RingNetworkHandler.Node suggestedNext = fromNext.getNext();
                    synchronized (next) {
                        setNext(new Node(suggestedNext.getId(), suggestedNext.getAddress(), suggestedNext.getPort()));
                    }
                    tempNext = getNext();
                    addNode(tempNext);
                    toNext = ManagedChannelBuilder.forTarget(tempNext.getAddress() + ":" + tempNext.getPort()).usePlaintext(true).build();
                    stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                    message = UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting).setFrom(message.getFrom())
                            .setNext(suggestedNext)
                            .setPrevious(message.getPrevious()).build();
                    fromNext = stubNext.update(message);
                }
                System.out.println(fromNext);

                tempNodes=getNodes();
                for (Node n: tempNodes) {
                    if(!n.getId().equals(getNext().getId())&&!n.getId().equals(getPrevious().getId())&&!n.getId().equals(node.getId())){
                        ManagedChannel channel=ManagedChannelBuilder.forTarget(n.getAddress()+":"+n.getPort()).usePlaintext(true).build();
                        if(channel!=null){
                            UpdateNeighboursBlockingStub stub= UpdateNeighboursGrpc.newBlockingStub(channel);
                            stub.update(message);
                        }
                    }
                }



            //}
        }

        entering = false;

        threadHandler.waitForUser();

        exiting=true;

        try {
            Random s=new Random();
            Thread.sleep(1000*s.nextInt(8));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("bye");
        System.out.println("io: "+node.getId()+" next: "+next.getId()+" prev: "+previous.getId());
        System.out.println("lista");
        tempNodes=getNodes();
        for (Node n: tempNodes) {
            System.out.println(n.getId());
        }

        if(tempNodes.size()>1) {
            UpdateNeighboursMessage message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                    .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();

            /*if(nodes.size()==2){
                toNext=toPrevious=ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stub=UpdateNeighboursGrpc.newBlockingStub(toNext);
                stub.update(message);
            }
            else {*/

                Node tempPrevious = getPrevious();
                toPrevious = ManagedChannelBuilder.forTarget(tempPrevious.getAddress() + ":" + tempPrevious.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                RingNetworkHandler.UpdateNeighboursResponse fromPrevious = stubPrevious.update(message);
                while (!fromPrevious.getOk()) {
                    removeNode(tempPrevious.getId());
                    RingNetworkHandler.Node suggestedPrevious = fromPrevious.getPrevious();
                    synchronized (previous) {
                        setPrevious(new Node(suggestedPrevious.getId(), suggestedPrevious.getAddress(), suggestedPrevious.getPort()));
                    }
                    tempPrevious = getPrevious();
                    message = UpdateNeighboursMessage.newBuilder().setEntering(message.getEntering()).setExiting(message.getExiting())
                            .setFrom(message.getFrom()).setPrevious(suggestedPrevious).setNext(message.getNext()).build();
                    toPrevious = ManagedChannelBuilder.forTarget(tempPrevious.getAddress() + ":" + tempPrevious.getPort()).usePlaintext(true).build();
                    stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                    fromPrevious = stubPrevious.update(message);
                }

                message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                        .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();


                Node tempNext = getNext();
                toNext = ManagedChannelBuilder.forTarget(tempNext.getAddress() + ":" + tempNext.getPort()).usePlaintext(true).build();
                UpdateNeighboursBlockingStub stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                RingNetworkHandler.UpdateNeighboursResponse fromNext = stubNext.update(message);
                while (!fromNext.getOk()) {
                    removeNode(tempNext.getId());
                    RingNetworkHandler.Node suggestedNext = fromNext.getNext();
                    synchronized (next) {
                        setNext(new Node(suggestedNext.getId(), suggestedNext.getAddress(), suggestedNext.getPort()));
                    }
                    tempNext = getNext();
                    message = UpdateNeighboursMessage.newBuilder().setEntering(message.getEntering()).setExiting(message.getExiting()).setFrom(message.getFrom())
                            .setNext(suggestedNext)
                            .setPrevious(message.getPrevious()).build();
                    toNext = ManagedChannelBuilder.forTarget(tempNext.getAddress() + ":" + tempNext.getPort()).usePlaintext(true).build();
                    stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                    fromNext = stubNext.update(message);
                }
                message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                        .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();

                tempNodes=getNodes();
                for (Node n : tempNodes) {
                    if (!n.getId().equals(next.getId()) && !n.getId().equals(previous.getId()) && !n.getId().equals(node.getId())) {
                        ManagedChannel channel=ManagedChannelBuilder.forTarget(n.getAddress()+":"+n.getPort()).usePlaintext(true).build();
                        if(channel!=null){
                            UpdateNeighboursBlockingStub stub= UpdateNeighboursGrpc.newBlockingStub(channel);
                            stub.update(message);
                        }
                    }
                }
            //}
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
                if (message.getNext().getId().equals(node.getId())/*&&!message.getPrevious().getId().equals(node.getId())*/) {
                    synchronized (previous){
                        if (evaluateLeftNeighbouring(fromId)) {
                            addNode(fromNode);
                            response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                            setPrevious(fromNode);
                            System.out.println("new prev: " + previous.getId());
                        } else {
                            Node tempPrevious=getPrevious();
                            RingNetworkHandler.Node suggestedNext = RingNetworkHandler.Node.newBuilder().setId(tempPrevious.getId())
                                    .setPort(tempPrevious.getPort()).setAddress(tempPrevious.getAddress()).build();
                            response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                        }
                    }
                } else if (message.getPrevious().getId().equals(node.getId())/*&&!message.getNext().getId().equals(node.getId())*/) {
                    synchronized (next){
                        if (evaluateRightNeighbouring(fromId)) {
                            addNode(fromNode);
                            response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                            setNext(fromNode);
                            System.out.println("new next: " + next.getId());
                        } else {
                            Node tempNext=getNext();
                            RingNetworkHandler.Node suggestedPrevious = RingNetworkHandler.Node.newBuilder().setId(tempNext.getId())
                                    .setPort(tempNext.getPort()).setAddress(tempNext.getAddress()).build();
                            response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).build();
                        }
                    }
                } /*else if(message.getNext().getId().equals(node.getId())&&message.getPrevious().getId().equals(node.getId())){
                    addNode(fromNode);
                    boolean left=false,right=false;
                    synchronized (next) {
                        if(evaluateRightNeighbouring(fromId)) {
                            setNext(fromNode);
                            right=true;
                        }
                    }
                    synchronized (previous) {
                        if(evaluateLeftNeighbouring(fromId)) {
                            setPrevious(fromNode);
                            left=true;
                        }
                    }
                    System.out.println("new next e prev: "+next.getId());
                    response=RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();

                }*/ else {
                    addNode(fromNode);
                    System.out.println("aggiunto nodo: "+fromNode.getId());
                    response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                }
            }
            else{
                System.out.println("sto uscendo e ho ricevuto");
                Node tempNext=getNext();
                Node tempPrevious=getPrevious();
                if (message.getNext().getId().equals(node.getId())&&!message.getPrevious().getId().equals(node.getId())){
                    RingNetworkHandler.Node suggestedNext = RingNetworkHandler.Node.newBuilder().setId(tempNext.getId())
                            .setPort(tempPrevious.getPort()).setAddress(tempPrevious.getAddress()).build();
                    response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                }
                else if(message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())){
                    RingNetworkHandler.Node suggestedPrevious = RingNetworkHandler.Node.newBuilder().setId(tempPrevious.getId())
                            .setPort(tempNext.getPort()).setAddress(tempNext.getAddress()).build();
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
                    synchronized (previous) {
                        setPrevious(new Node(message.getPrevious().getId(), message.getPrevious().getAddress(), message.getPrevious().getPort()));
                        System.out.println("prev removed, new prev");
                    }
                } else if (message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())) {
                    synchronized (next) {
                        setNext(new Node(message.getNext().getId(), message.getNext().getAddress(), message.getNext().getPort()));
                        System.out.println("next removed, new next");
                    }
                } else if(message.getNext().getId().equals(node.getId())&&message.getPrevious().getId().equals(node.getId())){
                    synchronized (previous) {
                        setPrevious(node);
                    }
                    synchronized (next) {
                        setNext(node);
                    }
                    System.out.println("removed nex&prev, new next&prev: "+node.getId());
                } else {
                    removeNode(fromId);
                    System.out.println("rimosso nodo: "+fromNode.getId());
                }
                response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
            }
            else{
                if (message.getNext().getId().equals(node.getId())&&!message.getPrevious().getId().equals(node.getId())) {
                    Node tempNext=getNext();
                    RingNetworkHandler.Node suggestedNext= RingNetworkHandler.Node.newBuilder().setId(tempNext.getId())
                            .setPort(tempNext.getPort()).setAddress(tempNext.getAddress()).build();
                    response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).build();
                } else if (message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())) {
                    Node tempPrevious=getPrevious();
                    RingNetworkHandler.Node suggestedPrevious = RingNetworkHandler.Node.newBuilder().setAddress(previous.getAddress())
                            .setPort(tempPrevious.getPort()).setId(tempPrevious.getId()).build();
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
