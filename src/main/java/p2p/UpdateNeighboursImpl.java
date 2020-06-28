package p2p;

import com.netHandler.UpdateNeighboursGrpc.*;
import com.netHandler.RingNetworkHandler.*;
import io.grpc.stub.StreamObserver;

import java.util.HashSet;
import java.util.LinkedHashSet;

public class UpdateNeighboursImpl extends UpdateNeighboursImplBase {

    HashSet<StreamObserver> observers = new LinkedHashSet<StreamObserver>();

    public StreamObserver<UpdateNeighboursMessage> update(StreamObserver<UpdateNeighboursMessage> responseObserver){

        synchronized (observers){
            observers.add(responseObserver);
        }

        return new StreamObserver<UpdateNeighboursMessage>() {
            @Override
            public void onNext(UpdateNeighboursMessage message) {
                System.out.println("impl");
                HashSet<StreamObserver> copy;

                synchronized (observers) {
                    copy = new HashSet<>(observers);
                }


                for(StreamObserver<UpdateNeighboursMessage> observer: copy){
                    if(!observer.equals(responseObserver)) {
                        try {
                            observer.onNext(message);
                        } catch (io.grpc.StatusRuntimeException e) {
                            //peer no longer available
                            synchronized (observers) {
                                observers.remove(responseObserver);
                            }
                        }
                    }
                }



            }

            @Override
            public void onError(Throwable throwable) {
                synchronized (observers) {
                    observers.remove(responseObserver);
                }
            }

            @Override
            public void onCompleted() {
                synchronized (observers) {
                    observers.remove(responseObserver);
                }
            }
        };
    }

}
