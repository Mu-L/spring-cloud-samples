package org.hongxi.cloud.sample.grpc.server;

import io.grpc.stub.StreamObserver;
import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hongxi.cloud.sample.idl.unary.GreeterReply;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class GreeterImpl extends GreeterGrpc.GreeterImplBase {

    private static final Logger log = LoggerFactory.getLogger(GreeterImpl.class);
    @Override
    public void greet(GreeterRequest request, StreamObserver<GreeterReply> responseObserver) {
        log.info("Received request: {}", request.getName());
        responseObserver.onNext(GreeterReply.newBuilder().setMessage("Hello, " + request.getName()).build());
        responseObserver.onCompleted();
    }
}