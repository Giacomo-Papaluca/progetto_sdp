package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ThreadHandler {

    boolean exit;

    ThreadHandler(){
        exit=false;
    }

    public synchronized void waitForUser() {
        try{
            while (!exit){
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void notifyExit() {
        exit=true;
        notify();
    }
}
