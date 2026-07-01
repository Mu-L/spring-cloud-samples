package org.hongxi.cloud.sample.seata.storage.dubbo.service;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.seata.core.context.RootContext;
import org.hongxi.cloud.sample.api.seata.SeataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

@DubboService
public class SeataStorageServiceImpl implements SeataStorageService {

    private static final Logger log = LoggerFactory.getLogger(SeataStorageServiceImpl.class);

    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";

    private final JdbcTemplate jdbcTemplate;

    public SeataStorageServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String deduct(String commodityCode, int count) {
        log.info("Storage Dubbo Service Begin ... xid: {}", RootContext.getXID());
        int result = jdbcTemplate.update(
                "update storage_tbl set count = count - ? where commodity_code = ?",
                count, commodityCode);
        log.info("Storage Dubbo Service End ... ");
        if (result == 1) {
            return SUCCESS;
        }
        return FAIL;
    }
}
