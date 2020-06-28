package p2p;

import beans.Node;
import beans.NodeNetwork;
import com.netHandler.RingNetworkHandler.UpdateNeighboursMessage;
import com.netHandler.UpdateNeighboursGrpc;
import com.netHandler.UpdateNeighboursGrpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class NetworkHandler implements Runnable{

    Node node;
    Node next;
    Node previous;
    NodeNetwork nodeNetwork;
    boolean entering;
    boolean exiting;
    ManagedChannel toNext;
    ManagedChannel toPrevious;
    ThreadHandler threadHandler;

    public NetworkHandler(Node node, NodeNetwork nodeNetwork, ThreadHandler threadHandler){
        this.threadHandler = threadHandler;
        this.node=node;
        this.nodeNetwork=nodeNetwork;
        List<Node> nodes = nodeNetwork.getNodes();
        for (Node n: nodes) {
            System.out.println(n.getId());
        }
        next=findNext(node, nodes);
        previous=findPrev(node, nodes);
        entering = true;
        exiting = false;
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
        System.out.println("prev="+previous.getId()+"  next="+next.getId());
        if(nodeNetwork.getNodes().size()>1) {
            toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
            toPrevious = ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
            UpdateNeighboursStub stubNext = UpdateNeighboursGrpc.newStub(toNext);
            UpdateNeighboursStub stubPrevious = UpdateNeighboursGrpc.newStub(toPrevious);


            ////// DA NEXT NON MI PUÒ ARRIVARE UN MESSAGGIO INOLTRATO DI AGGIORNAMENTO RETE MA SOLO INSERIMENTI DIRETTI /////
            StreamObserver<UpdateNeighboursMessage> streamNext = stubNext.update(new StreamObserver<UpdateNeighboursMessage>() {
                @Override
                public void onNext(UpdateNeighboursMessage message) {
                    System.out.println("next");
                    List<UpdateNeighboursMessage.Node> messageNodes = message.getNetworkList();
                    String fromID = message.getFrom().getId();
                    UpdateNeighboursMessage.Node messageNext = message.getNext();
                    UpdateNeighboursMessage.Node messagePrevious = message.getPrevious();
                    String myId = node.getId();
                    if (message.getEntering()) {
                        if (messageNext.getId().equals(myId)) { //// non dovrebbe capitare perchè se arriva da next quello che entra sarà il next di me
                            System.out.println("io, " + myId + ", sono il next di " + fromID);
                            toPrevious.shutdown();
                            previous = new Node(messagePrevious.getId(), messagePrevious.getAddress(), messagePrevious.getPort());
                            toPrevious = ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                        } else if (messagePrevious.getId().equals(myId)) {
                            System.out.println("io, " + myId + ", sono il prev di " + fromID);
                            toNext.shutdown();
                            next = new Node(messageNext.getId(), messageNext.getAddress(), messageNext.getPort());
                            toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                        } else if (fromID.equals(myId)) {
                            System.out.println("NON DOVREBBE SUCCEDERE");
                        } else {
                            //// vorrei inoltrare al mio next un messaggio con entering e exiting a false solo per inviare la lista aggiornata agli altri nodi
                        }
                    }
                    /// tutti gli altri messaggi dal mio next li droppo.

                }

                @Override
                public void onError(Throwable throwable) {
                    //throwable.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    toNext.shutdown();
                }
            });

            ///// QUANDO IMPLEMENTO STREAMPREV SE MI ARRIVA UN UPDATE INDIRETTO LO GIRO A NEXT /////
            StreamObserver<UpdateNeighboursMessage> streamPrevious = stubPrevious.update(new StreamObserver<UpdateNeighboursMessage>() {
                @Override
                public void onNext(UpdateNeighboursMessage message) {
                    System.out.println("prev");
                    List<UpdateNeighboursMessage.Node> messageNodes = message.getNetworkList();
                    String fromID = message.getFrom().getId();
                    UpdateNeighboursMessage.Node messageNext = message.getNext();
                    UpdateNeighboursMessage.Node messagePrevious = message.getPrevious();
                    String myId = node.getId();
                    if (message.getEntering()) {
                        if (messagePrevious.getId().equals(myId)) {  /////// non dovrebbe succedere
                            System.out.println("io, " + myId + ", sono il prev di " + fromID);
                            toNext.shutdown();
                            next = new Node(messageNext.getId(), messageNext.getAddress(), messageNext.getPort());
                            toNext = ManagedChannelBuilder.forTarget(next.getAddress() + ":" + next.getPort()).usePlaintext(true).build();
                        }
                        if (messageNext.getId().equals(myId)) {
                            System.out.println("io, " + myId + ", sono il next di " + fromID);
                            toPrevious.shutdown();
                            previous = new Node(messagePrevious.getId(), messagePrevious.getAddress(), messagePrevious.getPort());
                            toPrevious = ManagedChannelBuilder.forTarget(previous.getAddress() + ":" + previous.getPort()).usePlaintext(true).build();
                            //inoltro l'update;
                            entering = false;
                            UpdateNeighboursMessage fwdMessage = UpdateNeighboursMessage.newBuilder().setNext(messageNext).addAllNetwork(messageNodes)
                                    .setFrom(message.getFrom()).setPrevious(messagePrevious).setExiting(exiting).setEntering(entering).build();
                            streamNext.onNext(fwdMessage);
                        }
                    } else {
                        if (!message.getExiting()) {
                            //to do: metodo per aggiornare la nodeNetwork.
                            for (UpdateNeighboursMessage.Node n : message.getNetworkList()) {           ///// ovviamente non così
                                nodeNetwork.removeNode(n.getId());
                            }
                            for (UpdateNeighboursMessage.Node n : message.getNetworkList()) {
                                nodeNetwork.addNode(new Node(n.getId(), n.getAddress(), n.getPort()));
                            }
                        }
                    }

                }

                @Override
                public void onError(Throwable throwable) {
                    //throwable.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    toPrevious.shutdown();
                }
            });


            if (nodeNetwork.getNodes().size() > 2) {
                System.out.println("nellif");
                UpdateNeighboursMessage.Builder builder = UpdateNeighboursMessage.newBuilder().setEntering(entering).setExiting(exiting)
                        .setFrom(UpdateNeighboursMessage.Node.newBuilder().setAddress(node.getAddress()).setId(node.getId()).setPort(node.getPort()).build())
                        .setNext(UpdateNeighboursMessage.Node.newBuilder().setAddress(next.getAddress()).setId(next.getId()).setPort(next.getPort()).build())
                        .setPrevious(UpdateNeighboursMessage.Node.newBuilder().setAddress(previous.getAddress()).setId(previous.getId()).setPort(previous.getPort()).build());
                for (Node n : nodeNetwork.getNodes()) {
                    builder.addNetwork(UpdateNeighboursMessage.Node.newBuilder()
                            .setAddress(n.getAddress()).setId(n.getId()).setPort(n.getPort())
                            .build());
                }
                System.out.println("primadinext");
                streamNext.onNext(builder.build());
                System.out.println("doponext");
                streamPrevious.onNext(builder.build());
                System.out.println("dopoprev");
                entering = false;
            }
        }

        for (Node n: nodeNetwork.getNodes()) {
            System.out.println(n.getId());
        }
        threadHandler.waitForUser();

        System.out.println("bye");
    }
}
