package org.hongxi.cloud.sample.seata.account.dubbo.service;

import java.util.Random;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.seata.core.context.RootContext;
import org.hongxi.cloud.sample.api.seata.SeataAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

@DubboService
public class SeataAccountServiceImpl implements SeataAccountService {

    private static final Logger log = LoggerFactory.getLogger(SeataAccountServiceImpl.class);

    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";

    private final JdbcTemplate jdbcTemplate;
    private final Random random;

    public SeataAccountServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.random = new Random();
    }

    @Override
    public String deduct(String userId, int money) {
        log.info("Account Dubbo Service Begin ... xid: {}", RootContext.getXID());

        if (random.nextBoolean()) {
            throw new RuntimeException("This is a mock Exception");
        }

        int result = jdbcTemplate.update(
                "update account_tbl set money = money - ? where user_id = ?",
                money, userId);
        log.info("Account Dubbo Service End ... ");
        if (result == 1) {
            return SUCCESS;
        }
        return FAIL;
    }
}
