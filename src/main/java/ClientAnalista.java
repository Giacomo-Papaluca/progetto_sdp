import beans.Measurement;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;

public class ClientAnalista {

    public static String gateway="localhost";
    public static int gatewayPort=8080;
    private static final String resource= "/SDP_papaluca_2020_war_exploded";

    public static void main(String[] args) {
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        String input;
        System.out.println("This is a distributed application that has the aim of measuring air pollution in a given area");
        System.out.println("COMMANDS: ");
        System.out.println("1) nodes: print the number of nodes present in the system");
        System.out.println("2) data: after giving this input a number n will be asked, given the number n, last n measurements will be printed");
        System.out.println("3) mean: after giving this input a number n will be asked, given the number n, the mean of the last n measurements will be printed");
        System.out.println("4) std: after giving this input a number n will be asked, given the number n, the std of the last n measurements will be printed");


        Client client= ClientBuilder.newClient();
        WebTarget webTarget;
        Response response;
        
        do{
            try {
                input=br.readLine();
                if(input.toLowerCase().equals("nodes")){
                    webTarget=client.target(URI.create("http://" + gateway + ":" + gatewayPort + resource)).path("/analyst/get/size/");
                    Invocation.Builder invocationBuilder= webTarget.request(MediaType.APPLICATION_JSON);
                    response = invocationBuilder.get();
                    String numNodes=response.readEntity(String.class);
                    System.out.println("Number of nodes: "+numNodes);
                }
                else if(input.toLowerCase().equals("data")){
                    int n;
                    System.out.println("Insert number n of last measurement to print: ");
                    n=Integer.parseInt(br.readLine());
                    webTarget=client.target(URI.create("http://" + gateway + ":" + gatewayPort + resource)).path("/analyst/get/statistics/"+n);
                    Invocation.Builder invocationBuilder= webTarget.request(MediaType.APPLICATION_JSON);
                    response = invocationBuilder.get();
                    List<Measurement> measurements=response.readEntity(List.class);
                    if(measurements.size()==0){
                        System.out.println("couldn't find the list of last "+n+" measurements, try later or try a lower number for n");
                    }
                    else {
                        for (Object m: measurements) {
                            System.out.println(m);
                        }
                    }
                }
                else if(input.toLowerCase().equals("mean")){
                    int n;
                    System.out.println("Insert number n of last measurements to consider for calculating mean: ");
                    n=Integer.parseInt(br.readLine());
                    webTarget=client.target(URI.create("http://" + gateway + ":" + gatewayPort + resource)).path("/analyst/get/mean/"+n);
                    Invocation.Builder invocationBuilder= webTarget.request(MediaType.TEXT_PLAIN);
                    response = invocationBuilder.get();
                    String mean = response.readEntity(String.class);
                    if(mean.equals("NaN")){
                        System.out.println("couldn't find the mean of the last "+n+" measurements, try later or try a lower number for n");
                    }
                    else {
                        System.out.println("mean: "+mean);
                    }

                }
                else if(input.toLowerCase().equals("std")){
                    int n;
                    System.out.println("Insert number n of last measurements to consider for calculating std: ");
                    n=Integer.parseInt(br.readLine());
                    webTarget=client.target(URI.create("http://" + gateway + ":" + gatewayPort + resource)).path("/analyst/get/std/"+n);
                    Invocation.Builder invocationBuilder= webTarget.request(MediaType.TEXT_PLAIN);
                    response = invocationBuilder.get();
                    String std=response.readEntity(String.class);
                    if(std.equals("NaN")){
                        System.out.println("couldn't find the std of the last "+n+" measurements, try later or try a lower number for n");
                    }
                    else {
                        System.out.println("std: "+std);
                    }

                }
                else {
                    if(!input.toLowerCase().equals("quit")) {
                        System.out.println("UNKNOWN COMMAND: " + input);
                    }
                }
            } catch (IOException e) {
                System.out.println("bad input, exception "+e.getMessage()+ " thrown, retry");
                input="";
                continue;
            }
        }while (!input.toLowerCase().equals("quit"));
    }

}
