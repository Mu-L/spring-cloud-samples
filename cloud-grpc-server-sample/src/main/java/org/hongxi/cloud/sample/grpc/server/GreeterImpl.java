package org.hongxi.cloud.sample.grpc.server;

import io.grpc.stub.StreamObserver;
import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterReply;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class GreeterImpl extends GreeterGrpc.GreeterImplBase {

    private static final Logger log = LoggerFactory.getLogger(GreeterImpl.class);

    @Value("${spring.grpc.server.port}")
    private int grpcPort;

    @Override
    public void greet(GreeterRequest request, StreamObserver<GreeterReply> responseObserver) {
        log.info("Received request: {}", request.getName());
        String message = "Hello, " + request.getName() + " (from port " + grpcPort + ")";
        responseObserver.onNext(GreeterReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }
}