package org.hongxi.cloud.sample.seata.order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import org.apache.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private static final String SUCCESS = "SUCCESS";

    private static final String FAIL = "FAIL";

    private static final String USER_ID = "U100001";

    private static final String COMMODITY_CODE = "C00321";

    private final JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate;

    private final Random random;

    public OrderController(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
        this.random = new Random();
    }

    @PostMapping("/order")
    public String order(String userId, String commodityCode, int orderCount) {
        log.info("Order Service Begin ... xid: {}", RootContext.getXID());

        int orderMoney = calculate(commodityCode, orderCount);

        invokerAccountService(orderMoney);

        Order order = new Order();
        order.setUserId(userId);
        order.setCommodityCode(commodityCode);
        order.setCount(orderCount);
        order.setMoney(orderMoney);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int result = jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement pst = con.prepareStatement(
                        "insert into order_tbl (user_id, commodity_code, count, money) values (?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                pst.setObject(1, order.getUserId());
                pst.setObject(2, order.getCommodityCode());
                pst.setObject(3, order.getCount());
                pst.setObject(4, order.getMoney());
                return pst;
            }
        }, keyHolder);

        order.setId(keyHolder.getKey().longValue());

        if (random.nextBoolean()) {
            throw new RuntimeException("This is a mock Exception");
        }

        log.info("Order Service End ... Created {}", order);

        if (result == 1) {
            return SUCCESS;
        }
        return FAIL;
    }

    private int calculate(String commodityId, int orderCount) {
        return 2 * orderCount;
    }

    private void invokerAccountService(int orderMoney) {
        String url = "http://account-service/account";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("userId", USER_ID);
        map.add("money", orderMoney + "");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        restTemplate.postForEntity(url, request, String.class);
    }

}
