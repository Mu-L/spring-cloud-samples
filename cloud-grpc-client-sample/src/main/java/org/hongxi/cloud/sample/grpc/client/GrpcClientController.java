package org.hongxi.cloud.sample.grpc.client;

import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grpc")
public class GrpcClientController {
    private static final Logger log = LoggerFactory.getLogger(GrpcClientController.class);

    @Autowired
    private GreeterGrpc.GreeterBlockingStub greeterBlockingStub;

    @GetMapping("/hello")
    public String hello(String name, @RequestHeader(value = "traceparent", required = false) String traceparent) {
        log.info("traceparent: {}", traceparent);
        log.info("Calling gRPC service, {}", name);
        GreeterRequest request = GreeterRequest.newBuilder().setName(name).build();
        return greeterBlockingStub.greet(request).getMessage();
    }
}
