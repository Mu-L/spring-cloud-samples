package org.hongxi.cloud.sample.consumer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by javahongxi on 2026/6/1.
 */
@FeignClient("provider-sample")
public interface ProviderClient {

    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);
}
