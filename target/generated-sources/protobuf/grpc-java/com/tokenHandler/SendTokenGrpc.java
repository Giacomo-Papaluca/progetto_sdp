package com.tokenHandler;

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
    comments = "Source: TokenHandler.proto")
public final class SendTokenGrpc {

  private SendTokenGrpc() {}

  public static final String SERVICE_NAME = "com.tokenHandler.SendToken";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<com.tokenHandler.TokenHandler.Token,
      com.tokenHandler.TokenHandler.TokenResponse> METHOD_SEND =
      io.grpc.MethodDescriptor.<com.tokenHandler.TokenHandler.Token, com.tokenHandler.TokenHandler.TokenResponse>newBuilder()
          .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
          .setFullMethodName(generateFullMethodName(
              "com.tokenHandler.SendToken", "send"))
          .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              com.tokenHandler.TokenHandler.Token.getDefaultInstance()))
          .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
              com.tokenHandler.TokenHandler.TokenResponse.getDefaultInstance()))
          .setSchemaDescriptor(new SendTokenMethodDescriptorSupplier("send"))
          .build();

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SendTokenStub newStub(io.grpc.Channel channel) {
    return new SendTokenStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SendTokenBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new SendTokenBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SendTokenFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new SendTokenFutureStub(channel);
  }

  /**
   */
  public static abstract class SendTokenImplBase implements io.grpc.BindableService {

    /**
     */
    public void send(com.tokenHandler.TokenHandler.Token request,
        io.grpc.stub.StreamObserver<com.tokenHandler.TokenHandler.TokenResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_SEND, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SEND,
            asyncUnaryCall(
              new MethodHandlers<
                com.tokenHandler.TokenHandler.Token,
                com.tokenHandler.TokenHandler.TokenResponse>(
                  this, METHODID_SEND)))
          .build();
    }
  }

  /**
   */
  public static final class SendTokenStub extends io.grpc.stub.AbstractStub<SendTokenStub> {
    private SendTokenStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SendTokenStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SendTokenStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SendTokenStub(channel, callOptions);
    }

    /**
     */
    public void send(com.tokenHandler.TokenHandler.Token request,
        io.grpc.stub.StreamObserver<com.tokenHandler.TokenHandler.TokenResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_SEND, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class SendTokenBlockingStub extends io.grpc.stub.AbstractStub<SendTokenBlockingStub> {
    private SendTokenBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SendTokenBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SendTokenBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SendTokenBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.tokenHandler.TokenHandler.TokenResponse send(com.tokenHandler.TokenHandler.Token request) {
      return blockingUnaryCall(
          getChannel(), METHOD_SEND, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class SendTokenFutureStub extends io.grpc.stub.AbstractStub<SendTokenFutureStub> {
    private SendTokenFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SendTokenFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SendTokenFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SendTokenFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.tokenHandler.TokenHandler.TokenResponse> send(
        com.tokenHandler.TokenHandler.Token request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_SEND, getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SendTokenImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SendTokenImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND:
          serviceImpl.send((com.tokenHandler.TokenHandler.Token) request,
              (io.grpc.stub.StreamObserver<com.tokenHandler.TokenHandler.TokenResponse>) responseObserver);
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

  private static abstract class SendTokenBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SendTokenBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.tokenHandler.TokenHandler.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SendToken");
    }
  }

  private static final class SendTokenFileDescriptorSupplier
      extends SendTokenBaseDescriptorSupplier {
    SendTokenFileDescriptorSupplier() {}
  }

  private static final class SendTokenMethodDescriptorSupplier
      extends SendTokenBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SendTokenMethodDescriptorSupplier(String methodName) {
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
      synchronized (SendTokenGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SendTokenFileDescriptorSupplier())
              .addMethod(METHOD_SEND)
              .build();
        }
      }
    }
    return result;
  }
}
