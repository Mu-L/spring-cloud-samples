package org.hongxi.cloud.sample.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;

/**
 * Created by shenhongxi on 2020/7/12.
 */
@EnableDiscoveryClient
@SpringBootApplication
@LoadBalancerClients({
        @LoadBalancerClient("consumer-reactive-sample"),
        @LoadBalancerClient("consumer-sample")
})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
