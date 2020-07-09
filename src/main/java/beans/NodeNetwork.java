package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeNetwork {

    @XmlElement (name="nodes")
    List<Node> nodes;

    private static NodeNetwork instance;

    public NodeNetwork(){
        this.nodes=new ArrayList<Node>();
    }

    public synchronized static NodeNetwork getInstance(){
        if(instance==null){
            instance=new NodeNetwork();
        }
        return instance;
    }

    public synchronized List<Node> getNodes() {
        return new ArrayList<Node>(nodes);
    }

    public int countNodes(){
        return getNodes().size();
    }

    public synchronized NodeNetwork addNode(Node node){
        List<Node> copy=this.getNodes();
        NodeNetwork ret=null;
        String nodeId = node.getId();
        if(!containsDuplicatedID(copy, nodeId)){
            int i;
            for (i = 0; i < copy.size(); i++) {
                if (copy.get(i).getId().compareTo(nodeId)>0){ //compareTo è positivo se la prima stringa è maggiore della seconda
                    break;
                }
            }
            this.getInstance().nodes.add(i, node);
            ret=this.getInstance();
        }
        return ret;
    }

    public boolean containsDuplicatedID(List<Node> nodeList, String node){
        for (Node n: nodeList) {
            if(n.getId().equals(node)){
                return true;
            }
        }
        return false;
    }

    public synchronized boolean removeNode(String nodeId) {
        List<Node> copy = this.getNodes();
        int index=-1;
        for (Node n: copy) {
            if(n.getId().equals(nodeId)){
                index=copy.indexOf(n);
            }
        }
        if(index<0){
            return false;
        }
        this.getInstance().nodes.remove(index);
        return true;
    }

}
