package org.hongxi.cloud.sample.seata.order.dubbo.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.seata.core.context.RootContext;
import org.hongxi.cloud.sample.api.seata.SeataAccountService;
import org.hongxi.cloud.sample.api.seata.SeataOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@DubboService
public class SeataOrderServiceImpl implements SeataOrderService {

    private static final Logger log = LoggerFactory.getLogger(SeataOrderServiceImpl.class);

    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final String USER_ID = "U100001";

    private final JdbcTemplate jdbcTemplate;
    private final Random random;

    @DubboReference
    private SeataAccountService seataAccountService;

    public SeataOrderServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.random = new Random();
    }

    @Override
    public String create(String userId, String commodityCode, int orderCount) {
        log.info("Order Dubbo Service Begin ... xid: {}", RootContext.getXID());

        int orderMoney = calculate(commodityCode, orderCount);

        seataAccountService.deduct(USER_ID, orderMoney);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int result = jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement pst = con.prepareStatement(
                        "insert into order_tbl (user_id, commodity_code, count, money) values (?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                pst.setObject(1, userId);
                pst.setObject(2, commodityCode);
                pst.setObject(3, orderCount);
                pst.setObject(4, orderMoney);
                return pst;
            }
        }, keyHolder);

        if (random.nextBoolean()) {
            throw new RuntimeException("This is a mock Exception");
        }

        log.info("Order Dubbo Service End ... id: {}", keyHolder.getKey());

        if (result == 1) {
            return SUCCESS;
        }
        return FAIL;
    }

    private int calculate(String commodityId, int orderCount) {
        return 2 * orderCount;
    }
}
