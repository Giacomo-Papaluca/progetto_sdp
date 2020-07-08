package beans;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

@XmlRootElement
public class Measurement {

    String sensorId;
    float value;
    long timestamp;

    public Measurement(){}

    public Measurement(String sensorId, float value, long timestamp){
        this.sensorId=sensorId;
        this.value=value;
        this.timestamp=timestamp;
    }

    public float getValue() { return value; }
    public void setValue(float value) {
        this.value = value;
    }

    public String getSensorId() {
        return sensorId;
    }
    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
