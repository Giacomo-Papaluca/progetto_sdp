package p2p;

import beans.Node;
import com.tokenHandler.SendTokenGrpc;

public class TokenHandler extends SendTokenGrpc.SendTokenImplBase implements Runnable{

    Node node;
    Node destination;
    int networkSize;

    public TokenHandler(){}

    public void setNode(Node node) {
        this.node=node;
    }

    public void setDestination(Node destination){
        this.destination=destination;
    }

    public void setNetworkSize(int size){
        networkSize=size;
    }

    @Override
    public void run() {

    }


}
