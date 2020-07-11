package com.netHandler;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.7.0)",
    comments = "Source: RingNetworkHandler.proto")
public final class UpdateNeighboursGrpc {

  private UpdateNeighboursGrpc() {}

  public static final String SERVICE_NAME = "com.netHandler.UpdateNeighbours";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.netHandler.RingNetworkHandler.UpdateNeighboursMessage,
      com.netHandler.RingNetworkHandler.UpdateNeighboursResponse> METHOD_UPDATE =
      io.grpc.MethodDescriptor.<com.netHandler.RingNetworkHandler.UpdateNeighboursMessage, com.netHandler.RingNetworkHandler.UpdateNeighboursResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "com.netHandler.UpdateNeighbours", "update"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              com.netHandler.RingNetworkHandler.UpdateNeighboursMessage.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              com.netHandler.RingNetworkHandler.UpdateNeighboursResponse.getDefaultInstance()))
          .setSchemaDescriptor(new UpdateNeighboursMethodDescriptorSupplier("update"))
          .build();

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UpdateNeighboursStub newStub(io.grpc.Channel channel) {
    return new UpdateNeighboursStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UpdateNeighboursBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new UpdateNeighboursBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UpdateNeighboursFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new UpdateNeighboursFutureStub(channel);
  }

  /**
   */
  public static abstract class UpdateNeighboursImplBase implements io.grpc.BindableService {

    /**
     */
    public void update(com.netHandler.RingNetworkHandler.UpdateNeighboursMessage request,
        io.grpc.stub.StreamObserver<com.netHandler.RingNetworkHandler.UpdateNeighboursResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_UPDATE, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_UPDATE,
            asyncUnaryCall(
              new MethodHandlers<
                com.netHandler.RingNetworkHandler.UpdateNeighboursMessage,
                com.netHandler.RingNetworkHandler.UpdateNeighboursResponse>(
                  this, METHODID_UPDATE)))
          .build();
    }
  }

  /**
   */
  public static final class UpdateNeighboursStub extends io.grpc.stub.AbstractStub<UpdateNeighboursStub> {
    private UpdateNeighboursStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UpdateNeighboursStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UpdateNeighboursStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UpdateNeighboursStub(channel, callOptions);
    }

    /**
     */
    public void update(com.netHandler.RingNetworkHandler.UpdateNeighboursMessage request,
        io.grpc.stub.StreamObserver<com.netHandler.RingNetworkHandler.UpdateNeighboursResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_UPDATE, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class UpdateNeighboursBlockingStub extends io.grpc.stub.AbstractStub<UpdateNeighboursBlockingStub> {
    private UpdateNeighboursBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UpdateNeighboursBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UpdateNeighboursBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UpdateNeighboursBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.netHandler.RingNetworkHandler.UpdateNeighboursResponse update(com.netHandler.RingNetworkHandler.UpdateNeighboursMessage request) {
      return blockingUnaryCall(
          getChannel(), METHOD_UPDATE, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class UpdateNeighboursFutureStub extends io.grpc.stub.AbstractStub<UpdateNeighboursFutureStub> {
    private UpdateNeighboursFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private UpdateNeighboursFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UpdateNeighboursFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new UpdateNeighboursFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.netHandler.RingNetworkHandler.UpdateNeighboursResponse> update(
        com.netHandler.RingNetworkHandler.UpdateNeighboursMessage request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_UPDATE, getCallOptions()), request);
    }
  }

  private static final int METHODID_UPDATE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final UpdateNeighboursImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(UpdateNeighboursImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_UPDATE:
          serviceImpl.update((com.netHandler.RingNetworkHandler.UpdateNeighboursMessage) request,
              (io.grpc.stub.StreamObserver<com.netHandler.RingNetworkHandler.UpdateNeighboursResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class UpdateNeighboursBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    UpdateNeighboursBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.netHandler.RingNetworkHandler.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("UpdateNeighbours");
    }
  }

  private static final class UpdateNeighboursFileDescriptorSupplier
      extends UpdateNeighboursBaseDescriptorSupplier {
    UpdateNeighboursFileDescriptorSupplier() {}
  }

  private static final class UpdateNeighboursMethodDescriptorSupplier
      extends UpdateNeighboursBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    UpdateNeighboursMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (UpdateNeighboursGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new UpdateNeighboursFileDescriptorSupplier())
              .addMethod(METHOD_UPDATE)
              .build();
        }
      }
    }
    return result;
  }
}
