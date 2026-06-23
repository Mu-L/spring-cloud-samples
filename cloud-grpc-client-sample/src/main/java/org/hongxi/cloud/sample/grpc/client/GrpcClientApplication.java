package org.hongxi.cloud.sample.grpc.client;

import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ImportGrpcClients(basePackages = "org.hongxi.cloud.sample.idl.unary")
public class GrpcClientApplication {
    private static final Logger log = LoggerFactory.getLogger(GrpcClientApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(GreeterGrpc.GreeterBlockingStub stub) {
        return args -> {
            GreeterRequest request = GreeterRequest.newBuilder().setName("lily").build();
            log.info("Calling gRPC service, result: {}", stub.greet(request));
        };
    }
}
