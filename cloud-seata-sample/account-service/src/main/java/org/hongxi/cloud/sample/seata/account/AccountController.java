package org.hongxi.cloud.sample.seata.account;

import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import org.apache.seata.core.context.RootContext;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AccountController {

    private static final String SUCCESS = "SUCCESS";

    private static final String FAIL = "FAIL";

    private final JdbcTemplate jdbcTemplate;

    private final Random random;

    public AccountController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.random = new Random();
    }

    @PostMapping("/account")
    public String account(String userId, int money) {
        log.info("Account Service Begin ... xid: {}", RootContext.getXID());

        if (random.nextBoolean()) {
            throw new RuntimeException("This is a mock Exception");
        }

        int result = jdbcTemplate.update(
                "update account_tbl set money = money - ? where user_id = ?",
                money, userId);
        log.info("Account Service End ... ");
        if (result == 1) {
            return SUCCESS;
        }
        return FAIL;
    }

}
