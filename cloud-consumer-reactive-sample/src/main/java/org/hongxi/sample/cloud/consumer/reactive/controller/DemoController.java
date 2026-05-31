package org.hongxi.sample.cloud.consumer.reactive.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Created by shenhongxi on 2017/9/14.
 */
@RestController
public class DemoController {

    @Autowired
    private WebClient webClient;

    @Autowired
    private ReactiveDiscoveryClient reactiveDiscoveryClient;

    @RequestMapping("/hi")
    public Mono<String> hi(String name) {
        return webClient
                .get()
                .uri("/hello?name={name}", name)
                .retrieve()
                .bodyToMono(String.class);
    }

    @GetMapping("/services")
    public Flux<String> allServices() {
        return reactiveDiscoveryClient.getInstances("demo-provider-reactive")
                .map(serviceInstance -> serviceInstance.getHost() + ":"
                        + serviceInstance.getPort());
    }
}