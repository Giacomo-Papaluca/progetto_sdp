package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ThreadHandler {

    boolean exit;
    boolean destinaitonSet;
    boolean tokenAcquired;

    ThreadHandler(){
        exit=false;
        destinaitonSet=false;
        tokenAcquired=false;
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
        notifyAll();
    }

    public synchronized void waitForDestination() {
        try{
            while (!destinaitonSet){
                wait();
            }
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public synchronized void notifyDestinationSet() {
        destinaitonSet=true;
        notifyAll();
    }

    public void acquiredToken() {
        tokenAcquired=true;
    }

    public synchronized void waitForTokenRelease() {
        while (tokenAcquired){
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void releaseToken() {
        tokenAcquired=false;
        this.notify();
    }
}
