package beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Statistics {

    @XmlElement (name="measurement")
    SortedSet<Measurement> measurements;

    private static Statistics instance;

    public  Statistics(){
        this.measurements=new TreeSet<Measurement>(new Comparator<Measurement>() {
            public int compare(Measurement a, Measurement b) {
                int chaeck= a.timestamp.compareTo(b.timestamp);
                if(chaeck<0){
                    return 1;
                }
                else if(chaeck==0){
                    return 0;
                }
                else{
                    return -1;
                }
            }
        });
    }

    public static synchronized Statistics getInstance(){
        if(instance==null){
            instance= new Statistics();
        }
        return instance;
    }

    public synchronized List<Measurement> getLastStatistics(int n){
        List<Measurement> picture=new ArrayList<Measurement>(measurements);
        return picture.subList(0,n);
    }

    public float getMean(int n) {
        List<Measurement> stats= this.getLastStatistics(n);
        float sum=0;
        for (Measurement m: stats) {
            sum+=m.value;
        }
        return sum/stats.size();
    }

    public float getStd(int n) {
        List<Measurement> stats=this.getLastStatistics(n);
        float mean=this.getMean(n);
        float sum=0;
        for (Measurement m: stats) {
            sum+=Math.pow(m.value-mean, 2);
        }
        return (float) Math.sqrt(sum/(stats.size()-1));
    }

    public synchronized void addMeasurement(Measurement measurement){
        this.measurements.add(measurement);
    }

}
