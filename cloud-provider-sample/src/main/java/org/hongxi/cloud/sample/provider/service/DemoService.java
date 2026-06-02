package org.hongxi.cloud.sample.provider.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by javahongxi on 2026/6/1.
 */
@RestController
public class DemoService {
    @Value("${server.port}")
    private String port;

    @RequestMapping("/hello")
    public String hello(String name) {
        return "Hi, " + name + ", Here is " + port;
    }
}
