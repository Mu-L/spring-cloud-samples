package org.hongxi.cloud.sample.seata.business;

import io.micrometer.core.instrument.Counter;
import org.hongxi.cloud.sample.seata.business.BusinessApplication.OrderService;
import org.hongxi.cloud.sample.seata.business.BusinessApplication.StorageService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.hongxi.cloud.sample.api.seata.SeataOrderService;
import org.hongxi.cloud.sample.api.seata.SeataStorageService;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HomeController {

    private static final String SUCCESS = "SUCCESS";

    private static final String FAIL = "FAIL";

    private static final String USER_ID = "U100001";

    private static final String COMMODITY_CODE = "C00321";

    private static final int ORDER_COUNT = 2;

    private final RestTemplate restTemplate;

    private final OrderService orderService;

    private final StorageService storageService;

    private final Counter seataTransactionCommitted;

    private final Counter seataTransactionRolledBack;

    @DubboReference
    private SeataStorageService seataStorageService;

    @DubboReference
    private SeataOrderService seataOrderService;

    public HomeController(RestTemplate restTemplate, OrderService orderService,
            StorageService storageService,
            Counter seataTransactionCommitted, Counter seataTransactionRolledBack) {
        this.restTemplate = restTemplate;
        this.orderService = orderService;
        this.storageService = storageService;
        this.seataTransactionCommitted = seataTransactionCommitted;
        this.seataTransactionRolledBack = seataTransactionRolledBack;
    }

    @GlobalTransactional(timeoutMills = 300000, name = "spring-cloud-demo-tx")
    @GetMapping("/seata/rest")
    public String rest() {
        try {
            String result = restTemplate.getForObject(
                    "http://storage-service/storage/" + COMMODITY_CODE + "/" + ORDER_COUNT,
                    String.class);

            if (!SUCCESS.equals(result)) {
                throw new RuntimeException();
            }

            String url = "http://order-service/order";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("userId", USER_ID);
            map.add("commodityCode", COMMODITY_CODE);
            map.add("orderCount", ORDER_COUNT + "");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(
                    map, headers);

            ResponseEntity<String> response;
            try {
                response = restTemplate.postForEntity(url, request, String.class);
            } catch (Exception exx) {
                throw new RuntimeException("mock error");
            }
            result = response.getBody();
            if (!SUCCESS.equals(result)) {
                throw new RuntimeException();
            }

            seataTransactionCommitted.increment();
            return SUCCESS;
        } catch (Exception e) {
            seataTransactionRolledBack.increment();
            throw e;
        }
    }

    @GlobalTransactional(timeoutMills = 300000, name = "spring-cloud-demo-tx")
    @GetMapping("/seata/feign")
    public String feign() {
        try {
            String result = storageService.storage(COMMODITY_CODE, ORDER_COUNT);

            if (!SUCCESS.equals(result)) {
                throw new RuntimeException();
            }

            result = orderService.order(USER_ID, COMMODITY_CODE, ORDER_COUNT);

            if (!SUCCESS.equals(result)) {
                throw new RuntimeException();
            }
            seataTransactionCommitted.increment();
            return SUCCESS;
        } catch (Exception e) {
            seataTransactionRolledBack.increment();
            throw e;
        }
    }

    @GlobalTransactional(timeoutMills = 300000, name = "spring-cloud-demo-dubbo-tx")
    @GetMapping("/seata/dubbo")
    public String dubbo() {
        try {
            String result = seataStorageService.deduct(COMMODITY_CODE, ORDER_COUNT);

            if (!SUCCESS.equals(result)) {
                throw new RuntimeException();
            }

            result = seataOrderService.create(USER_ID, COMMODITY_CODE, ORDER_COUNT);

            if (!SUCCESS.equals(result)) {
                throw new RuntimeException();
            }
            seataTransactionCommitted.increment();
            return SUCCESS;
        } catch (Exception e) {
            seataTransactionRolledBack.increment();
            throw e;
        }
    }

}
