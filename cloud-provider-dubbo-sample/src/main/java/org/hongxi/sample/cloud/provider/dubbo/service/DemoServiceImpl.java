package org.hongxi.sample.cloud.provider.dubbo.service;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.Mapping;
import org.apache.dubbo.remoting.http12.rest.Param;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.RpcContext;
import org.hongxi.sample.cloud.api.DemoService;

@DubboService
public class DemoServiceImpl implements DemoService {

    @Mapping(path = "/hello", method = HttpMethods.GET)
    @Override
    public String sayHello(@Param(value = "name", type = ParamType.Param) String name) {
        return "Hello, " + name + ", Here is " + RpcContext.getServerContext().getLocalPort();
    }
}
