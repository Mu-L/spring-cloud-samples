package org.hongxi.cloud.sample.api.seata;

/**
 * Seata demo - Order Dubbo service interface
 */
public interface SeataOrderService {

    String create(String userId, String commodityCode, int orderCount);
}
