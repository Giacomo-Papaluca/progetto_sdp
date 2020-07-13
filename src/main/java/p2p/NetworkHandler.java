package p2p;

import beans.Node;
import com.netHandler.RingNetworkHandler;
import com.netHandler.RingNetworkHandler.UpdateNeighboursMessage;
import com.netHandler.UpdateNeighboursGrpc;
import com.netHandler.UpdateNeighboursGrpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class NetworkHandler extends UpdateNeighboursImplBase implements Runnable{

    Node node;
    Node next;
    Node previous;
    List<Node> nodes;
    boolean entering;
    boolean exiting;
    boolean justTwo;
    ManagedChannel toNext;
    ManagedChannel toPrevious;
    ThreadHandler threadHandler;
    TokenHandler tokenHandler;

    public NetworkHandler(){
        justTwo=false;
        entering=true;
        exiting=false;
    }

    public void setNode(Node node){
        this.node=node;
    }

    public void setNodes(List<Node> nodes){
        this.nodes = nodes;
    }

    public synchronized void setNext(Node next){
        this.next=next;
        tokenHandler.setDestination(next);
    }

    public synchronized void setPrevious(Node previous){
        this.previous=previous;
    }

    public void setTokenHandler(TokenHandler tokenHandler){
        this.tokenHandler=tokenHandler;
    }

    public void setThreadHandler(ThreadHandler threadHandler){
        this.threadHandler=threadHandler;
    }

    public Node getNext(){
        synchronized (next){return new Node(next.getId(), next.getAddress(), next.getPort());}
    }

    public Node getPrevious(){
        synchronized (previous){return new Node(previous.getId(), previous.getAddress(), previous.getPort());}
    }

    public List<Node> getNodes() {
        synchronized (nodes){return new ArrayList<>(nodes);}
    }

    public RingNetworkHandler.Node nodeBeanToMessage(Node node){
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
            tokenHandler.setNetworkSize(getNodes().size());
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
            tokenHandler.setNetworkSize(getNodes().size());
        }
    }

    private boolean evaluateLeftNeighbouring(String fromId){
        Node tempPrevious=getPrevious();
        List<Node> tempNodes=getNodes();
        if(tempPrevious.getId().equals(fromId)){
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
        if(tempNext.getId().equals(fromId)){
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
        System.out.println("io: "+node.getId()+ " ho come prev: "+getPrevious().getId()+" e next: "+getNext().getId());

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
            if(!justTwo&&tempNodes.size()!=2) {
                while (true) {
                    try {
                        Node tempPrevious = getPrevious();
                        toPrevious = ManagedChannelBuilder.forTarget(tempPrevious.getAddress() + ":" + tempPrevious.getPort()).usePlaintext(true).build();
                        UpdateNeighboursBlockingStub stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                        RingNetworkHandler.UpdateNeighboursResponse fromPrevious = stubPrevious.update(message);
                        toPrevious.shutdown();
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
                            toPrevious.shutdown();
                        }
                        System.out.println(fromPrevious);
                        break;
                    } catch (io.grpc.StatusRuntimeException exc) {
                        synchronized (previous) {
                            setPrevious(findPrev(node));
                        }
                        if (getPrevious().getId().equals(node.getId())) {
                            break;
                        } else {
                            continue;
                        }
                    }
                }

                message = UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                        .setFrom(nodeBeanToMessage(node))
                        .setNext(nodeBeanToMessage(getNext()))
                        .setPrevious(nodeBeanToMessage(getPrevious()))
                        .build();
            }
            while (true) {
                try {
                    Node tempNext = getNext();
                    toNext = ManagedChannelBuilder.forTarget(tempNext.getAddress() + ":" + tempNext.getPort()).usePlaintext(true).build();
                    UpdateNeighboursBlockingStub stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                    RingNetworkHandler.UpdateNeighboursResponse fromNext = stubNext.update(message);
                    toNext.shutdown();
                    while (!fromNext.getOk()) {
                        if (fromNext.hasNext() && fromNext.hasPrevious()) {
                            System.out.println("not ok has prev and next");
                            setNext(new Node(fromNext.getNext().getId(), fromNext.getNext().getAddress(), fromNext.getNext().getPort()));
                            setPrevious(new Node(fromNext.getPrevious().getId(), fromNext.getPrevious().getAddress(), fromNext.getPrevious().getPort()));
                            break;
                        }
                        System.out.println(node.getId() + " received next no ok, suggested: " + fromNext.getNext().getId()+" "+System.currentTimeMillis());
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
                        toNext.shutdown();
                    }
                    System.out.println(fromNext);
                    break;
                }catch (io.grpc.StatusRuntimeException exc){
                    synchronized (next){
                        setNext(findNext(node));
                    }
                    if(getNext().getId().equals(node.getId())){
                        break;
                    }
                    else{
                        continue;
                    }
                }
            }

            List<Node> picture=getNodes();
            for (Node n : picture) {
                if (!n.getId().equals(getNext().getId()) && !n.getId().equals(getPrevious().getId()) && !n.getId().equals(node.getId())) {
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(n.getAddress() + ":" + n.getPort()).usePlaintext(true).build();
                    if (channel != null) {
                        UpdateNeighboursBlockingStub stub = UpdateNeighboursGrpc.newBlockingStub(channel);
                        try {
                            stub.update(message);
                            channel.shutdown();
                        }catch (io.grpc.StatusRuntimeException exc){
                            System.out.println("nodo offline, non aggiornato");
                            removeNode(n.getId());
                        }
                    }
                }
            }

        }

        threadHandler.notifyDestinationSet();

        entering = false;

        threadHandler.waitForUser();

        tokenHandler.exiting=true;
        exiting=true;

        System.out.println("sleep");
        try {
            Random s=new Random();
            Thread.sleep(1000*s.nextInt(8));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("io: "+node.getId()+" next: "+getNext().getId()+" prev: "+getPrevious().getId());
        System.out.println("lista");
        tempNodes=getNodes();
        for (Node n: tempNodes) {
            System.out.println(n.getId());
        }

        if(tempNodes.size()>1) {
            UpdateNeighboursMessage message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                    .setNext(nodeBeanToMessage(getNext())).setPrevious(nodeBeanToMessage(getPrevious())).setFrom(nodeBeanToMessage(node)).build();

            while (true) {
                try {
                    Node tempPrevious = getPrevious();
                    toPrevious = ManagedChannelBuilder.forTarget(tempPrevious.getAddress() + ":" + tempPrevious.getPort()).usePlaintext(true).build();
                    UpdateNeighboursBlockingStub stubPrevious = UpdateNeighboursGrpc.newBlockingStub(toPrevious);
                    RingNetworkHandler.UpdateNeighboursResponse fromPrevious = stubPrevious.update(message);
                    toPrevious.shutdown();
                    while (!fromPrevious.getOk()) {
                        System.out.println("exiting, from previous not ok");
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
                        toPrevious.shutdown();
                    }
                    break;
                }catch (io.grpc.StatusRuntimeException exc){
                    synchronized (previous){
                        removeNode(previous.getId());
                        setPrevious(findPrev(node));
                    }
                    if(getPrevious().getId().equals(node.getId())){
                        break;
                    }
                    else {
                        continue;
                    }
                }
            }
            message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                    .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();

            while (true) {
                try {
                    Node tempNext = getNext();
                    toNext = ManagedChannelBuilder.forTarget(tempNext.getAddress() + ":" + tempNext.getPort()).usePlaintext(true).build();
                    UpdateNeighboursBlockingStub stubNext = UpdateNeighboursGrpc.newBlockingStub(toNext);
                    RingNetworkHandler.UpdateNeighboursResponse fromNext = stubNext.update(message);
                    toNext.shutdown();
                    while (!fromNext.getOk()) {
                        System.out.println("exiting, from next no ok");
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
                        toNext.shutdown();
                    }
                    break;
                }catch (io.grpc.StatusRuntimeException exc){
                    synchronized (next){
                        removeNode(next.getId());
                        setNext(findNext(node));
                    }
                    if(getNext().getId().equals(node.getId())){
                        break;
                    }
                    else {
                        continue;
                    }
                }
            }
            message = UpdateNeighboursMessage.newBuilder().setExiting(exiting).setEntering(entering)
                    .setNext(nodeBeanToMessage(next)).setPrevious(nodeBeanToMessage(previous)).setFrom(nodeBeanToMessage(node)).build();

            List<Node> picture=getNodes();
            for (Node n : picture) {
                if (!n.getId().equals(next.getId()) && !n.getId().equals(previous.getId()) && !n.getId().equals(node.getId())) {
                    ManagedChannel channel=ManagedChannelBuilder.forTarget(n.getAddress()+":"+n.getPort()).usePlaintext(true).build();
                    if(channel!=null){
                        UpdateNeighboursBlockingStub stub= UpdateNeighboursGrpc.newBlockingStub(channel);
                        try {
                            stub.update(message);
                            channel.shutdown();
                        }catch (io.grpc.StatusRuntimeException exc){
                            System.out.println("nodo offline, non aggiornato");
                            removeNode(n.getId());
                        }
                    }
                }
            }
        }
        System.out.println("bye");

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
                } else if (message.getPrevious().getId().equals(node.getId())&&!message.getNext().getId().equals(node.getId())) {
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
                } else if(message.getNext().getId().equals(node.getId())&&message.getPrevious().getId().equals(node.getId())){
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
                    if(left&&right) {
                        addNode(fromNode);
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(true).build();
                    }
                    else if(left){
                        addNode(fromNode);
                        RingNetworkHandler.Node suggestedPrevious= nodeBeanToMessage(next);
                        response= RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setPrevious(suggestedPrevious).setNext(nodeBeanToMessage(node)).build();
                    }
                    else if(right){
                        addNode(fromNode);
                        RingNetworkHandler.Node suggestedNext= nodeBeanToMessage(previous);
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).setPrevious(nodeBeanToMessage(node)).build();
                    }
                    else{
                        RingNetworkHandler.Node suggestedPrevious= nodeBeanToMessage(next);
                        RingNetworkHandler.Node suggestedNext= nodeBeanToMessage(previous);
                        response = RingNetworkHandler.UpdateNeighboursResponse.newBuilder().setOk(false).setNext(suggestedNext).setPrevious(suggestedPrevious).build();
                    }

                } else {
                    synchronized (next) {   //controllo comunque, potrebbe essermi arrivato un update mentre la mia lista non è aggiornata
                        if(evaluateRightNeighbouring(fromId)) {
                            setNext(fromNode);
                            System.out.println("next update con lista non aggiornata"+fromId);
                        }
                    }
                    synchronized (previous) {
                        if(evaluateLeftNeighbouring(fromId)) {
                            setPrevious(fromNode);
                            System.out.println("prev update con lista non aggiornata"+fromId);
                        }
                    }
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
