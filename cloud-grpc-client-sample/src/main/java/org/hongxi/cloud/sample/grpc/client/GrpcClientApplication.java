package org.hongxi.cloud.sample.grpc.client;

import lombok.extern.slf4j.Slf4j;
import org.hongxi.cloud.sample.idl.unary.GreeterGrpc;
import org.hongxi.cloud.sample.idl.unary.GreeterRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.ImportGrpcClients;

@Slf4j
@SpringBootApplication
@ImportGrpcClients(basePackages = "org.hongxi.cloud.sample.idl.unary")
public class GrpcClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(GreeterGrpc.GreeterBlockingStub stub) {
        return args -> {
            log.info("{}", stub.greet(GreeterRequest.newBuilder().setName("lily").build()));
        };
    }
}
