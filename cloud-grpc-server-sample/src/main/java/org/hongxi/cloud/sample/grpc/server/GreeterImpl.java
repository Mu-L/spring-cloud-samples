package org.hongxi.cloud.sample.grpc.server;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterReply;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
public class GreeterImpl extends GreeterGrpc.GreeterImplBase {
    @Override
    public void greet(GreeterRequest request, StreamObserver<GreeterReply> responseObserver) {
        log.info("Received request: {}", request.getName());
        responseObserver.onNext(GreeterReply.newBuilder().setMessage("Hello, " + request.getName()).build());
        responseObserver.onCompleted();
    }
}