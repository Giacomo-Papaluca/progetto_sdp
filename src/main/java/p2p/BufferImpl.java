package p2p;

import pm10.Buffer;
import pm10.Measurement;

import java.util.ArrayList;
import java.util.List;

public class BufferImpl implements Buffer {

    List<Measurement> buffer;
    float aggregated_value;
    long most_recent_timestamp;
    boolean newMeasurement;

    public BufferImpl(){
        buffer=new ArrayList<>();
        most_recent_timestamp=-1;
        newMeasurement=false;
    }

    @Override
    public synchronized void addMeasurement(Measurement m) {
        buffer.add(m);
        if (buffer.size()==12){
            int sum=0;
            for (Measurement measurement: buffer) {
                sum+=measurement.getValue();
            }
            aggregated_value=sum/buffer.size();
            most_recent_timestamp=m.getTimestamp();
            buffer.subList(0,6).clear();
            notifyAggregatedValue();
        }
    }

    public synchronized void notifyAggregatedValue(){
        newMeasurement=true;
        this.notify();
    }

    public float getAggregatedValue(){
        return aggregated_value;
    }

    public long getMostRecentTimestamp() {
        return most_recent_timestamp;
    }

    public synchronized void waitForMeasurement() {
        try {
            while (!newMeasurement) {
                this.wait();
            }
            newMeasurement=false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
