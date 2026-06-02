package org.hongxi.cloud.sample.consumer.controller;

import org.apache.dubbo.config.annotation.DubboReference;
import org.hongxi.cloud.sample.api.DemoService;
import org.hongxi.cloud.sample.consumer.client.ProviderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Created by shenhongxi on 2017/9/14.
 */
@RestController
public class DemoController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProviderClient providerClient;

    @Autowired
    private DiscoveryClient discoveryClient;

    @DubboReference(check = false)
    private DemoService demoService;

    @RequestMapping("/hi")
    public String hi(String name) {
        return restTemplate.getForObject(
                "http://provider-sample/hello?name=" + name, String.class);
    }

    @RequestMapping("/hi/feign")
    public String hiFeign(String name) {
        return providerClient.hello(name);
    }

    @GetMapping("/services/{service}")
    public Object client(@PathVariable String service) {
        return discoveryClient.getInstances(service);
    }

    @GetMapping("/services")
    public Object services() {
        return discoveryClient.getServices();
    }

    @RequestMapping("/dubbo")
    public String sayHello(String name) {
        return demoService.sayHello(name);
    }
}
