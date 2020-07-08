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
    List<Measurement> measurements;

    private static Statistics instance;

    public  Statistics(){
        this.measurements=new ArrayList<Measurement>();
    }

    public static synchronized Statistics getInstance(){
        if(instance==null){
            instance= new Statistics();
        }
        return instance;
    }

    public synchronized List<Measurement> getLastStatistics(int n){
        try {
            return new ArrayList<Measurement>(measurements.subList(0, n));
        }catch (java.lang.IndexOutOfBoundsException e){
            return new ArrayList<>();
        }
    }

    public synchronized float getMean(int n) {
        List<Measurement> stats= getLastStatistics(n);
        float sum=0;
        for (Measurement m: stats) {
            sum+=m.value;
        }
        return sum/stats.size();
    }

    public synchronized float getStd(int n) {
        List<Measurement> stats=this.getLastStatistics(n);
        float mean=this.getMean(n);
        float sum=0;
        for (Measurement m: stats) {
            sum+=Math.pow(m.value-mean, 2);
        }
        return (float) Math.sqrt(sum/(stats.size()-1));
    }

    public synchronized void addMeasurement(Measurement measurement){
        measurements.add(0, measurement);
    }

}
