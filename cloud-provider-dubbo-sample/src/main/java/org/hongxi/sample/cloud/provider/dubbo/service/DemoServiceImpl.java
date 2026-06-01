package org.hongxi.sample.cloud.provider.dubbo.service;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.rpc.RpcContext;
import org.hongxi.sample.cloud.api.DemoService;

@DubboService
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + ", Here is " + RpcContext.getServerContext().getLocalPort();
    }
}
