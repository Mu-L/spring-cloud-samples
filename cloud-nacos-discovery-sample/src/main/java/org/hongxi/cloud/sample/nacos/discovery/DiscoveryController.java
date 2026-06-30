package org.hongxi.cloud.sample.nacos.discovery;

import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/discovery")
public class DiscoveryController {

    @Autowired
    private NacosServiceManager nacosServiceManager;

    @GetMapping("/services")
    public ListView<String> allInstances() throws NacosException {
        return nacosServiceManager.getNamingService().getServicesOfServer(0, 20);
    }

    @GetMapping("/instances/{serviceName}")
    public List<Instance> instances(@PathVariable String serviceName,
                                    @RequestParam(required = false, defaultValue = "DEFAULT_GROUP") String group) throws NacosException {
        return nacosServiceManager.getNamingService().getAllInstances(serviceName, group);
    }
}
