package org.hongxi.cloud.sample.seata.account;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfiguration {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("delete from account_tbl where user_id = 'U100001'");
        jdbcTemplate.update(
                "insert into account_tbl(user_id, money) values ('U100001', 10000)");

        return jdbcTemplate;
    }

}
